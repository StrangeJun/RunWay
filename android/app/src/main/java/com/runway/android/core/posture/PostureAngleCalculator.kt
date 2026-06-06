package com.runway.android.core.posture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

object PostureAngleCalculator {

    private const val NOSE = 0
    private const val LEFT_SHOULDER = 11; private const val RIGHT_SHOULDER = 12
    private const val LEFT_ELBOW = 13;    private const val RIGHT_ELBOW = 14
    private const val LEFT_WRIST = 15;    private const val RIGHT_WRIST = 16
    private const val LEFT_HIP = 23;      private const val RIGHT_HIP = 24
    private const val LEFT_KNEE = 25;     private const val RIGHT_KNEE = 26
    private const val LEFT_ANKLE = 27;    private const val RIGHT_ANKLE = 28

    fun compute(landmarks: List<NormalizedLandmark>): PostureFrameAngles? {
        if (landmarks.size <= RIGHT_ANKLE) return null

        // Pick the body side with better average visibility.
        // For side-profile running video, one side faces the camera and has higher confidence.
        val leftVis = avg(landmarks, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE)
        val rightVis = avg(landmarks, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE)
        val bestVis = maxOf(leftVis, rightVis)

        if (bestVis < 0.55f) return null

        val useLeft = leftVis >= rightVis
        val S = if (useLeft) LEFT_SHOULDER else RIGHT_SHOULDER
        val E = if (useLeft) LEFT_ELBOW else RIGHT_ELBOW
        val W = if (useLeft) LEFT_WRIST else RIGHT_WRIST
        val H = if (useLeft) LEFT_HIP else RIGHT_HIP
        val K = if (useLeft) LEFT_KNEE else RIGHT_KNEE
        val A = if (useLeft) LEFT_ANKLE else RIGHT_ANKLE

        val kneeAngle  = threePointAngle(landmarks[H], landmarks[K], landmarks[A])
        val elbowAngle = threePointAngle(landmarks[S], landmarks[E], landmarks[W])
        val hipAngle   = threePointAngle(landmarks[S], landmarks[H], landmarks[K])
        val trunkAngle = trunkLeanAngle(
            landmarks[LEFT_SHOULDER], landmarks[RIGHT_SHOULDER],
            landmarks[LEFT_HIP], landmarks[RIGHT_HIP],
        )

        // Overstride: compare leading (forward) ankle to hip midpoint
        val hipMidX    = (landmarks[LEFT_HIP].x() + landmarks[RIGHT_HIP].x()) / 2f
        val hipMidY    = (landmarks[LEFT_HIP].y() + landmarks[RIGHT_HIP].y()) / 2f
        val shoulderMidY = (landmarks[LEFT_SHOULDER].y() + landmarks[RIGHT_SHOULDER].y()) / 2f
        val facingRight = landmarks[NOSE].x() > hipMidX

        val leftAnkleX  = landmarks[LEFT_ANKLE].x()
        val rightAnkleX = landmarks[RIGHT_ANKLE].x()
        val leadingAnkleX = if (facingRight) maxOf(leftAnkleX, rightAnkleX) else minOf(leftAnkleX, rightAnkleX)
        val leadingAnkleY = when {
            facingRight -> if (leftAnkleX >= rightAnkleX) landmarks[LEFT_ANKLE].y() else landmarks[RIGHT_ANKLE].y()
            else        -> if (leftAnkleX <= rightAnkleX) landmarks[LEFT_ANKLE].y() else landmarks[RIGHT_ANKLE].y()
        }

        val bodyHeight = abs(leadingAnkleY - shoulderMidY).coerceAtLeast(0.01f)
        val overstrideRatio = (abs(leadingAnkleX - hipMidX) / bodyHeight).coerceAtLeast(0f)
        val isLanding = leadingAnkleY > 0.65f && leadingAnkleY > hipMidY

        val nearAnkleY = landmarks[A].y()

        // Shank angle: angle between shin (knee→ankle) and vertical [0,1]
        // running-form-analyzer: horizontal_vector=[0,1], shin_vector=ankle-knee
        val shankAngle = run {
            val kx = landmarks[K].x(); val ky = landmarks[K].y()
            val ax = landmarks[A].x(); val ay = landmarks[A].y()
            val shinX = ax - kx; val shinY = ay - ky
            val mag = sqrt(shinX * shinX + shinY * shinY)
            if (mag < 1e-6f) 0f
            else Math.toDegrees(acos((shinY / mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
        }

        // Arm swing angle: angle between torso vector (shoulder→hip) and upper-arm (shoulder→elbow)
        val armSwingAngle = run {
            val sx = landmarks[S].x(); val sy = landmarks[S].y()
            val hx = landmarks[H].x(); val hy = landmarks[H].y()
            val ex = landmarks[E].x(); val ey = landmarks[E].y()
            val torsoX = hx - sx; val torsoY = hy - sy
            val armX = ex - sx;  val armY = ey - sy
            val dot = torsoX * armX + torsoY * armY
            val mag = sqrt((torsoX*torsoX+torsoY*torsoY) * (armX*armX+armY*armY))
            if (mag < 1e-6f) 0f
            else Math.toDegrees(acos((dot / mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
        }

        return PostureFrameAngles(
            kneeFlexAngle = kneeAngle,
            trunkLeanAngle = trunkAngle,
            elbowAngle = elbowAngle,
            hipExtensionAngle = hipAngle,
            overstrideRatio = overstrideRatio,
            isLandingFrame = isLanding,
            visibility = bestVis,
            hipMidY = hipMidY,
            nearAnkleY = nearAnkleY,
            shankAngle = shankAngle,
            armSwingAngle = armSwingAngle,
        )
    }

    private fun avg(landmarks: List<NormalizedLandmark>, vararg indices: Int): Float =
        indices.map { landmarks[it].visibility().orElse(0f) }.average().toFloat()

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
        ls: NormalizedLandmark, rs: NormalizedLandmark,
        lh: NormalizedLandmark, rh: NormalizedLandmark,
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
