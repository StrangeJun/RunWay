package com.runway.android.core.posture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PostureFrameInterpolatorTest {

    // ─── bisectRight ───

    @Test
    fun `bisectRight returns 0 for empty list`() {
        assertEquals(0, bisectRight(emptyList(), 500L))
    }

    @Test
    fun `bisectRight returns size when all frames are before posMs`() {
        val frames = listOf(frame(100), frame(200), frame(300))
        assertEquals(3, bisectRight(frames, 400L))
    }

    @Test
    fun `bisectRight returns 0 when all frames are after posMs`() {
        val frames = listOf(frame(100), frame(200), frame(300))
        assertEquals(0, bisectRight(frames, 50L))
    }

    @Test
    fun `bisectRight returns correct index for exact match`() {
        // bisectRight: first index with t > posMs, so exact match goes to right side
        val frames = listOf(frame(100), frame(200), frame(300))
        assertEquals(2, bisectRight(frames, 200L))
    }

    @Test
    fun `bisectRight returns correct index between frames`() {
        val frames = listOf(frame(100), frame(200), frame(300))
        // 150 is between index 0 (t=100) and index 1 (t=200)
        assertEquals(1, bisectRight(frames, 150L))
    }

    // ─── interpolateFrame ───

    @Test
    fun `interpolateFrame returns null for empty list`() {
        assertNull(interpolateFrame(emptyList(), 100L))
    }

    @Test
    fun `interpolateFrame returns only frame for single-element list`() {
        val f = frame(100, 0.2f, 0.3f)
        assertSame(f, interpolateFrame(listOf(f), 50L))
    }

    @Test
    fun `interpolateFrame returns first frame when posMs before all frames`() {
        val frames = listOf(frame(100, 0.1f, 0.1f), frame(200, 0.9f, 0.9f))
        val result = interpolateFrame(frames, 50L)!!
        assertEquals(0.1f, result.pts[0].x, 0.001f)
    }

    @Test
    fun `interpolateFrame returns last frame when posMs after all frames`() {
        val frames = listOf(frame(100, 0.1f, 0.1f), frame(200, 0.9f, 0.9f))
        val result = interpolateFrame(frames, 300L)!!
        assertEquals(0.9f, result.pts[0].x, 0.001f)
    }

    @Test
    fun `interpolateFrame at exact midpoint gives 0_5 lerp`() {
        val frames = listOf(frame(0, 0.0f, 0.0f), frame(200, 1.0f, 1.0f))
        val result = interpolateFrame(frames, 100L)!!
        assertEquals(0.5f, result.pts[0].x, 0.001f)
        assertEquals(0.5f, result.pts[0].y, 0.001f)
    }

    @Test
    fun `interpolateFrame at t=0_25 gives correct lerp`() {
        val frames = listOf(frame(0, 0.0f, 0.0f), frame(400, 1.0f, 1.0f))
        val result = interpolateFrame(frames, 100L)!!
        assertEquals(0.25f, result.pts[0].x, 0.001f)
    }

    @Test
    fun `interpolateFrame at frame boundary returns that frame x`() {
        val a = frame(100, 0.2f, 0.3f)
        val b = frame(200, 0.8f, 0.7f)
        // posMs == a.t → t=0.0 → lerp returns a's values
        val result = interpolateFrame(listOf(a, b), 100L)!!
        assertEquals(0.2f, result.pts[0].x, 0.001f)
    }

    @Test
    fun `interpolateFrame returns before when pts sizes differ`() {
        val a = PostureVideoFrame(0L, listOf(SkeletonPoint(0f, 0f, 1f), SkeletonPoint(0f, 0f, 1f)))
        val b = PostureVideoFrame(100L, listOf(SkeletonPoint(1f, 1f, 1f)))  // different size
        val result = interpolateFrame(listOf(a, b), 50L)!!
        assertEquals(a, result)
    }

    @Test
    fun `interpolateFrame result has posMs as timestamp`() {
        val frames = listOf(frame(0, 0f, 0f), frame(200, 1f, 1f))
        val result = interpolateFrame(frames, 75L)!!
        assertEquals(75L, result.t)
    }

    // ─── duplicate timestamps ───

    @Test
    fun `no duplicate timestamps when all frames have unique t`() {
        val frames = listOf(frame(0), frame(100), frame(200), frame(300))
        val timestamps = frames.map { it.t }
        assertEquals(timestamps.size, timestamps.toSet().size)
    }

    // ─── helper ───

    private fun frame(t: Long, x: Float = 0f, y: Float = 0f) =
        PostureVideoFrame(t, listOf(SkeletonPoint(x, y, 1f)))
}
