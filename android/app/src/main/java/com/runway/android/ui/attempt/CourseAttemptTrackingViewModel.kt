package com.runway.android.ui.attempt

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.runway.android.core.location.GpsStatus
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.tracking.RunTrackingAction
import com.runway.android.core.tracking.RunTrackingManager
import com.runway.android.core.tracking.RunTrackingMode
import com.runway.android.core.tracking.RunTrackingService
import com.runway.android.data.attempt.model.AbandonAttemptRequest
import com.runway.android.data.attempt.model.FinishAttemptRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.domain.attempt.CourseAttemptRepository
import com.runway.android.domain.running.RunningRepository
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

sealed class AttemptNavEvent {
    data class NavigateToLeaderboard(val courseId: String) : AttemptNavEvent()
    object NavigateBack : AttemptNavEvent()
}

@HiltViewModel
class CourseAttemptTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val runningRepository: RunningRepository,
    private val courseAttemptRepository: CourseAttemptRepository,
    private val manager: RunTrackingManager,
    @Named("appScope") private val appScope: CoroutineScope,
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])
    private val courseAttemptId: String = checkNotNull(savedStateHandle["courseAttemptId"])
    private val runningRecordId: String = checkNotNull(savedStateHandle["runningRecordId"])

    var gpsStatus by mutableStateOf(GpsStatus.PERMISSION_REQUIRED)
        private set
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
                "%.1f".format(gpsSpeed * 3.6f)
            } else if (elapsedSeconds < 1) {
                "0.0"
            } else {
                "%.1f".format(distanceKm / (elapsedSeconds / 3600.0))
            }
        }

    private val _navEvent = MutableSharedFlow<AttemptNavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    private var isDone = false
    private var serviceStarted = false

    private var stateObserveJob: Job? = null
    private var batchJob: Job? = null

    init {
        startBatchSaving()
    }

    // Called by the Screen after location permission is granted.
    fun startTracking() {
        if (serviceStarted) return
        serviceStarted = true
        gpsStatus = GpsStatus.WAITING_FOR_FIX

        ContextCompat.startForegroundService(
            context,
            Intent(context, RunTrackingService::class.java).apply {
                action = RunTrackingAction.ACTION_START
                putExtra(RunTrackingAction.EXTRA_MODE, RunTrackingMode.COURSE_ATTEMPT.name)
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
        val points = manager.consumePoints()
        if (points.isEmpty()) return
        val result = runningRepository.savePoints(runningRecordId, SavePointsRequest(points))
        if (result !is NetworkResult.Success) {
            manager.requeuePoints(points)
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

            stopServiceAndJobs()
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
            stopServiceAndJobs()
            isDone = true
            _navEvent.emit(AttemptNavEvent.NavigateBack)
        }
    }

    private fun stopServiceAndJobs() {
        stateObserveJob?.cancel()
        batchJob?.cancel()
        manager.stop()
        context.startService(
            Intent(context, RunTrackingService::class.java).apply {
                action = RunTrackingAction.ACTION_STOP
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        if (!isDone) {
            stopServiceAndJobs()
            appScope.launch {
                courseAttemptRepository.abandonAttempt(
                    attemptId = courseAttemptId,
                    request = AbandonAttemptRequest(abandonedAt = Instant.now().toString()),
                )
            }
        }
    }
}
