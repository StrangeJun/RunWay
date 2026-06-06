package com.runway.android.core.posture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunningFormStrikeDetectorTest {

    @Test
    fun `detects original y-axis local minimum after upward swing`() {
        val detector = RunningFormStrikeDetector(windowSize = 3)

        assertFalse(detector.update(point(0.70f), 0L))
        assertFalse(detector.update(point(0.65f), 500L))
        assertTrue(detector.update(point(0.69f), 1_000L))
    }

    @Test
    fun `suppresses strikes closer than 400 milliseconds`() {
        val detector = RunningFormStrikeDetector(windowSize = 3)
        detector.update(point(0.70f), 0L)
        detector.update(point(0.65f), 100L)
        assertTrue(detector.update(point(0.69f), 200L))

        detector.update(point(0.64f), 300L)
        assertFalse(detector.update(point(0.69f), 400L))
    }

    private fun point(y: Float) = SkeletonPoint(x = 0.5f, y = y, v = 0.9f)
}
