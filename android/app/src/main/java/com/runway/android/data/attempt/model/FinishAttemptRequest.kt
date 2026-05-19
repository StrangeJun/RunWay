package com.runway.android.data.attempt.model

data class FinishAttemptRequest(
    val endedAt: String,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val avgPaceSecondsPerKm: Int,
    val caloriesBurned: Int,
    val avgHeartRateBpm: Int? = null,
)
