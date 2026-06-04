package com.runway.android.data.attempt.model

data class MyBestAttemptResponse(
    val completionCount: Long,
    val bestTimeSeconds: Int?,
    val lastAttemptAt: String?,
)
