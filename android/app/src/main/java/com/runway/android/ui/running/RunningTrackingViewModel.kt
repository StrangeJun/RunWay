package com.runway.android.ui.running

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.location.GpsStatus
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.tracking.PauseReason
import com.runway.android.core.tracking.PendingPointQueue
import com.runway.android.core.tracking.RunTrackingAction
import com.runway.android.core.tracking.RunTrackingManager
import com.runway.android.core.tracking.RunTrackingMode
import com.runway.android.core.tracking.RunTrackingService
import com.runway.android.core.tracking.TrackingSessionSnapshot
import com.runway.android.core.tracking.TrackingSessionStore
import com.runway.android.core.tracking.toRunPointRequest
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.domain.running.RunningRepository
import com.runway.android.core.util.formatDuration
import com.runway.android.core.voice.RunningVoiceGuide
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named

data class RunResult(
    val runId: String?,
    val elapsedSeconds: Int,
    val distanceKm: Float,
)

enum class RunningState { RUNNING, PAUSED, AUTO_PAUSED }

@HiltViewModel
class RunningTrackingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runningRepository: RunningRepository,
    private val manager: RunTrackingManager,
    private val pendingPointQueue: PendingPointQueue,
    private val sessionStore: TrackingSessionStore,
    private val voiceGuide: RunningVoiceGuide,
    @Named("appScope") private val appScope: CoroutineScope,
) : ViewModel() {

    var gpsStatus by mutableStateOf(GpsStatus.PERMISSION_REQUIRED)
        private set
    var runningState by mutableStateOf(RunningState.RUNNING)
        private set
    var elapsedSeconds by mutableStateOf(0)
        private set
    var distanceKm by mutableStateOf(0.0)
        private set
    var isConnecting by mutableStateOf(true)
        private set
    var isFinishing by mutableStateOf(false)
        private set
    var finishError by mutableStateOf<String?>(null)
        private set
    var milestoneMessage by mutableStateOf<String?>(null)
        private set

    private var lastSpeedMps by mutableStateOf<Float?>(null)
    private var cadenceSpm by mutableStateOf<Int?>(null)

    val timerText: String
        get() = formatDuration(elapsedSeconds)

    val distanceText: String
        get() = "%.2f".format(distanceKm)

    val paceText: String
        get() {
            if (distanceKm < 0.001) return "--'--\""
            val secsPerKm = (elapsedSeconds / distanceKm).toInt()
            return "%d'%02d\"".format(secsPerKm / 60, secsPerKm % 60)
        }

    val speedText: String
        get() {
            val gpsSpeed = lastSpeedMps
            return if (gpsSpeed != null && gpsSpeed >= 0.1f) {
                "%.1f".format(gpsSpeed * 3.6f)
            } else if (elapsedSeconds < 1) {
                "0.0"
            } else {
                "%.1f".format(distanceKm / (elapsedSeconds / 3600.0))
            }
        }

    val cadenceText: String
        get() = cadenceSpm?.takeIf { it > 0 }?.toString() ?: "--"

    private val _navigateToResult = MutableSharedFlow<RunResult>()
    val navigateToResult = _navigateToResult.asSharedFlow()

    private var runId: String? = null
    private var isFinished = false
    private var serviceStarted = false
    private var prevAutoPaused = false

    private var stateObserveJob: Job? = null
    private var batchJob: Job? = null
    private var milestoneCollectJob: Job? = null
    private var milestoneDisplayJob: Job? = null

    fun startTracking() {
        if (serviceStarted) return
        serviceStarted = true
        gpsStatus = GpsStatus.WAITING_FOR_FIX
        voiceGuide.start()
        startRun()

        ContextCompat.startForegroundService(
            context,
            Intent(context, RunTrackingService::class.java).apply {
                action = RunTrackingAction.ACTION_START
                putExtra(RunTrackingAction.EXTRA_MODE, RunTrackingMode.FREE_RUN.name)
            }
        )

        stateObserveJob = viewModelScope.launch {
            manager.state.collect { state ->
                if (state.hasFirstFix && gpsStatus != GpsStatus.ACTIVE) {
                    gpsStatus = GpsStatus.ACTIVE
                }
                elapsedSeconds = state.elapsedSeconds
                distanceKm = state.distanceMeters / 1000.0
                lastSpeedMps = state.currentSpeedMps
                cadenceSpm = state.cadenceSpm

                // 자동 일시정지 ↔ 재개 전환 시 음성 안내
                if (state.isAutoPaused != prevAutoPaused) {
                    if (state.isAutoPaused) voiceGuide.autoPaused()
                    else voiceGuide.autoResumed()
                    prevAutoPaused = state.isAutoPaused
                }

                runningState = when {
                    state.isAutoPaused -> RunningState.AUTO_PAUSED
                    state.isPaused -> RunningState.PAUSED
                    else -> RunningState.RUNNING
                }
            }
        }

        milestoneCollectJob = viewModelScope.launch {
            manager.milestoneFlow.collect { km -> triggerMilestone(km) }
        }
    }

    private fun triggerMilestone(km: Int) {
        vibrate()
        voiceGuide.kilometer(km, elapsedSeconds)
        milestoneDisplayJob?.cancel()
        milestoneMessage = "${km}km 완료 · 현재 페이스 $paceText /km"
        milestoneDisplayJob = viewModelScope.launch {
            delay(4_000)
            milestoneMessage = null
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
        )
    }

    private fun startRun() {
        viewModelScope.launch {
            val result = runningRepository.startRun(StartRunRequest(startedAt = Instant.now().toString()))
            when (result) {
                is NetworkResult.Success -> {
                    val rid = result.data.runId
                    runId = rid
                    sessionStore.saveSnapshot(
                        TrackingSessionSnapshot(
                            runningRecordId = rid,
                            courseAttemptId = null,
                            elapsedSeconds = 0,
                            distanceMeters = 0.0,
                        )
                    )
                    isConnecting = false
                    startBatchSaving()
                }
                else -> isConnecting = false
            }
        }
    }

    private fun startBatchSaving() {
        batchJob = viewModelScope.launch {
            while (true) {
                delay(5_000L)
                flushPendingPoints()
                updateSnapshot()
            }
        }
    }

    private suspend fun flushPendingPoints() {
        val rid = runId ?: return
        val inMemory = manager.consumePoints()
        pendingPointQueue.add(rid, inMemory)

        val batch = pendingPointQueue.dequeue(rid, 50)
        if (batch.isEmpty()) return

        val result = runningRepository.savePoints(rid, SavePointsRequest(batch.map { it.toRunPointRequest() }))
        if (result is NetworkResult.Success) {
            pendingPointQueue.deleteByIds(batch.map { it.id })
        }
    }

    private suspend fun updateSnapshot() {
        val rid = runId ?: return
        sessionStore.saveSnapshot(
            TrackingSessionSnapshot(
                runningRecordId = rid,
                courseAttemptId = null,
                elapsedSeconds = elapsedSeconds,
                distanceMeters = distanceKm * 1000.0,
            )
        )
    }

    fun pause() {
        manager.pause()
        val rid = runId ?: return
        viewModelScope.launch {
            if (runningRepository.pauseRun(rid) !is NetworkResult.Success) {
                manager.resume()
            }
        }
    }

    fun resume() {
        val wasAutoPaused = manager.state.value.isAutoPaused
        manager.resume()
        if (!wasAutoPaused) {
            val rid = runId ?: return
            viewModelScope.launch {
                if (runningRepository.resumeRun(rid) !is NetworkResult.Success) {
                    manager.pause()
                }
            }
        }
    }

    fun finish() {
        if (isFinishing) return
        isFinishing = true
        finishError = null

        val currentRunId = runId
        val seconds = elapsedSeconds
        val distance = distanceKm

        viewModelScope.launch {
            if (currentRunId != null) {
                pendingPointQueue.add(currentRunId, manager.consumePoints())
                // 큐가 빌 때까지 업로드. 연속 실패 10회 시 중단 (서버/네트워크 문제).
                var failures = 0
                while (failures < 10) {
                    val batch = pendingPointQueue.dequeue(currentRunId, 50)
                    if (batch.isEmpty()) break
                    val r = runningRepository.savePoints(
                        currentRunId,
                        SavePointsRequest(batch.map { it.toRunPointRequest() }),
                    )
                    if (r is NetworkResult.Success) {
                        pendingPointQueue.deleteByIds(batch.map { it.id })
                        failures = 0
                    } else {
                        failures++
                    }
                }

                when (runningRepository.finishRun(
                    currentRunId,
                    FinishRunRequest(
                        endedAt = Instant.now().toString(),
                        distanceMeters = distance * 1000.0,
                        durationSeconds = seconds,
                        avgPaceSecondsPerKm = if (distance > 0.001) (seconds / distance).toInt() else 0,
                        caloriesBurned = (distance * 72).toInt(),
                    ),
                )) {
                    is NetworkResult.ApiError,
                    is NetworkResult.NetworkError -> {
                        isFinishing = false
                        finishError = "런 완료에 실패했습니다. 다시 시도해 주세요."
                        return@launch
                    }
                    is NetworkResult.Success -> {
                        voiceGuide.finish()
                        sessionStore.clearSnapshot()
                        pendingPointQueue.deleteByRunningRecordId(currentRunId)
                    }
                }
            }

            stopServiceAndJobs()
            isFinished = true
            _navigateToResult.emit(
                RunResult(
                    runId = currentRunId,
                    elapsedSeconds = seconds,
                    distanceKm = distance.toFloat(),
                )
            )
        }
    }

    private fun stopServiceAndJobs() {
        stateObserveJob?.cancel()
        batchJob?.cancel()
        milestoneCollectJob?.cancel()
        milestoneDisplayJob?.cancel()
        manager.stop()
        context.startService(
            Intent(context, RunTrackingService::class.java).apply {
                action = RunTrackingAction.ACTION_STOP
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        if (!isFinished) {
            stopServiceAndJobs()
            val rid = runId
            if (rid != null) {
                appScope.launch {
                    runningRepository.abandonRun(rid)
                    sessionStore.clearSnapshot()
                    pendingPointQueue.deleteByRunningRecordId(rid)
                }
            }
        }
    }
}
