package com.runway.wear.model

sealed interface RunGoal {
    data object Free : RunGoal
    data class Time(val minutes: Int) : RunGoal
    data class Distance(val meters: Int) : RunGoal
    data class Interval(
        val workSeconds: Int,
        val restSeconds: Int,
        val sets: Int,
    ) : RunGoal
}

enum class WatchScreen {
    HOME,
    GOAL_TYPE,
    TIME_GOAL,
    DISTANCE_GOAL,
    INTERVAL_GOAL,
    TRACKING,
    PAUSED,
    SUMMARY,
}

data class WatchRunState(
    val screen: WatchScreen = WatchScreen.HOME,
    val goal: RunGoal = RunGoal.Free,
    val isPhoneConnected: Boolean = false,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val paceMinPerKm: Float = 0f,
    val heartRateBpm: Int? = null,
    val cadenceSpm: Int? = null,
    val gpsStatus: String = "SEARCHING",
    val intervalStep: Int = 1,
    val intervalIsWork: Boolean = true,
    val phoneStatusMessage: String? = null,
) {
    val progressPercent: Int?
        get() = GoalProgress.percent(goal, elapsedSeconds, distanceMeters)

    val remainingLabel: String?
        get() = GoalProgress.remaining(goal, elapsedSeconds, distanceMeters, intervalStep, intervalIsWork)
}

object GoalProgress {
    fun percent(goal: RunGoal, elapsedSeconds: Long, distanceMeters: Double): Int? = when (goal) {
        RunGoal.Free -> null
        is RunGoal.Time -> (elapsedSeconds * 100 / (goal.minutes * 60L))
            .toInt().coerceIn(0, 100)
        is RunGoal.Distance -> (distanceMeters * 100 / goal.meters)
            .toInt().coerceIn(0, 100)
        is RunGoal.Interval -> {
            val total = (goal.workSeconds + goal.restSeconds) * goal.sets
            (elapsedSeconds * 100 / total.coerceAtLeast(1)).toInt().coerceIn(0, 100)
        }
    }

    fun remaining(
        goal: RunGoal,
        elapsedSeconds: Long,
        distanceMeters: Double,
        intervalStep: Int,
        intervalIsWork: Boolean,
    ): String? = when (goal) {
        RunGoal.Free -> null
        is RunGoal.Time -> "${formatDuration((goal.minutes * 60L - elapsedSeconds).coerceAtLeast(0))} 남음"
        is RunGoal.Distance -> {
            val remainingKm = (goal.meters - distanceMeters).coerceAtLeast(0.0) / 1000.0
            "%.1fkm 남음".format(remainingKm)
        }
        is RunGoal.Interval -> "${if (intervalIsWork) "운동" else "회복"} $intervalStep/${goal.sets}"
    }
}

fun formatDuration(seconds: Long): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)

fun formatPace(paceMinPerKm: Float): String {
    if (paceMinPerKm <= 0f || !paceMinPerKm.isFinite()) return "--'--\""
    val minutes = paceMinPerKm.toInt()
    val seconds = ((paceMinPerKm - minutes) * 60).toInt().coerceIn(0, 59)
    return "%d'%02d\"".format(minutes, seconds)
}
