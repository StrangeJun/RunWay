package com.runway.wear.model

enum class GoalCompletionAction {
    PAUSE,
    CONTINUE,
}

sealed interface IntervalTarget {
    data class Time(val seconds: Int) : IntervalTarget
    data class Distance(val meters: Int) : IntervalTarget
}

sealed interface RunGoal {
    val completionAction: GoalCompletionAction

    data object Free : RunGoal {
        override val completionAction = GoalCompletionAction.CONTINUE
    }

    data class Time(
        val minutes: Int,
        override val completionAction: GoalCompletionAction,
    ) : RunGoal

    data class Distance(
        val meters: Int,
        override val completionAction: GoalCompletionAction,
    ) : RunGoal

    data class Interval(
        val work: IntervalTarget,
        val recovery: IntervalTarget,
        val sets: Int,
        override val completionAction: GoalCompletionAction,
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

enum class PhoneAuthState {
    CHECKING,
    LOGGED_OUT,
    LOGGED_IN,
}

data class WatchRunState(
    val screen: WatchScreen = WatchScreen.HOME,
    val goal: RunGoal = RunGoal.Free,
    val isPhoneConnected: Boolean = false,
    val phoneAuthState: PhoneAuthState = PhoneAuthState.CHECKING,
    val authMessage: String? = null,
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
    val intervalSegmentProgress: Int = 0,
    val intervalRemainingLabel: String? = null,
    val goalCompleted: Boolean = false,
    val phoneStatusMessage: String? = null,
) {
    val progressPercent: Int?
        get() = GoalProgress.percent(this)

    val remainingLabel: String?
        get() = GoalProgress.remaining(this)
}

object GoalProgress {
    fun percent(state: WatchRunState): Int? = when (val goal = state.goal) {
        RunGoal.Free -> null
        is RunGoal.Time -> (state.elapsedSeconds * 100 / (goal.minutes * 60L))
            .toInt().coerceIn(0, 100)
        is RunGoal.Distance -> (state.distanceMeters * 100 / goal.meters)
            .toInt().coerceIn(0, 100)
        is RunGoal.Interval -> {
            val completedSegments = (state.intervalStep - 1) * 2 +
                if (state.intervalIsWork) 0 else 1
            ((completedSegments * 100 + state.intervalSegmentProgress) / (goal.sets * 2))
                .coerceIn(0, 100)
        }
    }

    fun remaining(state: WatchRunState): String? {
        if (state.goalCompleted) return "목표 달성 · 계속 기록 중"
        return when (val goal = state.goal) {
            RunGoal.Free -> null
            is RunGoal.Time ->
                "${formatDuration((goal.minutes * 60L - state.elapsedSeconds).coerceAtLeast(0))} 남음"
            is RunGoal.Distance -> {
                val remainingKm = (goal.meters - state.distanceMeters).coerceAtLeast(0.0) / 1000.0
                "%.1fkm 남음".format(remainingKm)
            }
            is RunGoal.Interval -> {
                val phase = if (state.intervalIsWork) "운동" else "회복"
                "$phase ${state.intervalStep}/${goal.sets} · ${state.intervalRemainingLabel.orEmpty()}"
            }
        }
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
