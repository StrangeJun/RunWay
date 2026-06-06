package com.runway.wear.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressTest {
    @Test
    fun `time goal progress is clamped`() {
        val goal = RunGoal.Time(10, GoalCompletionAction.PAUSE)
        assertEquals(50, GoalProgress.percent(WatchRunState(goal = goal, elapsedSeconds = 300)))
        assertEquals(100, GoalProgress.percent(WatchRunState(goal = goal, elapsedSeconds = 900)))
    }

    @Test
    fun `distance goal reports remaining kilometers`() {
        assertEquals(
            "2.5km 남음",
            GoalProgress.remaining(
                WatchRunState(
                    goal = RunGoal.Distance(5_000, GoalCompletionAction.CONTINUE),
                    distanceMeters = 2_500.0,
                ),
            ),
        )
    }

    @Test
    fun `pace formatter handles missing data`() {
        assertEquals("--'--\"", formatPace(0f))
        assertEquals("5'30\"", formatPace(5.5f))
    }
}
