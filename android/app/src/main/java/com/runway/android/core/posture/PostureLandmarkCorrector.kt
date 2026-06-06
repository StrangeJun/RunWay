package com.runway.android.core.posture

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Stabilizes detected landmarks and compensates for MediaPipe's tendency to place
 * side-profile knee and ankle landmarks in front of the visible joint center.
 *
 * Near side (facing camera) and far side (occluded) legs are handled separately:
 * the far-side leg gets a stronger correction because its 3D→2D reprojection
 * from an occluded position introduces more forward bias.
 */
class PostureLandmarkCorrector {
    private val smoother = PostureSkeletonSmoother(
        minCutoff = 1.2f,
        beta = 4.0f,
        lowVisFreezeThreshold = 0.5f,
    )
    private var facingDirection = 0f  // +1 facing right, -1 facing left, 0 unknown

    fun correct(raw: List<SkeletonPoint>, timestampMs: Long): List<SkeletonPoint> {
        if (raw.size <= SKEL_R_ANKLE) return raw

        val stable = smoother.smooth(raw, timestampMs)
        val hipMidX      = (stable[SKEL_L_HIP].x + stable[SKEL_R_HIP].x) / 2f
        val hipMidY      = (stable[SKEL_L_HIP].y + stable[SKEL_R_HIP].y) / 2f
        val shoulderMidY = (stable[SKEL_L_SHOULDER].y + stable[SKEL_R_SHOULDER].y) / 2f
        val torsoHeight  = abs(hipMidY - shoulderMidY).coerceAtLeast(MIN_BODY_SCALE)
        val noseOffset   = stable[SKEL_NOSE].x - hipMidX
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

        // 가까운 쪽(near) 다리와 먼 쪽(far) 다리를 판별한다.
        // facingDirection == +1(오른쪽 향함) 이면 왼다리가 카메라에서 멀어진다(far),
        // facingDirection == -1(왼쪽 향함) 이면 오른다리가 far side 가 된다.
        val lFar = facingDirection > 0f   // true → L leg is far side
        val lKneeStrength  = if (lFar) correctionStrength * FAR_SIDE_SCALE else correctionStrength
        val rKneeStrength  = if (!lFar) correctionStrength * FAR_SIDE_SCALE else correctionStrength
        val lAnkleStrength = if (lFar) correctionStrength * FAR_SIDE_SCALE else correctionStrength
        val rAnkleStrength = if (!lFar) correctionStrength * FAR_SIDE_SCALE else correctionStrength

        return stable.toMutableList().apply {
            this[SKEL_L_KNEE] = correctedJoint(
                hip = stable[SKEL_L_HIP], joint = stable[SKEL_L_KNEE], ankle = stable[SKEL_L_ANKLE],
                strength = lKneeStrength, ratioBase = KNEE_OFFSET_RATIO,
                minOff = MIN_KNEE_OFFSET, maxOff = MAX_KNEE_OFFSET,
            )
            this[SKEL_R_KNEE] = correctedJoint(
                hip = stable[SKEL_R_HIP], joint = stable[SKEL_R_KNEE], ankle = stable[SKEL_R_ANKLE],
                strength = rKneeStrength, ratioBase = KNEE_OFFSET_RATIO,
                minOff = MIN_KNEE_OFFSET, maxOff = MAX_KNEE_OFFSET,
            )
            this[SKEL_L_ANKLE] = correctedJoint(
                hip = stable[SKEL_L_HIP], joint = stable[SKEL_L_ANKLE], ankle = stable[SKEL_L_ANKLE],
                strength = lAnkleStrength, ratioBase = ANKLE_OFFSET_RATIO,
                minOff = MIN_ANKLE_OFFSET, maxOff = MAX_ANKLE_OFFSET,
            )
            this[SKEL_R_ANKLE] = correctedJoint(
                hip = stable[SKEL_R_HIP], joint = stable[SKEL_R_ANKLE], ankle = stable[SKEL_R_ANKLE],
                strength = rAnkleStrength, ratioBase = ANKLE_OFFSET_RATIO,
                minOff = MIN_ANKLE_OFFSET, maxOff = MAX_ANKLE_OFFSET,
            )
        }
    }

    fun reset() {
        smoother.reset()
        facingDirection = 0f
    }

    /**
     * [hip] ~ [joint] ~ [ankle] 으로 구성된 다리 segment 에서 [joint] 를 앞쪽 바이어스 방향
     * 반대로 이동시킨다. [hip]/[ankle] 는 레그 길이 계산에만 사용된다.
     */
    private fun correctedJoint(
        hip: SkeletonPoint,
        joint: SkeletonPoint,
        ankle: SkeletonPoint,
        strength: Float,
        ratioBase: Float,
        minOff: Float,
        maxOff: Float,
    ): SkeletonPoint {
        if (minOf(hip.v, joint.v, ankle.v) < MIN_LIMB_VISIBILITY) return joint
        val legLength = distance(hip, ankle)
        val offset = (legLength * ratioBase).coerceIn(minOff, maxOff) * strength
        return joint.copy(x = (joint.x - facingDirection * offset).coerceIn(0f, 1f))
    }

    private fun distance(a: SkeletonPoint, b: SkeletonPoint): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private companion object {
        const val MIN_BODY_SCALE = 0.05f
        const val MIN_DIRECTION_VISIBILITY = 0.5f
        const val MIN_LIMB_VISIBILITY = 0.45f
        const val FACING_UPDATE_RATIO = 0.12f
        const val SIDE_PROFILE_START_RATIO = 0.08f
        const val SIDE_PROFILE_FULL_RATIO = 0.28f

        // 무릎 오프셋 — MediaPipe side-profile 무릎 전방 바이어스 보정
        const val KNEE_OFFSET_RATIO = 0.06f
        const val MIN_KNEE_OFFSET = 0.006f
        const val MAX_KNEE_OFFSET = 0.030f

        // 발목 오프셋 — 무릎보다 작지만 far-side 에서 전방 오차 발생
        const val ANKLE_OFFSET_RATIO = 0.04f
        const val MIN_ANKLE_OFFSET = 0.004f
        const val MAX_ANKLE_OFFSET = 0.022f

        // far-side (카메라에서 먼 쪽) 다리는 occlusion 으로 인해 오차가 더 크다
        const val FAR_SIDE_SCALE = 1.5f
    }
}
