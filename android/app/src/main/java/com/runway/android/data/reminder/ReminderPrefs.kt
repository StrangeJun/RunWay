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
)
