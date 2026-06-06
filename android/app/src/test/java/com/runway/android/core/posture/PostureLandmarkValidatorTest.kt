package com.runway.android.core.posture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostureLandmarkValidatorTest {

    @Test
    fun `hides leg attached to a nearby background structure`() {
        val points = normalPose().toMutableList().apply {
            this[SKEL_R_KNEE] = point(0.62f, 0.58f)
            this[SKEL_R_ANKLE] = point(0.74f, 0.62f)
        }

        val result = PostureLandmarkValidator().validate(points)

        assertEquals(0f, result[SKEL_R_KNEE].v, 0f)
        assertEquals(0f, result[SKEL_R_ANKLE].v, 0f)
        assertTrue(result[SKEL_L_KNEE].v > 0f)
    }

    @Test
    fun `keeps balanced running legs visible`() {
        val result = PostureLandmarkValidator().validate(normalPose())

        assertTrue(result[SKEL_L_KNEE].v > 0f)
        assertTrue(result[SKEL_R_KNEE].v > 0f)
        assertTrue(result[SKEL_L_ANKLE].v > 0f)
        assertTrue(result[SKEL_R_ANKLE].v > 0f)
    }

    @Test
    fun `rejects a one frame ankle teleport`() {
        val validator = PostureLandmarkValidator()
        validator.validate(normalPose())
        val jumped = normalPose().toMutableList().apply {
            this[SKEL_R_ANKLE] = point(0.95f, 0.20f)
        }

        val result = validator.validate(jumped)

        assertEquals(0f, result[SKEL_R_ANKLE].v, 0f)
    }

    private fun normalPose(): List<SkeletonPoint> = listOf(
        point(0.50f, 0.16f),
        point(0.46f, 0.28f), point(0.54f, 0.28f),
        point(0.43f, 0.40f), point(0.57f, 0.40f),
        point(0.42f, 0.52f), point(0.58f, 0.52f),
        point(0.47f, 0.48f), point(0.53f, 0.48f),
        point(0.40f, 0.68f), point(0.60f, 0.68f),
        point(0.34f, 0.88f), point(0.66f, 0.88f),
    )

    private fun point(x: Float, y: Float) = SkeletonPoint(x, y, 0.95f)
}
