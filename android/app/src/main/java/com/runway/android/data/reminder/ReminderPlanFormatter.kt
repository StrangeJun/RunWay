package com.runway.android.data.reminder

object ReminderPlanFormatter {
    fun summary(plan: DayRunningPlan?): String {
        if (plan == null) return "가볍게 달릴 준비를 해보세요."

        val targets = buildList {
            plan.distanceKm.trim()
                .takeIf { it.toDoubleOrNull()?.let { value -> value > 0 } == true }
                ?.let { add("${it}km") }
            plan.targetPace.trim()
                .takeIf(::isValidPace)
                ?.let { add("$it/km") }
            plan.durationMinutes.trim()
                .takeIf { it.toIntOrNull()?.let { value -> value > 0 } == true }
                ?.let { add("${it}분") }
        }

        return buildString {
            append(plan.workoutType.label)
            if (targets.isNotEmpty()) append(" · ${targets.joinToString(" · ")}")
            plan.note.trim().takeIf { it.isNotEmpty() }?.let { append(" · $it") }
        }
    }

    fun isValidPace(value: String): Boolean {
        val parts = value.split(":")
        if (parts.size != 2) return false
        val minutes = parts[0].toIntOrNull() ?: return false
        val seconds = parts[1].toIntOrNull() ?: return false
        return minutes > 0 && seconds in 0..59
    }
}
