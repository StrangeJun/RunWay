package com.runway.android.core.wear

sealed interface WatchRunGoal {
    data object Free : WatchRunGoal
    data class Time(val targetMinutes: Int) : WatchRunGoal
    data class Distance(val targetMeters: Int) : WatchRunGoal
    data class Interval(
        val workSeconds: Int,
        val restSeconds: Int,
        val sets: Int,
    ) : WatchRunGoal
}
