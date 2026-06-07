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
    var recoveryError by mutableStateOf<String?>(null)
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
        recoveryError = null

        viewModelScope.launch {
            // Best-effort upload persisted points
            repeat(3) {
                val batch = pendingPointQueue.dequeue(snap.runningRecordId, 20)
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

            // elapsedSeconds == 0인데 distanceMeters > 0이면 IMPOSSIBLE_SPEED 에러 방지
            val safeDistance = if (snap.elapsedSeconds == 0) 0.0 else snap.distanceMeters
            val safeDistKm = safeDistance / 1000.0

            val result = if (snap.courseAttemptId != null) {
                courseAttemptRepository.finishAttempt(
                    attemptId = snap.courseAttemptId,
                    request = FinishAttemptRequest(
                        endedAt = Instant.now().toString(),
                        distanceMeters = safeDistance,
                        durationSeconds = snap.elapsedSeconds,
                        avgPaceSecondsPerKm = if (safeDistKm > 0.001) (snap.elapsedSeconds / safeDistKm).toInt() else 0,
                        caloriesBurned = (safeDistKm * 72).toInt(),
                    ),
                )
            } else {
                runningRepository.finishRun(
                    runId = snap.runningRecordId,
                    request = FinishRunRequest(
                        endedAt = Instant.now().toString(),
                        distanceMeters = safeDistance,
                        durationSeconds = snap.elapsedSeconds,
                        avgPaceSecondsPerKm = if (safeDistKm > 0.001) (snap.elapsedSeconds / safeDistKm).toInt() else 0,
                        caloriesBurned = (safeDistKm * 72).toInt(),
                    ),
                )
            }

            when {
                result is NetworkResult.Success -> cleanUp(snap.runningRecordId)
                // 서버에서 이미 완료/포기 처리됐거나 레코드가 없는 경우:
                // 네트워크 응답을 못 받은 상태에서 재시도한 케이스이므로 로컬 세션만 정리
                result is NetworkResult.ApiError && result.errorCode in ALREADY_TERMINAL_ERRORS ->
                    cleanUp(snap.runningRecordId)
                else -> {
                    recoveryError = "완료 처리에 실패했습니다. 다시 시도하거나 세션을 닫아주세요."
                    isRecovering = false
                }
            }
        }
    }

    fun abandon() {
        val snap = snapshot ?: return
        if (isRecovering) return
        isRecovering = true
        recoveryError = null

        viewModelScope.launch {
            val result = if (snap.courseAttemptId != null) {
                courseAttemptRepository.abandonAttempt(
                    attemptId = snap.courseAttemptId,
                    request = AbandonAttemptRequest(endedAt = Instant.now().toString()),
                )
            } else {
                runningRepository.abandonRun(snap.runningRecordId)
            }

            when {
                result is NetworkResult.Success -> cleanUp(snap.runningRecordId)
                result is NetworkResult.ApiError && result.errorCode in ALREADY_TERMINAL_ERRORS ->
                    cleanUp(snap.runningRecordId)
                else -> {
                    recoveryError = "포기 처리에 실패했습니다. 다시 시도하거나 세션을 닫아주세요."
                    isRecovering = false
                }
            }
        }
    }

    /** 서버 오류가 지속될 때 사용자가 로컬 세션을 강제로 폐기하는 탈출구 */
    fun forceClose() {
        val snap = snapshot ?: return
        viewModelScope.launch {
            cleanUp(snap.runningRecordId)
        }
    }

    private suspend fun cleanUp(runningRecordId: String) {
        sessionStore.clearSnapshot()
        pendingPointQueue.deleteByRunningRecordId(runningRecordId)
        isRecovering = false
        isVisible = false
    }

    companion object {
        private val ALREADY_TERMINAL_ERRORS = setOf(
            "INVALID_ATTEMPT_STATUS",   // 이미 완료/포기된 코스 시도
            "INVALID_RUN_STATUS",       // 이미 완료/포기된 런 기록
            "COURSE_ATTEMPT_NOT_FOUND", // 서버에서 삭제된 코스 시도
            "RUN_NOT_FOUND",            // 서버에서 삭제된 런 기록
        )
    }
}
