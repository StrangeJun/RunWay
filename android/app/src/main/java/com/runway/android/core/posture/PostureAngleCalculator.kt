package com.runway.android.core.posture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

object PostureAngleCalculator {

    // MediaPipe Pose landmark indices
    private const val LEFT_SHOULDER = 11
    private const val RIGHT_SHOULDER = 12
    private const val LEFT_ELBOW = 13
    private const val LEFT_WRIST = 15
    private const val LEFT_HIP = 23
    private const val RIGHT_HIP = 24
    private const val LEFT_KNEE = 25
    private const val LEFT_ANKLE = 27

    fun compute(landmarks: List<NormalizedLandmark>): PostureFrameAngles? {
        if (landmarks.size <= LEFT_ANKLE) return null

        val avgVis = listOf(LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE)
            .map { landmarks[it].visibility().orElse(0f) }
            .average()
            .toFloat()

        if (avgVis < 0.6f) return null

        val knee = threePointAngle(landmarks[LEFT_HIP], landmarks[LEFT_KNEE], landmarks[LEFT_ANKLE])
        val trunk = trunkLeanAngle(landmarks[LEFT_SHOULDER], landmarks[RIGHT_SHOULDER], landmarks[LEFT_HIP], landmarks[RIGHT_HIP])
        val elbow = threePointAngle(landmarks[LEFT_SHOULDER], landmarks[LEFT_ELBOW], landmarks[LEFT_WRIST])
        val hip = threePointAngle(landmarks[LEFT_SHOULDER], landmarks[LEFT_HIP], landmarks[LEFT_KNEE])

        val hipX = (landmarks[LEFT_HIP].x() + landmarks[RIGHT_HIP].x()) / 2f
        val ankleX = landmarks[LEFT_ANKLE].x()
        val shoulderY = (landmarks[LEFT_SHOULDER].y() + landmarks[RIGHT_SHOULDER].y()) / 2f
        val ankleY = (landmarks[LEFT_ANKLE].y())
        val bodyHeight = abs(ankleY - shoulderY).coerceAtLeast(0.01f)
        val overstrideRatio = ((ankleX - hipX) / bodyHeight).coerceAtLeast(0f)

        val prevAnkleY = landmarks[LEFT_ANKLE].y()
        val isLanding = prevAnkleY > 0.7f

        return PostureFrameAngles(
            kneeFlexAngle = knee,
            trunkLeanAngle = trunk,
            elbowAngle = elbow,
            hipExtensionAngle = hip,
            overstrideRatio = overstrideRatio,
            isLandingFrame = isLanding,
            visibility = avgVis,
        )
    }

    private fun threePointAngle(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Float {
        val ax = a.x() - b.x(); val ay = a.y() - b.y()
        val cx = c.x() - b.x(); val cy = c.y() - b.y()
        val dot = ax * cx + ay * cy
        val magA = sqrt(ax * ax + ay * ay)
        val magC = sqrt(cx * cx + cy * cy)
        if (magA < 1e-6f || magC < 1e-6f) return 180f
        val cosAngle = (dot / (magA * magC)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle).toDouble()).toFloat()
    }

    private fun trunkLeanAngle(
        ls: NormalizedLandmark,
        rs: NormalizedLandmark,
        lh: NormalizedLandmark,
        rh: NormalizedLandmark,
    ): Float {
        val shoulderMidX = (ls.x() + rs.x()) / 2f
        val shoulderMidY = (ls.y() + rs.y()) / 2f
        val hipMidX = (lh.x() + rh.x()) / 2f
        val hipMidY = (lh.y() + rh.y()) / 2f
        val dx = shoulderMidX - hipMidX
        val dy = hipMidY - shoulderMidY
        val angleRad = atan2(dx.toDouble(), dy.toDouble())
        return Math.toDegrees(angleRad).toFloat()
    }
}
