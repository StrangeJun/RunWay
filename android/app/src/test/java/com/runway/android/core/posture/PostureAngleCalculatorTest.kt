package com.runway.android.core.posture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PostureAngleCalculatorTest {

    @Test
    fun `calculates bilateral knee angles in pixel aspect space`() {
        val result = PostureAngleCalculator.compute(landmarks(), aspectRatio = 16f / 9f)

        assertNotNull(result)
        assertEquals(180f, result!!.leftKneeAngle!!, 0.1f)
        assertEquals(180f, result.rightKneeAngle!!, 0.1f)
    }

    @Test
    fun `drops individual angle below original confidence threshold`() {
        val points = landmarks().toMutableList()
        points[SKEL_L_KNEE] = points[SKEL_L_KNEE].copy(v = 0.2f)

        val result = PostureAngleCalculator.compute(points)

        assertEquals(null, result?.leftKneeAngle)
        assertNotNull(result?.rightKneeAngle)
    }

    private fun landmarks(): List<SkeletonPoint> = listOf(
        point(0.50f, 0.10f),
        point(0.45f, 0.25f),
        point(0.55f, 0.25f),
        point(0.45f, 0.40f),
        point(0.55f, 0.40f),
        point(0.45f, 0.55f),
        point(0.55f, 0.55f),
        point(0.45f, 0.50f),
        point(0.55f, 0.50f),
        point(0.45f, 0.70f),
        point(0.55f, 0.70f),
        point(0.45f, 0.90f),
        point(0.55f, 0.90f),
    )

    private fun point(x: Float, y: Float) = SkeletonPoint(x, y, 0.95f)
}
