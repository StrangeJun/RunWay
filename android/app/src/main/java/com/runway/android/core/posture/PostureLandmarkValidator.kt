package com.runway.android.core.posture

import kotlin.math.sqrt

/**
 * Rejects transient lower-body detections that attach a leg to background
 * objects. Invalid joints are hidden instead of drawing a fabricated limb.
 */
class PostureLandmarkValidator {
    private var previous: List<SkeletonPoint>? = null

    fun validate(points: List<SkeletonPoint>): List<SkeletonPoint> {
        if (points.size <= SKEL_R_ANKLE) return points

        val output = points.toMutableList()
        val torsoScale = distance(
            midpoint(points[SKEL_L_SHOULDER], points[SKEL_R_SHOULDER]),
            midpoint(points[SKEL_L_HIP], points[SKEL_R_HIP]),
        ).coerceAtLeast(MIN_TORSO_SCALE)

        validateMotion(output, previous, torsoScale)
        validateLegLength(output, torsoScale)
        validateLegLengthBalance(output)
        previous = output
        return output
    }

    fun reset() {
        previous = null
    }

    private fun validateMotion(
        points: MutableList<SkeletonPoint>,
        old: List<SkeletonPoint>?,
        torsoScale: Float,
    ) {
        if (old == null || old.size != points.size) return

        rejectJump(points, old, SKEL_L_KNEE, torsoScale * MAX_KNEE_TRAVEL)
        rejectJump(points, old, SKEL_R_KNEE, torsoScale * MAX_KNEE_TRAVEL)
        rejectJump(points, old, SKEL_L_ANKLE, torsoScale * MAX_ANKLE_TRAVEL)
        rejectJump(points, old, SKEL_R_ANKLE, torsoScale * MAX_ANKLE_TRAVEL)
    }

    private fun rejectJump(
        points: MutableList<SkeletonPoint>,
        old: List<SkeletonPoint>,
        index: Int,
        maxTravel: Float,
    ) {
        val current = points[index]
        val previousPoint = old[index]
        if (previousPoint.v >= MIN_VISIBILITY && distance(current, previousPoint) > maxTravel) {
            points[index] = current.copy(v = 0f)
        }
    }

    private fun validateLegLengthBalance(points: MutableList<SkeletonPoint>) {
        val leftLength = legLength(points, SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE)
        val rightLength = legLength(points, SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE)
        if (leftLength <= 0f || rightLength <= 0f) return

        val shorterRatio = minOf(leftLength, rightLength) / maxOf(leftLength, rightLength)
        if (shorterRatio >= MIN_BILATERAL_LEG_RATIO) return

        if (leftLength < rightLength) {
            hideLeg(points, SKEL_L_KNEE, SKEL_L_ANKLE)
        } else {
            hideLeg(points, SKEL_R_KNEE, SKEL_R_ANKLE)
        }
    }

    private fun validateLegLength(points: MutableList<SkeletonPoint>, torsoScale: Float) {
        val minimumLegLength = torsoScale * MIN_LEG_TO_TORSO_RATIO
        val leftLength = legLength(points, SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE)
        val rightLength = legLength(points, SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE)

        if (leftLength in 0f..<minimumLegLength) {
            hideLeg(points, SKEL_L_KNEE, SKEL_L_ANKLE)
        }
        if (rightLength in 0f..<minimumLegLength) {
            hideLeg(points, SKEL_R_KNEE, SKEL_R_ANKLE)
        }
    }

    private fun legLength(
        points: List<SkeletonPoint>,
        hipIndex: Int,
        kneeIndex: Int,
        ankleIndex: Int,
    ): Float {
        val hip = points[hipIndex]
        val knee = points[kneeIndex]
        val ankle = points[ankleIndex]
        if (minOf(hip.v, knee.v, ankle.v) < MIN_VISIBILITY) return 0f
        return distance(hip, knee) + distance(knee, ankle)
    }

    private fun hideLeg(points: MutableList<SkeletonPoint>, kneeIndex: Int, ankleIndex: Int) {
        points[kneeIndex] = points[kneeIndex].copy(v = 0f)
        points[ankleIndex] = points[ankleIndex].copy(v = 0f)
    }

    private fun midpoint(first: SkeletonPoint, second: SkeletonPoint) = SkeletonPoint(
        x = (first.x + second.x) / 2f,
        y = (first.y + second.y) / 2f,
        v = minOf(first.v, second.v),
    )

    private fun distance(first: SkeletonPoint, second: SkeletonPoint): Float {
        val dx = first.x - second.x
        val dy = first.y - second.y
        return sqrt(dx * dx + dy * dy)
    }

    private companion object {
        const val MIN_VISIBILITY = 0.30f
        const val MIN_TORSO_SCALE = 0.08f
        const val MAX_KNEE_TRAVEL = 0.45f
        const val MAX_ANKLE_TRAVEL = 0.70f
        const val MIN_LEG_TO_TORSO_RATIO = 1.25f
        const val MIN_BILATERAL_LEG_RATIO = 0.78f
    }
}
