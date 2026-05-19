package com.runway.android.data.attempt.model

data class CourseAttemptResponse(
    val courseAttemptId: String,
    val courseId: String,
    val runningRecordId: String,
    val attemptStatus: String,
    val verificationStatus: String?,
    val durationSeconds: Int?,
    val distanceMeters: Double?,
    val startedAt: String,
    val completedAt: String?,
)
