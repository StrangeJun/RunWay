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

        assertEquals(WatchCommand.StartDistanceGoalRun(5000), command)
    }

    @Test
    fun `clamps interval values to supported range`() {
        val command = WatchCommandParser.parse(
            """{"type":"START_INTERVAL_RUN","workSeconds":10,"restSeconds":10,"sets":99}"""
                .encodeToByteArray(),
        )

        assertEquals(
            WatchCommand.StartIntervalRun(workSeconds = 60, restSeconds = 60, sets = 20),
            command,
        )
    }

    @Test
    fun `rejects unknown or malformed command`() {
        assertNull(WatchCommandParser.parse("""{"type":"UNKNOWN"}""".encodeToByteArray()))
        assertNull(WatchCommandParser.parse("not-json".encodeToByteArray()))
    }
}
