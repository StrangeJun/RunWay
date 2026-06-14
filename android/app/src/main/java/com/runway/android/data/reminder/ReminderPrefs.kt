package com.runway.android.data.reminder

import java.util.Calendar

data class ReminderPrefs(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val enabledDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.WEDNESDAY,
        Calendar.FRIDAY,
    ),
    val plans: Map<Int, DayRunningPlan> = emptyMap(),
)

data class DayRunningPlan(
    val dayOfWeek: Int,
    val workoutType: ReminderWorkoutType = ReminderWorkoutType.FREE,
    val distanceKm: String = "",
    val targetPace: String = "",
    val durationMinutes: String = "",
    val note: String = "",
)

enum class ReminderWorkoutType(val label: String) {
    FREE("자유 달리기"),
    EASY("이지런"),
    RECOVERY("회복런"),
    TEMPO("템포런"),
    INTERVAL("인터벌"),
    LONG("롱런"),
}
