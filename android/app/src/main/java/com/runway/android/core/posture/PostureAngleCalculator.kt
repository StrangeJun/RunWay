package com.runway.android.core.posture

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

object PostureAngleCalculator {

    fun compute(landmarks: List<SkeletonPoint>): PostureFrameAngles? {
        if (landmarks.size <= SKEL_R_ANKLE) return null

        // Pick the body side with better average visibility.
        // For side-profile running video, one side faces the camera and has higher confidence.
        val leftVis = avg(landmarks, SKEL_L_SHOULDER, SKEL_L_ELBOW, SKEL_L_WRIST, SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE)
        val rightVis = avg(landmarks, SKEL_R_SHOULDER, SKEL_R_ELBOW, SKEL_R_WRIST, SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE)
        val bestVis = maxOf(leftVis, rightVis)

        if (bestVis < 0.55f) return null

        val useLeft = leftVis >= rightVis
        val S = if (useLeft) SKEL_L_SHOULDER else SKEL_R_SHOULDER
        val E = if (useLeft) SKEL_L_ELBOW else SKEL_R_ELBOW
        val W = if (useLeft) SKEL_L_WRIST else SKEL_R_WRIST
        val H = if (useLeft) SKEL_L_HIP else SKEL_R_HIP
        val K = if (useLeft) SKEL_L_KNEE else SKEL_R_KNEE
        val A = if (useLeft) SKEL_L_ANKLE else SKEL_R_ANKLE

        val kneeAngle  = threePointAngle(landmarks[H], landmarks[K], landmarks[A])
        val elbowAngle = threePointAngle(landmarks[S], landmarks[E], landmarks[W])
        val hipAngle   = threePointAngle(landmarks[S], landmarks[H], landmarks[K])
        val trunkAngle = trunkLeanAngle(
            landmarks[SKEL_L_SHOULDER], landmarks[SKEL_R_SHOULDER],
            landmarks[SKEL_L_HIP], landmarks[SKEL_R_HIP],
        )

        // Overstride: compare leading (forward) ankle to hip midpoint
        val hipMidX    = (landmarks[SKEL_L_HIP].x + landmarks[SKEL_R_HIP].x) / 2f
        val hipMidY    = (landmarks[SKEL_L_HIP].y + landmarks[SKEL_R_HIP].y) / 2f
        val shoulderMidY = (landmarks[SKEL_L_SHOULDER].y + landmarks[SKEL_R_SHOULDER].y) / 2f
        val facingRight = landmarks[SKEL_NOSE].x > hipMidX

        val leftAnkleX  = landmarks[SKEL_L_ANKLE].x
        val rightAnkleX = landmarks[SKEL_R_ANKLE].x
        val leadingAnkleX = if (facingRight) maxOf(leftAnkleX, rightAnkleX) else minOf(leftAnkleX, rightAnkleX)
        val leadingAnkleY = when {
            facingRight -> if (leftAnkleX >= rightAnkleX) landmarks[SKEL_L_ANKLE].y else landmarks[SKEL_R_ANKLE].y
            else        -> if (leftAnkleX <= rightAnkleX) landmarks[SKEL_L_ANKLE].y else landmarks[SKEL_R_ANKLE].y
        }

        val bodyHeight = abs(leadingAnkleY - shoulderMidY).coerceAtLeast(0.01f)
        val overstrideRatio = (abs(leadingAnkleX - hipMidX) / bodyHeight).coerceAtLeast(0f)
        val isLanding = leadingAnkleY > 0.65f && leadingAnkleY > hipMidY

        val nearAnkleY = landmarks[A].y

        // Shank angle: angle between shin (knee→ankle) and vertical [0,1]
        // running-form-analyzer: horizontal_vector=[0,1], shin_vector=ankle-knee
        val shankAngle = run {
            val kx = landmarks[K].x; val ky = landmarks[K].y
            val ax = landmarks[A].x; val ay = landmarks[A].y
            val shinX = ax - kx; val shinY = ay - ky
            val mag = sqrt(shinX * shinX + shinY * shinY)
            if (mag < 1e-6f) 0f
            else Math.toDegrees(acos((shinY / mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
        }

        // Arm swing angle: angle between torso vector (shoulder→hip) and upper-arm (shoulder→elbow)
        val armSwingAngle = run {
            val sx = landmarks[S].x; val sy = landmarks[S].y
            val hx = landmarks[H].x; val hy = landmarks[H].y
            val ex = landmarks[E].x; val ey = landmarks[E].y
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

    private fun avg(landmarks: List<SkeletonPoint>, vararg indices: Int): Float =
        indices.map { landmarks[it].v }.average().toFloat()

    private fun threePointAngle(a: SkeletonPoint, b: SkeletonPoint, c: SkeletonPoint): Float {
        val ax = a.x - b.x; val ay = a.y - b.y
        val cx = c.x - b.x; val cy = c.y - b.y
        val dot = ax * cx + ay * cy
        val magA = sqrt(ax * ax + ay * ay)
        val magC = sqrt(cx * cx + cy * cy)
        if (magA < 1e-6f || magC < 1e-6f) return 180f
        val cosAngle = (dot / (magA * magC)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle).toDouble()).toFloat()
    }

    private fun trunkLeanAngle(
        ls: SkeletonPoint, rs: SkeletonPoint,
        lh: SkeletonPoint, rh: SkeletonPoint,
    ): Float {
        val shoulderMidX = (ls.x + rs.x) / 2f
        val shoulderMidY = (ls.y + rs.y) / 2f
        val hipMidX = (lh.x + rh.x) / 2f
        val hipMidY = (lh.y + rh.y) / 2f
        val dx = shoulderMidX - hipMidX
        val dy = hipMidY - shoulderMidY
        return Math.toDegrees(atan2(dx.toDouble(), dy.toDouble())).toFloat()
    }
}
