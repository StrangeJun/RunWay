package com.runway.android.data.running.model

data class FinishRunRequest(
    val endedAt: String,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val avgPaceSecondsPerKm: Int,
    val caloriesBurned: Int,
)
