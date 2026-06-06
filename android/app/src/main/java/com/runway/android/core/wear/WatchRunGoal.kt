package com.runway.android.core.wear

enum class WatchGoalCompletionAction {
    PAUSE,
    CONTINUE,
}

sealed interface WatchIntervalTarget {
    data class Time(val seconds: Int) : WatchIntervalTarget
    data class Distance(val meters: Int) : WatchIntervalTarget
}

sealed interface WatchRunGoal {
    data object Free : WatchRunGoal
    data class Time(
        val targetMinutes: Int,
        val completionAction: WatchGoalCompletionAction,
    ) : WatchRunGoal
    data class Distance(
        val targetMeters: Int,
        val completionAction: WatchGoalCompletionAction,
    ) : WatchRunGoal
    data class Interval(
        val work: WatchIntervalTarget,
        val recovery: WatchIntervalTarget,
        val sets: Int,
        val completionAction: WatchGoalCompletionAction,
    ) : WatchRunGoal
}
