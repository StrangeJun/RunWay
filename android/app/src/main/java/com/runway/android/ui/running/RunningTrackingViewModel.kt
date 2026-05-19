package com.runway.android.ui.running

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.location.DistanceCalculator
import com.runway.android.core.location.GpsStatus
import com.runway.android.core.location.LocationTracker
import com.runway.android.core.location.RunwayLocation
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.RunPointRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

enum class RunningState { RUNNING, PAUSED }

@HiltViewModel
class RunningTrackingViewModel @Inject constructor(
    private val runningRepository: RunningRepository,
    private val locationTracker: LocationTracker,
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

    private var lastSpeedMps by mutableStateOf<Float?>(null)

    val timerText: String
        get() = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

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
                "%.1f".format(gpsSpeed * 3.6f) // m/s → km/h
            } else if (elapsedSeconds < 1) {
                "0.0"
            } else {
                "%.1f".format(distanceKm / (elapsedSeconds / 3600.0))
            }
        }

    private val _navigateToResult = MutableSharedFlow<RunResult>()
    val navigateToResult = _navigateToResult.asSharedFlow()

    private var runId: String? = null
    private var isFinished = false
    private var pointSequence = 0
    private val pendingPoints = mutableListOf<RunPointRequest>()
    private var lastLocation: RunwayLocation? = null

    private var locationJob: Job? = null
    private var timerJob: Job? = null
    private var batchJob: Job? = null

    init {
        startRun()
        startTimer()
    }

    // Called by the Screen after location permission is granted.
    fun startTracking() {
        if (locationJob != null && locationJob!!.isActive) return
        gpsStatus = GpsStatus.WAITING_FOR_FIX
        locationJob = viewModelScope.launch {
            locationTracker.locationFlow().collect { location ->
                gpsStatus = GpsStatus.ACTIVE
                if (runningState == RunningState.RUNNING) {
                    val prev = lastLocation
                    if (prev != null) {
                        val deltaMeters = DistanceCalculator.calculate(prev, location)
                        if (deltaMeters > 0) {
                            distanceKm += deltaMeters / 1000.0
                        }
                    }
                    recordGpsPoint(location)
                }
                location.speedMps?.let { lastSpeedMps = it }
                // Always update lastLocation (including during pause) so resume picks up
                // from the current position rather than the pre-pause position.
                lastLocation = location
            }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                if (runningState == RunningState.RUNNING) {
                    elapsedSeconds++
                }
            }
        }
    }

    private fun recordGpsPoint(location: RunwayLocation) {
        pendingPoints.add(
            RunPointRequest(
                sequence = pointSequence,
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = location.altitudeMeters ?: 0.0,
                speedMps = location.speedMps?.toDouble() ?: 0.0,
                recordedAt = location.recordedAt.toString(),
            )
        )
        pointSequence++
    }

    private fun startRun() {
        viewModelScope.launch {
            val result = runningRepository.startRun(StartRunRequest(startedAt = Instant.now().toString()))
            when (result) {
                is NetworkResult.Success -> {
                    runId = result.data.runId
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
            }
        }
    }

    private suspend fun flushPendingPoints() {
        val rid = runId ?: return
        if (pendingPoints.isEmpty()) return
        val batch = pendingPoints.toList()
        pendingPoints.clear()
        val result = runningRepository.savePoints(rid, SavePointsRequest(batch))
        if (result !is NetworkResult.Success) {
            pendingPoints.addAll(0, batch)
        }
    }

    fun pause() {
        runningState = RunningState.PAUSED
        val rid = runId ?: return
        viewModelScope.launch {
            if (runningRepository.pauseRun(rid) !is NetworkResult.Success) {
                runningState = RunningState.RUNNING
            }
        }
    }

    fun resume() {
        runningState = RunningState.RUNNING
        val rid = runId ?: return
        viewModelScope.launch {
            if (runningRepository.resumeRun(rid) !is NetworkResult.Success) {
                runningState = RunningState.PAUSED
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
                flushPendingPoints()
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
                    is NetworkResult.Success -> Unit
                }
            }
            timerJob?.cancel()
            locationJob?.cancel()
            batchJob?.cancel()
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        locationJob?.cancel()
        batchJob?.cancel()
        val rid = runId
        if (rid != null && !isFinished) {
            appScope.launch { runningRepository.abandonRun(rid) }
        }
    }
}
