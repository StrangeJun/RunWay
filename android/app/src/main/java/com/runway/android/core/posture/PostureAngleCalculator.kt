package com.runway.android.core.posture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

object PostureAngleCalculator {

    private const val NOSE = 0
    private const val LEFT_SHOULDER = 11
    private const val RIGHT_SHOULDER = 12
    private const val LEFT_ELBOW = 13
    private const val LEFT_WRIST = 15
    private const val LEFT_HIP = 23
    private const val RIGHT_HIP = 24
    private const val LEFT_KNEE = 25
    private const val RIGHT_KNEE = 26
    private const val LEFT_ANKLE = 27
    private const val RIGHT_ANKLE = 28

    fun compute(landmarks: List<NormalizedLandmark>): PostureFrameAngles? {
        if (landmarks.size <= RIGHT_ANKLE) return null

        val avgVis = listOf(LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE)
            .map { landmarks[it].visibility().orElse(0f) }
            .average()
            .toFloat()

        if (avgVis < 0.6f) return null

        val knee = threePointAngle(landmarks[LEFT_HIP], landmarks[LEFT_KNEE], landmarks[LEFT_ANKLE])
        val trunk = trunkLeanAngle(
            landmarks[LEFT_SHOULDER], landmarks[RIGHT_SHOULDER],
            landmarks[LEFT_HIP], landmarks[RIGHT_HIP],
        )
        val elbow = threePointAngle(landmarks[LEFT_SHOULDER], landmarks[LEFT_ELBOW], landmarks[LEFT_WRIST])
        val hip = threePointAngle(landmarks[LEFT_SHOULDER], landmarks[LEFT_HIP], landmarks[LEFT_KNEE])

        val hipMidX = (landmarks[LEFT_HIP].x() + landmarks[RIGHT_HIP].x()) / 2f
        val shoulderMidY = (landmarks[LEFT_SHOULDER].y() + landmarks[RIGHT_SHOULDER].y()) / 2f

        // Detect facing direction: right-facing when nose is to the right of hip midpoint.
        // For right-facing runners the leading (forward) foot has the highest x;
        // for left-facing runners it has the lowest x.
        val facingRight = landmarks[NOSE].x() > hipMidX
        val leftAnkleX = landmarks[LEFT_ANKLE].x()
        val rightAnkleX = landmarks[RIGHT_ANKLE].x()
        val leadingAnkleX = if (facingRight) maxOf(leftAnkleX, rightAnkleX) else minOf(leftAnkleX, rightAnkleX)
        val leadingAnkleY = when {
            facingRight -> if (leftAnkleX >= rightAnkleX) landmarks[LEFT_ANKLE].y() else landmarks[RIGHT_ANKLE].y()
            else -> if (leftAnkleX <= rightAnkleX) landmarks[LEFT_ANKLE].y() else landmarks[RIGHT_ANKLE].y()
        }

        val bodyHeight = abs(leadingAnkleY - shoulderMidY).coerceAtLeast(0.01f)
        // abs() handles both facing directions: deviation is always positive for a forward-landing foot.
        val overstrideRatio = (abs(leadingAnkleX - hipMidX) / bodyHeight).coerceAtLeast(0f)

        // Landing frame: leading ankle is in the lower 35% of frame (y > 0.65 in normalized coords)
        // and below the hip (ankle y > hip y in screen coords)
        val hipMidY = (landmarks[LEFT_HIP].y() + landmarks[RIGHT_HIP].y()) / 2f
        val isLanding = leadingAnkleY > 0.65f && leadingAnkleY > hipMidY

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
        return Math.toDegrees(atan2(dx.toDouble(), dy.toDouble())).toFloat()
    }
}
