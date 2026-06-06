package com.runway.android.core.posture

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Stabilizes detected landmarks and compensates for MediaPipe's tendency to place
 * side-profile knee landmarks slightly in front of the visible joint center.
 */
class PostureLandmarkCorrector {
    private val smoother = PostureSkeletonSmoother(
        minCutoff = 1.2f,
        beta = 4.0f,
        lowVisFreezeThreshold = 0.5f,
    )
    private var facingDirection = 0f

    fun correct(raw: List<SkeletonPoint>, timestampMs: Long): List<SkeletonPoint> {
        if (raw.size <= SKEL_R_ANKLE) return raw

        val stable = smoother.smooth(raw, timestampMs)
        val hipMidX = (stable[SKEL_L_HIP].x + stable[SKEL_R_HIP].x) / 2f
        val hipMidY = (stable[SKEL_L_HIP].y + stable[SKEL_R_HIP].y) / 2f
        val shoulderMidY = (stable[SKEL_L_SHOULDER].y + stable[SKEL_R_SHOULDER].y) / 2f
        val torsoHeight = abs(hipMidY - shoulderMidY).coerceAtLeast(MIN_BODY_SCALE)
        val noseOffset = stable[SKEL_NOSE].x - hipMidX
        val sideProfileRatio = abs(noseOffset) / torsoHeight

        if (sideProfileRatio >= FACING_UPDATE_RATIO && stable[SKEL_NOSE].v >= MIN_DIRECTION_VISIBILITY) {
            facingDirection = if (noseOffset >= 0f) 1f else -1f
        }

        if (facingDirection == 0f || sideProfileRatio <= SIDE_PROFILE_START_RATIO) {
            return stable
        }

        val correctionStrength = (
            (sideProfileRatio - SIDE_PROFILE_START_RATIO) /
                (SIDE_PROFILE_FULL_RATIO - SIDE_PROFILE_START_RATIO)
            ).coerceIn(0f, 1f)

        return stable.toMutableList().apply {
            this[SKEL_L_KNEE] = correctedKnee(
                hip = stable[SKEL_L_HIP],
                knee = stable[SKEL_L_KNEE],
                ankle = stable[SKEL_L_ANKLE],
                correctionStrength = correctionStrength,
            )
            this[SKEL_R_KNEE] = correctedKnee(
                hip = stable[SKEL_R_HIP],
                knee = stable[SKEL_R_KNEE],
                ankle = stable[SKEL_R_ANKLE],
                correctionStrength = correctionStrength,
            )
        }
    }

    fun reset() {
        smoother.reset()
        facingDirection = 0f
    }

    private fun correctedKnee(
        hip: SkeletonPoint,
        knee: SkeletonPoint,
        ankle: SkeletonPoint,
        correctionStrength: Float,
    ): SkeletonPoint {
        if (minOf(hip.v, knee.v, ankle.v) < MIN_LIMB_VISIBILITY) return knee

        val legLength = distance(hip, knee) + distance(knee, ankle)
        val offset = (legLength * KNEE_OFFSET_RATIO)
            .coerceIn(MIN_KNEE_OFFSET, MAX_KNEE_OFFSET) * correctionStrength

        return knee.copy(x = (knee.x - facingDirection * offset).coerceIn(0f, 1f))
    }

    private fun distance(a: SkeletonPoint, b: SkeletonPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private companion object {
        const val MIN_BODY_SCALE = 0.05f
        const val MIN_DIRECTION_VISIBILITY = 0.5f
        const val MIN_LIMB_VISIBILITY = 0.45f
        const val FACING_UPDATE_RATIO = 0.12f
        const val SIDE_PROFILE_START_RATIO = 0.08f
        const val SIDE_PROFILE_FULL_RATIO = 0.28f
        const val KNEE_OFFSET_RATIO = 0.06f
        const val MIN_KNEE_OFFSET = 0.006f
        const val MAX_KNEE_OFFSET = 0.030f
    }
}
