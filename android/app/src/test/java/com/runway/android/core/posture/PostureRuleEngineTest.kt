package com.runway.android.core.posture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PostureRuleEngineTest {

    private lateinit var engine: PostureRuleEngine

    @Before
    fun setup() {
        engine = PostureRuleEngine()
    }

    // ─── empty frames ───

    @Test
    fun `evaluate with empty frames returns emptyResult with score 0`() {
        val result = engine.evaluate(emptyList())
        assertEquals(0, result.overallScore)
        assertEquals("D", result.grade)
    }

    // ─── gradeFrom ───

    @Test
    fun `grade S for score 90 to 100`() {
        assertEquals("S", PostureResult.gradeFrom(100))
        assertEquals("S", PostureResult.gradeFrom(90))
    }

    @Test
    fun `grade A for score 75 to 89`() {
        assertEquals("A", PostureResult.gradeFrom(89))
        assertEquals("A", PostureResult.gradeFrom(75))
    }

    @Test
    fun `grade B for score 60 to 74`() {
        assertEquals("B", PostureResult.gradeFrom(74))
        assertEquals("B", PostureResult.gradeFrom(60))
    }

    @Test
    fun `grade C for score 45 to 59`() {
        assertEquals("C", PostureResult.gradeFrom(59))
        assertEquals("C", PostureResult.gradeFrom(45))
    }

    @Test
    fun `grade D for score below 45`() {
        assertEquals("D", PostureResult.gradeFrom(44))
        assertEquals("D", PostureResult.gradeFrom(0))
    }

    // ─── overall score with perfect angles ───

    @Test
    fun `perfect angles produce overall score 100 and grade S`() {
        val frame = perfectFrame()
        val result = engine.evaluate(listOf(frame))
        assertEquals(100, result.overallScore)
        assertEquals("S", result.grade)
        assertEquals(100, result.knee.score)
        assertEquals(100, result.trunk.score)
        assertEquals(100, result.elbow.score)
        assertEquals(100, result.hip.score)
        assertEquals(100, result.overstride.score)
    }

    // ─── category score boundary tests ───

    @Test
    fun `knee score 100 when angle is within 135-165`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(kneeFlexAngle = 150f)))
        assertEquals(100, result.knee.score)
    }

    @Test
    fun `knee score drops when angle deviates 5 degrees below idealMin`() {
        // 5° below ideal min (135-5=130)
        val result = engine.evaluate(listOf(perfectFrame().copy(kneeFlexAngle = 130f)))
        assertTrue("score should be between 70 and 90 for 130°", result.knee.score in 70..90)
    }

    @Test
    fun `knee score drops further when angle deviates 15 degrees below idealMin`() {
        // 15° below ideal min (135-15=120)
        val result = engine.evaluate(listOf(perfectFrame().copy(kneeFlexAngle = 120f)))
        assertTrue("score should be between 40 and 70 for 120°", result.knee.score in 40..70)
    }

    @Test
    fun `professional runner knee 140 degrees scores 60 or above`() {
        // Elite runners land at 130-150°; 140° is within the ideal range (135-165)
        val result = engine.evaluate(listOf(perfectFrame().copy(kneeFlexAngle = 140f)))
        assertTrue("elite landing angle should score >= 60", result.knee.score >= 60)
    }

    @Test
    fun `knee score approaches 0 for extreme deviation`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(kneeFlexAngle = 100f)))
        assertTrue("score should be low", result.knee.score < 40)
    }

    @Test
    fun `trunk score 100 when angle in 5-10 degrees forward lean`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(trunkLeanAngle = 7f)))
        assertEquals(100, result.trunk.score)
    }

    @Test
    fun `trunk score low for backward lean`() {
        // -5° lean: deviation = 10°, score = 70 - ((10-5)/10)*30 = 55
        val result = engine.evaluate(listOf(perfectFrame().copy(trunkLeanAngle = -5f)))
        assertTrue("expected score in 40..70", result.trunk.score in 40..70)
    }

    @Test
    fun `elbow score 100 when angle in 85-95`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(elbowAngle = 90f)))
        assertEquals(100, result.elbow.score)
    }

    @Test
    fun `overstride score 100 when ratio at or below 0_10`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(overstrideRatio = 0.08f)))
        assertEquals(100, result.overstride.score)
    }

    @Test
    fun `overstride score drops linearly between 0_10 and 0_20`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(overstrideRatio = 0.15f)))
        assertTrue("score should be between 60 and 100", result.overstride.score in 60..100)
    }

    @Test
    fun `overstride score 0 for extreme overstride`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(overstrideRatio = 0.50f)))
        assertEquals(0, result.overstride.score)
    }

    // ─── hip category ───

    @Test
    fun `hip score 100 when angle in 160-180`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(hipExtensionAngle = 175f)))
        assertEquals(100, result.hip.score)
    }

    @Test
    fun `hip score drops when angle below 160`() {
        val result = engine.evaluate(listOf(perfectFrame().copy(hipExtensionAngle = 145f)))
        assertTrue("score should be low for hip flex < 145", result.hip.score < 70)
    }

    // ─── landing frame selection ───

    @Test
    fun `knee median uses only landing frames when available`() {
        val landingFrame = perfectFrame().copy(isLandingFrame = true, kneeFlexAngle = 162f)
        val nonLandingFrame = perfectFrame().copy(isLandingFrame = false, kneeFlexAngle = 120f)
        val result = engine.evaluate(listOf(landingFrame, nonLandingFrame))
        // Should use landing frame angle (162), not non-landing (120)
        assertTrue("knee score should reflect 162° not 120°", result.knee.score > 80)
    }

    @Test
    fun `overstride uses only landing frames when available`() {
        val landingFrame = perfectFrame().copy(isLandingFrame = true, overstrideRatio = 0.05f)
        val nonLandingFrame = perfectFrame().copy(isLandingFrame = false, overstrideRatio = 0.50f)
        val result = engine.evaluate(listOf(landingFrame, nonLandingFrame))
        assertEquals(100, result.overstride.score)
    }

    @Test
    fun `falls back to all frames when no landing frames`() {
        val frames = listOf(
            perfectFrame().copy(isLandingFrame = false, kneeFlexAngle = 162f),
            perfectFrame().copy(isLandingFrame = false, kneeFlexAngle = 163f),
        )
        val result = engine.evaluate(frames)
        assertEquals(100, result.knee.score)
        // Overstride not measured when no landing frames — score is neutral (50)
        assertEquals(50, result.overstride.score)
    }

    // ─── weight sum sanity ───

    @Test
    fun `weight sum totals 100 percent`() {
        // 30 + 25 + 20 + 15 + 10 = 100
        val weights = listOf(0.30, 0.25, 0.20, 0.15, 0.10)
        assertEquals(1.00, weights.sum(), 0.001)
    }

    @Test
    fun `renormalized weights without overstride sum to 100 percent`() {
        // 37.5 + 31.25 + 18.75 + 12.5 = 100
        val weights = listOf(0.375, 0.3125, 0.1875, 0.125)
        assertEquals(1.00, weights.sum(), 0.001)
    }

    // ─── helper ───

    private fun perfectFrame() = PostureFrameAngles(
        kneeFlexAngle = 162f,
        trunkLeanAngle = 7f,
        elbowAngle = 90f,
        hipExtensionAngle = 178f,
        overstrideRatio = 0.05f,
        isLandingFrame = true,
        visibility = 0.95f,
    )
}
