package com.runway.android.data.running.model

data class RunDetailResponse(
    val runId: String,
    val status: String,
    val startedAt: String,
    val endedAt: String?,
    val distanceMeters: Double?,
    val durationSeconds: Int?,
    val avgPaceSecondsPerKm: Int?,
    val caloriesBurned: Int?,
    val avgHeartRateBpm: Int?,
    val points: List<RunPointResponse>,
)
