package com.runway.wear.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressTest {
    @Test
    fun `time goal progress is clamped`() {
        assertEquals(50, GoalProgress.percent(RunGoal.Time(10), 300, 0.0))
        assertEquals(100, GoalProgress.percent(RunGoal.Time(10), 900, 0.0))
    }

    @Test
    fun `distance goal reports remaining kilometers`() {
        assertEquals(
            "2.5km 남음",
            GoalProgress.remaining(RunGoal.Distance(5_000), 0, 2_500.0, 1, true),
        )
    }

    @Test
    fun `pace formatter handles missing data`() {
        assertEquals("--'--\"", formatPace(0f))
        assertEquals("5'30\"", formatPace(5.5f))
    }
}
