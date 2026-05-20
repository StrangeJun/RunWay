package com.runway.android.data.running.model

data class RunningStatsResponse(
    val period: String,
    val totalRuns: Long,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Long,
    val totalCaloriesBurned: Long,
    val averagePaceSecondsPerKm: Int,
    val longestRunMeters: Double,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val activeDays: Long,
    val periodStart: String?,
    val periodEnd: String?,
)
