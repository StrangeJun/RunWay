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
)
