package com.runway.android.data.attempt.model

data class StartAttemptResponse(
    val courseId: String,
    val courseAttemptId: String,
    val runningRecordId: String,
    val status: String,
    val verificationStatus: String,
    val startedAt: String,
)
