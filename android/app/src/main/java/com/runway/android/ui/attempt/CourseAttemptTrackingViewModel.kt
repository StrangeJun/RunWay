package com.runway.android.ui.attempt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.attempt.model.AbandonAttemptRequest
import com.runway.android.data.attempt.model.FinishAttemptRequest
import com.runway.android.data.running.model.RunPointRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.domain.attempt.CourseAttemptRepository
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

sealed class AttemptNavEvent {
    data class NavigateToLeaderboard(val courseId: String) : AttemptNavEvent()
    object NavigateBack : AttemptNavEvent()
}

private val SAMPLE_COORDS = listOf(
    36.9706 to 127.8718, 36.9710 to 127.8724, 36.9715 to 127.8730,
    36.9720 to 127.8736, 36.9725 to 127.8741, 36.9728 to 127.8748,
    36.9723 to 127.8755, 36.9718 to 127.8761, 36.9713 to 127.8756,
    36.9708 to 127.8750, 36.9706 to 127.8742, 36.9704 to 127.8733,
    36.9706 to 127.8725, 36.9708 to 127.8718, 36.9706 to 127.8718,
)

@HiltViewModel
class CourseAttemptTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runningRepository: RunningRepository,
    private val courseAttemptRepository: CourseAttemptRepository,
    @Named("appScope") private val appScope: CoroutineScope,
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])
    private val courseAttemptId: String = checkNotNull(savedStateHandle["courseAttemptId"])
    private val runningRecordId: String = checkNotNull(savedStateHandle["runningRecordId"])

    var elapsedSeconds by mutableStateOf(0)
        private set
    var distanceKm by mutableStateOf(0.0)
        private set
    var isFinishing by mutableStateOf(false)
        private set
    var isAbandoning by mutableStateOf(false)
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

    private val _navEvent = MutableSharedFlow<AttemptNavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    private var isDone = false
    private var pointSequence = 0
    private val pendingPoints = mutableListOf<RunPointRequest>()
    private var simulationJob: Job? = null
    private var batchJob: Job? = null

    init {
        startSimulation()
        startBatchSaving()
    }

    private fun startSimulation() {
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                elapsedSeconds++
                distanceKm += 0.002778
                recordGpsPoint()
            }
        }
    }

    private fun recordGpsPoint() {
        val idx = pointSequence % SAMPLE_COORDS.size
        val (lat, lng) = SAMPLE_COORDS[idx]
        pendingPoints.add(
            RunPointRequest(
                sequence = pointSequence,
                latitude = lat,
                longitude = lng,
                altitudeMeters = 72.0 + (pointSequence % 5) * 0.5,
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
        if (pendingPoints.isEmpty()) return
        val batch = pendingPoints.toList()
        pendingPoints.clear()
        val result = runningRepository.savePoints(runningRecordId, SavePointsRequest(batch))
        if (result !is NetworkResult.Success) {
            pendingPoints.addAll(0, batch)
        }
    }

    fun finish() {
        if (isFinishing || isAbandoning) return
        isFinishing = true
        finishError = null

        val seconds = elapsedSeconds
        val distance = distanceKm

        viewModelScope.launch {
            flushPendingPoints()

            when (courseAttemptRepository.finishAttempt(
                attemptId = courseAttemptId,
                request = FinishAttemptRequest(
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
                    finishError = "완주 처리에 실패했습니다. 다시 시도해 주세요."
                    return@launch
                }
                is NetworkResult.Success -> Unit
            }

            simulationJob?.cancel()
            batchJob?.cancel()
            isDone = true
            _navEvent.emit(AttemptNavEvent.NavigateToLeaderboard(courseId))
        }
    }

    fun abandon() {
        if (isFinishing || isAbandoning) return
        isAbandoning = true

        viewModelScope.launch {
            courseAttemptRepository.abandonAttempt(
                attemptId = courseAttemptId,
                request = AbandonAttemptRequest(abandonedAt = Instant.now().toString()),
            )
            simulationJob?.cancel()
            batchJob?.cancel()
            isDone = true
            _navEvent.emit(AttemptNavEvent.NavigateBack)
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        batchJob?.cancel()
        if (!isDone) {
            // ViewModel이 비정상 종료 시 백그라운드에서 abandon 처리
            appScope.launch {
                courseAttemptRepository.abandonAttempt(
                    attemptId = courseAttemptId,
                    request = AbandonAttemptRequest(abandonedAt = Instant.now().toString()),
                )
            }
        }
    }
}
