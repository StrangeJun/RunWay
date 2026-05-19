package com.runway.android.data.attempt.model

data class StartAttemptResponse(
    val courseId: String,
    val courseAttemptId: String,
    val runningRecordId: String,
    val attemptStatus: String,
    val runStatus: String,
    val startedAt: String,
)
