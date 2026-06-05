package com.runway.android.core.posture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PostureSkeletonSmootherTest {

    private val defaultSmoother = PostureSkeletonSmoother(alpha = 0.4f, maxDelta = 0.12f, lowVisAlpha = 0.15f)

    // ─── init validation ───

    @Test(expected = IllegalArgumentException::class)
    fun `alpha above 1 throws`() {
        PostureSkeletonSmoother(alpha = 1.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `alpha below 0 throws`() {
        PostureSkeletonSmoother(alpha = -0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative maxDelta throws`() {
        PostureSkeletonSmoother(maxDelta = -0.01f)
    }

    // ─── first call passes through unchanged ───

    @Test
    fun `first smooth call returns raw values unchanged`() {
        val raw = listOf(pt(0.5f, 0.5f, 0.9f), pt(0.1f, 0.2f, 0.8f))
        val result = defaultSmoother.smooth(raw)
        assertEquals(raw, result)
    }

    // ─── EMA converges toward raw ───

    @Test
    fun `repeated calls with same input converge to that value`() {
        val smoother = PostureSkeletonSmoother(alpha = 0.4f, maxDelta = 1f)
        val target = listOf(pt(0.8f, 0.6f, 0.9f))
        // First call seeds state
        smoother.smooth(listOf(pt(0.0f, 0.0f, 0.9f)))
        // Repeatedly apply target
        var last = smoother.smooth(target)
        repeat(20) { last = smoother.smooth(target) }
        assertTrue("x should converge within 0.01 of 0.8", kotlin.math.abs(last[0].x - 0.8f) < 0.01f)
        assertTrue("y should converge within 0.01 of 0.6", kotlin.math.abs(last[0].y - 0.6f) < 0.01f)
    }

    // ─── jump clamping ───

    @Test
    fun `jump larger than maxDelta is clamped`() {
        val smoother = PostureSkeletonSmoother(alpha = 1.0f, maxDelta = 0.10f)
        smoother.smooth(listOf(pt(0.0f, 0.0f, 0.9f)))
        // Raw jump of 0.5 should be clamped to 0.10
        val result = smoother.smooth(listOf(pt(0.5f, 0.0f, 0.9f)))
        assertEquals(0.10f, result[0].x, 0.001f)
    }

    @Test
    fun `jump within maxDelta passes through at alpha=1`() {
        val smoother = PostureSkeletonSmoother(alpha = 1.0f, maxDelta = 0.20f)
        smoother.smooth(listOf(pt(0.0f, 0.0f, 0.9f)))
        val result = smoother.smooth(listOf(pt(0.15f, 0.0f, 0.9f)))
        assertEquals(0.15f, result[0].x, 0.001f)
    }

    // ─── low-visibility uses smaller alpha ───

    @Test
    fun `low visibility landmark moves less than high visibility`() {
        val smootherHigh = PostureSkeletonSmoother(alpha = 0.4f, maxDelta = 1f, lowVisAlpha = 0.05f)
        val smootherLow  = PostureSkeletonSmoother(alpha = 0.4f, maxDelta = 1f, lowVisAlpha = 0.05f)

        smootherHigh.smooth(listOf(pt(0.0f, 0.0f, 0.9f)))   // seed
        smootherLow.smooth(listOf(pt(0.0f, 0.0f, 0.3f)))    // seed

        val highResult = smootherHigh.smooth(listOf(pt(1.0f, 0.0f, 0.9f)))
        val lowResult  = smootherLow.smooth(listOf(pt(1.0f, 0.0f, 0.3f)))

        // High-vis moves more (alpha=0.4) than low-vis (alpha=0.05)
        assertTrue("high-vis should move more than low-vis", highResult[0].x > lowResult[0].x)
    }

    // ─── reset ───

    @Test
    fun `reset causes next call to return raw values`() {
        val smoother = PostureSkeletonSmoother()
        smoother.smooth(listOf(pt(0.0f, 0.0f, 0.9f)))
        smoother.reset()
        val raw = listOf(pt(0.7f, 0.3f, 0.9f))
        assertEquals(raw, smoother.smooth(raw))
    }

    // ─── size mismatch falls back to raw ───

    @Test
    fun `mismatched size resets and returns raw`() {
        val smoother = PostureSkeletonSmoother()
        smoother.smooth(listOf(pt(0f, 0f, 1f), pt(0f, 0f, 1f)))
        // Different size — smoother should return raw without crashing
        val raw = listOf(pt(0.5f, 0.5f, 0.8f))
        val result = smoother.smooth(raw)
        assertEquals(raw, result)
    }

    // ─── output is a new list, not mutation of input ───

    @Test
    fun `smooth returns new list not input reference`() {
        val smoother = PostureSkeletonSmoother()
        val raw = listOf(pt(0f, 0f, 1f))
        smoother.smooth(raw)
        val raw2 = listOf(pt(0.5f, 0.5f, 1f))
        val result = smoother.smooth(raw2)
        assertNotSame(raw2, result)
    }

    // ─── helper ───

    private fun pt(x: Float, y: Float, v: Float) = SkeletonPoint(x, y, v)
}
