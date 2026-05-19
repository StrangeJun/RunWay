package com.runway.android.ui.running

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.RunPointRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.data.running.model.StartRunRequest
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
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

// Sample GPS coordinates cycling through a loop near Seoul's Hangang Park
private val SAMPLE_COORDS = listOf(
    37.5100 to 126.9997,
    37.5105 to 127.0005,
    37.5110 to 127.0013,
    37.5115 to 127.0021,
    37.5120 to 127.0029,
    37.5125 to 127.0037,
    37.5130 to 127.0045,
    37.5125 to 127.0053,
    37.5120 to 127.0061,
    37.5115 to 127.0069,
    37.5110 to 127.0061,
    37.5105 to 127.0053,
    37.5100 to 127.0045,
    37.5095 to 127.0037,
    37.5100 to 127.0029,
    37.5105 to 127.0021,
    37.5110 to 127.0013,
    37.5115 to 127.0005,
    37.5110 to 126.9997,
    37.5105 to 126.9989,
)

@HiltViewModel
class RunningTrackingViewModel @Inject constructor(
    private val runningRepository: RunningRepository,
    @Named("appScope") private val appScope: CoroutineScope,
) : ViewModel() {

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
            if (elapsedSeconds < 1) return "0.0"
            return "%.1f".format(distanceKm / (elapsedSeconds / 3600.0))
        }

    private val _navigateToResult = MutableSharedFlow<RunResult>()
    val navigateToResult = _navigateToResult.asSharedFlow()

    private var runId: String? = null
    private var isFinished = false
    private var pointSequence = 0
    private val pendingPoints = mutableListOf<RunPointRequest>()

    private var simulationJob: Job? = null
    private var batchJob: Job? = null

    init {
        startRun()
        startSimulation()
    }

    private fun startRun() {
        viewModelScope.launch {
            val now = Instant.now().toString()
            val result = runningRepository.startRun(StartRunRequest(startedAt = now))
            when (result) {
                is NetworkResult.Success -> {
                    runId = result.data.runId
                    isConnecting = false
                    startBatchSaving()
                }
                else -> {
                    // Offline fallback — timer and GPS simulation continue without backend saving
                    isConnecting = false
                }
            }
        }
    }

    private fun startSimulation() {
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                if (runningState == RunningState.RUNNING) {
                    elapsedSeconds++
                    distanceKm += 0.002778 // ~10 km/h
                    recordGpsPoint()
                }
            }
        }
    }

    private fun recordGpsPoint() {
        val idx = pointSequence % SAMPLE_COORDS.size
        val (lat, lng) = SAMPLE_COORDS[idx]
        val altitude = 72.0 + (pointSequence % 5) * 0.5
        pendingPoints.add(
            RunPointRequest(
                sequence = pointSequence,
                latitude = lat,
                longitude = lng,
                altitudeMeters = altitude,
                speedMps = 2.78,
                recordedAt = Instant.now().toString(),
            )
        )
        pointSequence++
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
            // Re-queue at front to preserve order; will be retried on next flush
            pendingPoints.addAll(0, batch)
        }
    }

    fun pause() {
        runningState = RunningState.PAUSED  // optimistic update
        val rid = runId ?: return
        viewModelScope.launch {
            if (runningRepository.pauseRun(rid) !is NetworkResult.Success) {
                runningState = RunningState.RUNNING  // rollback on failure
            }
        }
    }

    fun resume() {
        runningState = RunningState.RUNNING  // optimistic update
        val rid = runId ?: return
        viewModelScope.launch {
            if (runningRepository.resumeRun(rid) !is NetworkResult.Success) {
                runningState = RunningState.PAUSED  // rollback on failure
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

                val distanceMeters = distance * 1000.0
                val avgPace = if (distance > 0.001) (seconds / distance).toInt() else 0
                val calories = (distance * 72).toInt()

                when (runningRepository.finishRun(
                    currentRunId,
                    FinishRunRequest(
                        endedAt = Instant.now().toString(),
                        distanceMeters = distanceMeters,
                        durationSeconds = seconds,
                        avgPaceSecondsPerKm = avgPace,
                        caloriesBurned = calories,
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

            simulationJob?.cancel()
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
        simulationJob?.cancel()
        batchJob?.cancel()
        // viewModelScope is already cancelled here; use app-level scope for cleanup
        val rid = runId
        if (rid != null && !isFinished) {
            appScope.launch {
                runningRepository.abandonRun(rid)
            }
        }
    }
}
