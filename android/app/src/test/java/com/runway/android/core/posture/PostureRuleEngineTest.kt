package com.runway.android.core.posture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PostureRuleEngineTest {
    private lateinit var engine: PostureRuleEngine

    @Before
    fun setup() {
        engine = PostureRuleEngine()
    }

    @Test
    fun `empty frames return score zero`() {
        assertEquals(0, engine.evaluate(emptyList()).overallScore)
    }

    @Test
    fun `original front knee threshold marks angle above 135 good`() {
        val result = engine.evaluate(listOf(frame(knee = 140f)))
        assertEquals(100, result.knee.score)
    }

    @Test
    fun `original front knee threshold marks 126 to 135 needs improvement`() {
        val result = engine.evaluate(listOf(frame(knee = 130f)))
        assertEquals(60, result.knee.score)
    }

    @Test
    fun `original front knee threshold marks below 126 bad`() {
        val result = engine.evaluate(listOf(frame(knee = 120f)))
        assertEquals(20, result.knee.score)
    }

    @Test
    fun `uses knee angle from detected foot strike`() {
        val result = engine.evaluate(
            listOf(
                frame(knee = 120f),
                frame(knee = 145f, leftStrike = true),
            ),
        )
        assertEquals(145f, result.knee.measuredValue, 0.001f)
        assertEquals(100, result.knee.score)
    }

    @Test
    fun `original torso threshold marks 5 to 15 good`() {
        assertEquals(100, engine.evaluate(listOf(frame(trunk = 12f))).trunk.score)
        assertEquals(60, engine.evaluate(listOf(frame(trunk = 3f))).trunk.score)
        assertEquals(20, engine.evaluate(listOf(frame(trunk = 20f))).trunk.score)
    }

    @Test
    fun `original elbow threshold marks 60 to 90 good`() {
        assertEquals(100, engine.evaluate(listOf(frame(elbow = 75f))).elbow.score)
        assertEquals(60, engine.evaluate(listOf(frame(elbow = 55f))).elbow.score)
        assertEquals(20, engine.evaluate(listOf(frame(elbow = 110f))).elbow.score)
    }

    @Test
    fun `original hip ankle and shank thresholds are evaluated at strike`() {
        val good = engine.evaluate(
            listOf(frame(hipAnkle = 10f, shank = 8f, leftStrike = true)),
        )
        val bad = engine.evaluate(
            listOf(frame(hipAnkle = 24f, shank = 18f, leftStrike = true)),
        )

        assertEquals(100, good.overstride.score)
        assertEquals(20, bad.overstride.score)
    }

    @Test
    fun `hip mobility tracks left and right swing cycles independently`() {
        val hips = listOf(
            10f to -10f,
            20f to -20f,
            35f to -35f,
            20f to -20f,
        )
        val frames = hips.map { (left, right) ->
            frame().copy(leftHipAngle = left, rightHipAngle = right)
        }

        val hip = engine.evaluate(frames).hip

        assertEquals(35f, hip.measuredValue, 0.001f)
        assertEquals(100, hip.score)
    }

    @Test
    fun `cadence counts both ankle strike events`() {
        val frames = listOf(
            frame(timestamp = 0L, leftStrike = true),
            frame(timestamp = 500L, rightStrike = true),
            frame(timestamp = 1_000L, leftStrike = true),
            frame(timestamp = 1_500L, rightStrike = true),
            frame(timestamp = 2_000L),
        )

        val cadence = engine.evaluate(frames).cadence
        assertEquals(120f, cadence.measuredValue, 0.001f)
        assertTrue(cadence.score > 0)
    }

    @Test
    fun `cadence uses alternating ankle phase instead of total video event count`() {
        val frames = (0 until 90).map { index ->
            val timestamp = index * (1_000L / 15L)
            val seconds = timestamp / 1_000.0
            val ankleDifference = (0.16 * sin(2.0 * PI * 1.5 * seconds)).toFloat()
            frame(timestamp = timestamp).copy(
                landmarks = ankleLandmarks(ankleDifference),
            )
        }

        val cadence = engine.evaluate(frames).cadence

        assertTrue(cadence.measuredValue in 175f..185f)
        assertTrue(cadence.score > 0)
    }

    @Test
    fun `legacy representative fields remain evaluable`() {
        val legacy = PostureFrameAngles(
            kneeFlexAngle = 140f,
            trunkLeanAngle = 10f,
            elbowAngle = 75f,
            hipExtensionAngle = 35f,
            overstrideRatio = 10f,
            isLandingFrame = true,
            visibility = 0.9f,
            shankAngle = 8f,
        )

        val result = engine.evaluate(listOf(legacy))
        assertEquals(100, result.knee.score)
        assertEquals(100, result.trunk.score)
        assertEquals(100, result.elbow.score)
        assertEquals(100, result.hip.score)
        assertEquals(100, result.overstride.score)
    }

    private fun frame(
        knee: Float = 140f,
        trunk: Float = 10f,
        elbow: Float = 75f,
        hip: Float = 35f,
        hipAnkle: Float = 10f,
        shank: Float = 8f,
        leftStrike: Boolean = false,
        rightStrike: Boolean = false,
        timestamp: Long = 0L,
    ) = PostureFrameAngles(
        kneeFlexAngle = knee,
        trunkLeanAngle = trunk,
        elbowAngle = elbow,
        hipExtensionAngle = hip,
        overstrideRatio = hipAnkle,
        isLandingFrame = leftStrike || rightStrike,
        visibility = 0.95f,
        timestampMs = timestamp,
        hipMidY = 0.5f,
        nearAnkleY = 0.9f,
        shankAngle = shank,
        leftKneeAngle = knee,
        leftElbowAngle = elbow,
        leftHipAngle = hip,
        leftHipAnkleAngle = hipAnkle,
        leftShankAngle = shank,
        leftFootStrike = leftStrike,
        rightFootStrike = rightStrike,
    )

    private fun ankleLandmarks(difference: Float): List<SkeletonPoint> =
        MutableList(13) { SkeletonPoint(0.5f, 0.5f, 0.95f) }.apply {
            this[SKEL_L_ANKLE] = SkeletonPoint(0.45f, 0.75f + difference / 2f, 0.95f)
            this[SKEL_R_ANKLE] = SkeletonPoint(0.55f, 0.75f - difference / 2f, 0.95f)
        }
}
