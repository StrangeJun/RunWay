package com.runway.android.ui.home

sealed class RunGoal {

    data class TimeGoal(val targetMinutes: Int) : RunGoal() {
        fun label() = "${targetMinutes}분"
    }

    data class DistanceGoal(val targetKm: Float) : RunGoal() {
        fun label() = if (targetKm == targetKm.toInt().toFloat()) "${targetKm.toInt()}km"
                      else "${targetKm}km"
    }

    data class IntervalGoal(
        val warmup: IntervalSegment,
        val work: IntervalSegment,
        val recovery: IntervalSegment,
        val cooldown: IntervalSegment? = null,
        val sets: Int,
    ) : RunGoal() {
        fun label() = "인터벌 ${sets}회"
    }
}

// ── Interval building blocks ──────────────────────────────────────────────────

sealed class IntervalDuration {
    data class ByTime(val minutes: Int) : IntervalDuration() {
        fun label() = "${minutes}분"
    }
    data class ByDistance(val meters: Int) : IntervalDuration() {
        fun label() = if (meters >= 1000) "${"%.1f".format(meters / 1000f)}km" else "${meters}m"
    }
    object None : IntervalDuration() {
        fun label() = "없음"
    }
}

data class IntervalSegment(
    val duration: IntervalDuration,
    val paceTargetSecPerKm: Int? = null,    // null = no pace target
) {
    fun paceLabel() = paceTargetSecPerKm?.let {
        val m = it / 60; val s = it % 60
        "%d'%02d\"/km".format(m, s)
    }
}

// ── Preset values ─────────────────────────────────────────────────────────────

object GoalPresets {
    val timeMinutes = listOf(10, 15, 20, 30, 45, 60, 90)
    val distanceKm  = listOf(1f, 3f, 5f, 10f, 15f, 21.1f, 42.2f)

    val warmupTimes     = listOf(5, 10, 15, 20)          // minutes
    val warmupDistances = listOf(500, 1000, 1500, 2000)   // metres

    val workDistances   = listOf(200, 400, 800, 1000, 2000, 3000, 5000) // metres
    val workPaces       = listOf(240, 270, 300, 330, 360, 390, 420)     // sec/km

    val recoveryDistances = listOf(100, 200, 400, 800)   // metres
    val recoveryPaces     = listOf(360, 390, 420, 450)   // sec/km

    val sets = listOf(1, 2, 3, 4, 5, 6, 8, 10)

    fun paceLabel(secPerKm: Int): String {
        val m = secPerKm / 60; val s = secPerKm % 60
        return "%d'%02d\"".format(m, s)
    }
}
