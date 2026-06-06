package com.runway.android.core.posture

import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Android port of running-form-analyzer/utils/angle_calculator.py.
 *
 * Both body sides are calculated independently. Missing low-confidence values
 * remain null so the stateful metrics pipeline can preserve the previous value,
 * matching the original Python angle dictionary.
 */
object PostureAngleCalculator {
    private const val CONFIDENCE_THRESHOLD = 0.30f

    fun compute(landmarks: List<SkeletonPoint>, aspectRatio: Float = 1f): PostureFrameAngles? {
        if (landmarks.size <= SKEL_R_ANKLE) return null
        val ar = aspectRatio.coerceAtLeast(0.1f)

        fun point(index: Int): FloatArray? {
            val landmark = landmarks[index]
            if (landmark.v <= CONFIDENCE_THRESHOLD) return null
            return floatArrayOf(landmark.x * ar, landmark.y)
        }

        fun angle(a: Int, vertex: Int, c: Int): Float? {
            val pa = point(a) ?: return null
            val pv = point(vertex) ?: return null
            val pc = point(c) ?: return null
            return angleBetween(
                floatArrayOf(pv[0] - pa[0], pv[1] - pa[1]),
                floatArrayOf(pv[0] - pc[0], pv[1] - pc[1]),
            )
        }

        fun trunk(shoulderIndex: Int, hipIndex: Int): Float? {
            val shoulder = point(shoulderIndex) ?: return null
            val hip = point(hipIndex) ?: return null
            return Math.toDegrees(
                atan2(
                    (shoulder[0] - hip[0]).toDouble(),
                    (hip[1] - shoulder[1]).toDouble(),
                ),
            ).toFloat()
        }

        fun verticalAngle(fromIndex: Int, toIndex: Int): Float? {
            val from = point(fromIndex) ?: return null
            val to = point(toIndex) ?: return null
            return angleBetween(
                floatArrayOf(0f, 1f),
                floatArrayOf(to[0] - from[0], to[1] - from[1]),
            )
        }

        fun signedSwing(originIndex: Int, limbIndex: Int, torsoIndex: Int): Float? {
            val origin = point(originIndex) ?: return null
            val limb = point(limbIndex) ?: return null
            val torso = point(torsoIndex) ?: return null
            val limbVector = floatArrayOf(origin[0] - limb[0], origin[1] - limb[1])
            val value = angleBetween(
                floatArrayOf(origin[0] - torso[0], origin[1] - torso[1]),
                limbVector,
            )
            return if (limbVector[0] < 0f) -value else value
        }

        fun signedHipSwing(shoulderIndex: Int, hipIndex: Int, kneeIndex: Int): Float? {
            val shoulder = point(shoulderIndex) ?: return null
            val hip = point(hipIndex) ?: return null
            val knee = point(kneeIndex) ?: return null
            val thighVector = floatArrayOf(knee[0] - hip[0], knee[1] - hip[1])
            val value = angleBetween(
                floatArrayOf(hip[0] - shoulder[0], hip[1] - shoulder[1]),
                thighVector,
            )
            return if (thighVector[0] < 0f) -value else value
        }

        val leftKnee = angle(SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE)
        val rightKnee = angle(SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE)
        val leftElbow = angle(SKEL_L_SHOULDER, SKEL_L_ELBOW, SKEL_L_WRIST)
        val rightElbow = angle(SKEL_R_SHOULDER, SKEL_R_ELBOW, SKEL_R_WRIST)
        val leftHip = signedHipSwing(SKEL_L_SHOULDER, SKEL_L_HIP, SKEL_L_KNEE)
        val rightHip = signedHipSwing(SKEL_R_SHOULDER, SKEL_R_HIP, SKEL_R_KNEE)
        val leftHipAnkle = verticalAngle(SKEL_L_HIP, SKEL_L_ANKLE)
        val rightHipAnkle = verticalAngle(SKEL_R_HIP, SKEL_R_ANKLE)
        val leftShank = verticalAngle(SKEL_L_KNEE, SKEL_L_ANKLE)
        val rightShank = verticalAngle(SKEL_R_KNEE, SKEL_R_ANKLE)
        val leftArmSwing = signedSwing(SKEL_L_SHOULDER, SKEL_L_ELBOW, SKEL_L_HIP)
        val rightArmSwing = signedSwing(SKEL_R_SHOULDER, SKEL_R_ELBOW, SKEL_R_HIP)

        val leftVisibility = averageVisibility(
            landmarks,
            SKEL_L_SHOULDER, SKEL_L_ELBOW, SKEL_L_WRIST,
            SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE,
        )
        val rightVisibility = averageVisibility(
            landmarks,
            SKEL_R_SHOULDER, SKEL_R_ELBOW, SKEL_R_WRIST,
            SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE,
        )
        if (maxOf(leftVisibility, rightVisibility) <= CONFIDENCE_THRESHOLD) return null

        val useLeft = leftVisibility >= rightVisibility
        val trunkAngle = if (useLeft) {
            trunk(SKEL_L_SHOULDER, SKEL_L_HIP)
        } else {
            trunk(SKEL_R_SHOULDER, SKEL_R_HIP)
        }
        val hipMidY = (landmarks[SKEL_L_HIP].y + landmarks[SKEL_R_HIP].y) / 2f

        return PostureFrameAngles(
            kneeFlexAngle = (if (useLeft) leftKnee else rightKnee) ?: 0f,
            trunkLeanAngle = trunkAngle ?: 0f,
            elbowAngle = (if (useLeft) leftElbow else rightElbow) ?: 0f,
            hipExtensionAngle = (if (useLeft) leftHip else rightHip) ?: 0f,
            overstrideRatio = (if (useLeft) leftHipAnkle else rightHipAnkle) ?: 0f,
            isLandingFrame = false,
            visibility = maxOf(leftVisibility, rightVisibility),
            hipMidY = hipMidY,
            nearAnkleY = landmarks[if (useLeft) SKEL_L_ANKLE else SKEL_R_ANKLE].y,
            shankAngle = (if (useLeft) leftShank else rightShank) ?: 0f,
            armSwingAngle = (if (useLeft) leftArmSwing else rightArmSwing) ?: 0f,
            leftKneeAngle = leftKnee,
            rightKneeAngle = rightKnee,
            leftElbowAngle = leftElbow,
            rightElbowAngle = rightElbow,
            leftHipAngle = leftHip,
            rightHipAngle = rightHip,
            leftHipAnkleAngle = leftHipAnkle,
            rightHipAnkleAngle = rightHipAnkle,
            leftShankAngle = leftShank,
            rightShankAngle = rightShank,
            leftArmSwingAngle = leftArmSwing,
            rightArmSwingAngle = rightArmSwing,
        )
    }

    private fun angleBetween(first: FloatArray, second: FloatArray): Float {
        val dot = first[0] * second[0] + first[1] * second[1]
        val firstMagnitude = sqrt(first[0] * first[0] + first[1] * first[1])
        val secondMagnitude = sqrt(second[0] * second[0] + second[1] * second[1])
        if (firstMagnitude < 1e-6f || secondMagnitude < 1e-6f) return 0f
        return Math.toDegrees(
            acos((dot / (firstMagnitude * secondMagnitude)).coerceIn(-1f, 1f)).toDouble(),
        ).toFloat()
    }

    private fun averageVisibility(
        landmarks: List<SkeletonPoint>,
        vararg indices: Int,
    ): Float = indices.map { landmarks[it].v }.average().toFloat()
}
