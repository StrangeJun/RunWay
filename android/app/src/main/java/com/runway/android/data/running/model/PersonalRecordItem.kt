package com.runway.android.data.running.model

data class PersonalRecordItem(
    val runId: String,
    val distanceMeters: Double?,
    val durationSeconds: Int?,
    val avgPaceSecondsPerKm: Int?,
    val caloriesBurned: Int?,
    val recordedAt: String,
)
