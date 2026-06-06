package com.runway.android.core.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchCommandParserTest {
    @Test
    fun `parses distance goal command`() {
        val command = WatchCommandParser.parse(
            """{"type":"START_DISTANCE_GOAL_RUN","targetMeters":5000}""".encodeToByteArray(),
        )

        assertEquals(
            WatchCommand.StartDistanceGoalRun(
                targetMeters = 5000,
                completionAction = WatchGoalCompletionAction.CONTINUE,
            ),
            command,
        )
    }

    @Test
    fun `clamps interval values to supported range`() {
        val command = WatchCommandParser.parse(
            """{"type":"START_INTERVAL_RUN","workSeconds":10,"restSeconds":10,"sets":99}"""
                .encodeToByteArray(),
        )

        assertEquals(
            WatchCommand.StartIntervalRun(
                work = WatchIntervalTarget.Time(10),
                recovery = WatchIntervalTarget.Time(10),
                sets = 20,
                completionAction = WatchGoalCompletionAction.CONTINUE,
            ),
            command,
        )
    }

    @Test
    fun `parses distance interval and pause completion action`() {
        val command = WatchCommandParser.parse(
            """
            {
              "type":"START_INTERVAL_RUN",
              "workType":"DISTANCE",
              "workMeters":400,
              "recoveryType":"DISTANCE",
              "recoveryMeters":200,
              "sets":6,
              "completionAction":"PAUSE"
            }
            """.trimIndent().encodeToByteArray(),
        )

        assertEquals(
            WatchCommand.StartIntervalRun(
                work = WatchIntervalTarget.Distance(400),
                recovery = WatchIntervalTarget.Distance(200),
                sets = 6,
                completionAction = WatchGoalCompletionAction.PAUSE,
            ),
            command,
        )
    }

    @Test
    fun `rejects unknown or malformed command`() {
        assertNull(WatchCommandParser.parse("""{"type":"UNKNOWN"}""".encodeToByteArray()))
        assertNull(WatchCommandParser.parse("not-json".encodeToByteArray()))
    }
}
