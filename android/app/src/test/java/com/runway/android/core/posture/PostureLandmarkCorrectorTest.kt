package com.runway.android.core.posture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostureLandmarkCorrectorTest {

    @Test
    fun `moves side-profile knees behind facing direction`() {
        val rightFacing = landmarks(noseX = 0.62f)
        val correctedRight = PostureLandmarkCorrector().correct(rightFacing, 0L)
        assertTrue(correctedRight[SKEL_L_KNEE].x < rightFacing[SKEL_L_KNEE].x)
        assertTrue(correctedRight[SKEL_R_KNEE].x < rightFacing[SKEL_R_KNEE].x)

        val leftFacing = landmarks(noseX = 0.38f)
        val correctedLeft = PostureLandmarkCorrector().correct(leftFacing, 0L)
        assertTrue(correctedLeft[SKEL_L_KNEE].x > leftFacing[SKEL_L_KNEE].x)
        assertTrue(correctedLeft[SKEL_R_KNEE].x > leftFacing[SKEL_R_KNEE].x)
    }

    @Test
    fun `does not directionally shift knees in frontal view`() {
        val frontal = landmarks(noseX = 0.5f)
        val corrected = PostureLandmarkCorrector().correct(frontal, 0L)

        assertEquals(frontal[SKEL_L_KNEE].x, corrected[SKEL_L_KNEE].x, 0.0001f)
        assertEquals(frontal[SKEL_R_KNEE].x, corrected[SKEL_R_KNEE].x, 0.0001f)
    }

    private fun landmarks(noseX: Float): List<SkeletonPoint> = listOf(
        point(noseX, 0.2f),
        point(0.49f, 0.3f),
        point(0.51f, 0.3f),
        point(0.47f, 0.42f),
        point(0.53f, 0.42f),
        point(0.46f, 0.55f),
        point(0.54f, 0.55f),
        point(0.49f, 0.5f),
        point(0.51f, 0.5f),
        point(0.57f, 0.7f),
        point(0.59f, 0.7f),
        point(0.55f, 0.92f),
        point(0.57f, 0.92f),
    )

    private fun point(x: Float, y: Float) = SkeletonPoint(x, y, 0.95f)
}
