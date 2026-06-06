package com.runway.android.core.posture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PostureSkeletonSmootherTest {

    @Test
    fun `first smooth call preserves coordinates`() {
        val raw = listOf(pt(0.5f, 0.5f, 0.9f), pt(0.1f, 0.2f, 0.8f))
        val result = PostureSkeletonSmoother().smooth(raw, 0L)

        assertEquals(raw[0].x, result[0].x, 0.0001f)
        assertEquals(raw[0].y, result[0].y, 0.0001f)
        assertEquals(raw[1].x, result[1].x, 0.0001f)
        assertEquals(raw[1].y, result[1].y, 0.0001f)
    }

    @Test
    fun `repeated visible samples converge toward input`() {
        val smoother = PostureSkeletonSmoother()
        smoother.smooth(listOf(pt(0f, 0f, 0.9f)), 0L)

        var result = emptyList<SkeletonPoint>()
        repeat(30) { index ->
            result = smoother.smooth(listOf(pt(0.8f, 0.6f, 0.9f)), (index + 1) * 100L)
        }

        assertTrue(kotlin.math.abs(result[0].x - 0.8f) < 0.01f)
        assertTrue(kotlin.math.abs(result[0].y - 0.6f) < 0.01f)
    }

    @Test
    fun `low visibility sample stays at last reliable coordinate`() {
        val smoother = PostureSkeletonSmoother(lowVisFreezeThreshold = 0.45f)
        smoother.smooth(listOf(pt(0.2f, 0.3f, 0.9f)), 0L)
        val result = smoother.smooth(listOf(pt(0.9f, 0.8f, 0.2f)), 100L)

        assertEquals(0.2f, result[0].x, 0.0001f)
        assertEquals(0.3f, result[0].y, 0.0001f)
    }

    @Test
    fun `faster movement receives a more responsive cutoff`() {
        val slow = PostureSkeletonSmoother(minCutoff = 1f, beta = 4f)
        val fast = PostureSkeletonSmoother(minCutoff = 1f, beta = 4f)
        slow.smooth(listOf(pt(0f, 0f, 0.9f)), 0L)
        fast.smooth(listOf(pt(0f, 0f, 0.9f)), 0L)

        val slowTarget = 0.02f
        val fastTarget = 0.2f
        val slowResult = slow.smooth(listOf(pt(slowTarget, 0f, 0.9f)), 100L)
        val fastResult = fast.smooth(listOf(pt(fastTarget, 0f, 0.9f)), 100L)

        assertTrue(fastResult[0].x / fastTarget > slowResult[0].x / slowTarget)
    }

    @Test
    fun `reset causes next coordinates to pass through`() {
        val smoother = PostureSkeletonSmoother()
        smoother.smooth(listOf(pt(0f, 0f, 0.9f)), 0L)
        smoother.reset()

        val raw = pt(0.7f, 0.3f, 0.9f)
        val result = smoother.smooth(listOf(raw), 100L)
        assertEquals(raw.x, result[0].x, 0.0001f)
        assertEquals(raw.y, result[0].y, 0.0001f)
    }

    @Test
    fun `landmark count change reinitializes filters`() {
        val smoother = PostureSkeletonSmoother()
        smoother.smooth(listOf(pt(0f, 0f, 1f), pt(0f, 0f, 1f)), 0L)

        val raw = pt(0.5f, 0.5f, 0.8f)
        val result = smoother.smooth(listOf(raw), 100L)
        assertEquals(raw.x, result[0].x, 0.0001f)
        assertEquals(raw.y, result[0].y, 0.0001f)
    }

    @Test
    fun `smooth returns a new list`() {
        val smoother = PostureSkeletonSmoother()
        val raw = listOf(pt(0f, 0f, 1f))
        val result = smoother.smooth(raw, 0L)

        assertNotSame(raw, result)
    }

    private fun pt(x: Float, y: Float, v: Float) = SkeletonPoint(x, y, v)
}
