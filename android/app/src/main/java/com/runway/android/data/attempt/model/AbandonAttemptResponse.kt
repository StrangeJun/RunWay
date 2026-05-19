package com.runway.android.data.attempt.model

data class AbandonAttemptResponse(
    val courseAttemptId: String,
    val runningRecordId: String,
    val attemptStatus: String,
    val runStatus: String,
)
