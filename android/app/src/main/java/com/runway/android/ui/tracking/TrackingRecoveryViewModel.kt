package com.runway.android.ui.tracking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.tracking.PendingPointQueue
import com.runway.android.core.tracking.RunTrackingManager
import com.runway.android.core.tracking.TrackingSessionSnapshot
import com.runway.android.core.tracking.TrackingSessionStore
import com.runway.android.core.tracking.toRunPointRequest
import com.runway.android.data.attempt.model.AbandonAttemptRequest
import com.runway.android.data.attempt.model.FinishAttemptRequest
import com.runway.android.data.running.model.FinishRunRequest
import com.runway.android.data.running.model.SavePointsRequest
import com.runway.android.domain.attempt.CourseAttemptRepository
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TrackingRecoveryViewModel @Inject constructor(
    private val sessionStore: TrackingSessionStore,
    private val pendingPointQueue: PendingPointQueue,
    private val runningRepository: RunningRepository,
    private val courseAttemptRepository: CourseAttemptRepository,
    private val manager: RunTrackingManager,
) : ViewModel() {

    var snapshot by mutableStateOf<TrackingSessionSnapshot?>(null)
        private set
    var isRecovering by mutableStateOf(false)
        private set
    var isVisible by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            sessionStore.snapshot.collect { snap ->
                snapshot = snap
                isVisible = snap != null && !manager.state.value.isTracking
            }
        }
    }

    fun finish() {
        val snap = snapshot ?: return
        if (isRecovering) return
        isRecovering = true

        viewModelScope.launch {
            // Best-effort upload persisted points
            repeat(3) {
                val batch = pendingPointQueue.dequeue(snap.runningRecordId, 50)
                if (batch.isNotEmpty()) {
                    val r = runningRepository.savePoints(
                        snap.runningRecordId,
                        SavePointsRequest(batch.map { it.toRunPointRequest() }),
                    )
                    if (r is NetworkResult.Success) {
                        pendingPointQueue.deleteByIds(batch.map { it.id })
                    }
                }
            }

            if (snap.courseAttemptId != null) {
                val distKm = snap.distanceMeters / 1000.0
                courseAttemptRepository.finishAttempt(
                    attemptId = snap.courseAttemptId,
                    request = FinishAttemptRequest(
                        endedAt = Instant.now().toString(),
                        distanceMeters = snap.distanceMeters,
                        durationSeconds = snap.elapsedSeconds,
                        avgPaceSecondsPerKm = if (distKm > 0.001) (snap.elapsedSeconds / distKm).toInt() else 0,
                        caloriesBurned = (distKm * 72).toInt(),
                    ),
                )
            } else {
                val distKm = snap.distanceMeters / 1000.0
                runningRepository.finishRun(
                    runId = snap.runningRecordId,
                    request = FinishRunRequest(
                        endedAt = Instant.now().toString(),
                        distanceMeters = snap.distanceMeters,
                        durationSeconds = snap.elapsedSeconds,
                        avgPaceSecondsPerKm = if (distKm > 0.001) (snap.elapsedSeconds / distKm).toInt() else 0,
                        caloriesBurned = (distKm * 72).toInt(),
                    ),
                )
            }

            cleanUp(snap.runningRecordId)
        }
    }

    fun abandon() {
        val snap = snapshot ?: return
        if (isRecovering) return
        isRecovering = true

        viewModelScope.launch {
            if (snap.courseAttemptId != null) {
                courseAttemptRepository.abandonAttempt(
                    attemptId = snap.courseAttemptId,
                    request = AbandonAttemptRequest(abandonedAt = Instant.now().toString()),
                )
            } else {
                runningRepository.abandonRun(snap.runningRecordId)
            }
            cleanUp(snap.runningRecordId)
        }
    }

    private suspend fun cleanUp(runningRecordId: String) {
        sessionStore.clearSnapshot()
        pendingPointQueue.deleteByRunningRecordId(runningRecordId)
        isRecovering = false
        isVisible = false
    }
}
