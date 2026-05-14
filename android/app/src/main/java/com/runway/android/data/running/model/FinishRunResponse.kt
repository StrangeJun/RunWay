package com.runway.android.data.running.model

data class FinishRunResponse(
    val runId: String,
    val status: String,
    val startedAt: String,
    val endedAt: String,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val avgPaceSecondsPerKm: Int,
    val pathCreated: Boolean,
)
