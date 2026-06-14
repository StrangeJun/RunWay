package com.runway.android.data.reminder

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlanFormatterTest {
    @Test
    fun combinesMultipleOptionalTargets() {
        val plan = DayRunningPlan(
            dayOfWeek = Calendar.MONDAY,
            workoutType = ReminderWorkoutType.TEMPO,
            distanceKm = "8",
            targetPace = "5:20",
            durationMinutes = "45",
            note = "마지막 1km 가속",
        )

        assertEquals(
            "템포런 · 8km · 5:20/km · 45분 · 마지막 1km 가속",
            ReminderPlanFormatter.summary(plan),
        )
    }

    @Test
    fun ignoresInvalidTargets() {
        val plan = DayRunningPlan(
            dayOfWeek = Calendar.MONDAY,
            workoutType = ReminderWorkoutType.EASY,
            distanceKm = "0",
            targetPace = "5:99",
            durationMinutes = "",
        )

        assertEquals("이지런", ReminderPlanFormatter.summary(plan))
        assertTrue(ReminderPlanFormatter.isValidPace("6:05"))
        assertFalse(ReminderPlanFormatter.isValidPace("6:75"))
    }
}
