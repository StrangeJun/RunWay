package com.runway.android.data.attempt.model

data class FinishAttemptResponse(
    val courseAttemptId: String,
    val runningRecordId: String,
    val courseId: String,
    val attemptStatus: String,
    val verificationStatus: String,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val completedAt: String,
    /** 개인 최고 기록(PR) 여부 */
    val isPR: Boolean = false,
    /** 이전 최고 기록(초). 첫 완주이면 null */
    val previousBestSeconds: Int? = null,
    /** 이전 기록 대비 개선 시간(초). 양수 = 빠름, 음수 = 느림. 첫 완주이면 null */
    val improvementSeconds: Int? = null,
)
