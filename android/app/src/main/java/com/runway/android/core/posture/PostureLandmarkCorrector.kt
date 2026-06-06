package com.runway.android.core.posture

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "LandmarkCorrector"

/**
 * MediaPipe 측면 촬영 무릎·발목 전방 바이어스 보정 + 해부학적 유효성 검사.
 *
 * 파이프라인:
 *   1. One Euro Filter 평활화
 *   2. 해부학적 유효성 검사 (골반 기준 좌표 범위 위반 시 이전 유효값으로 대체)
 *   3. 측면 프로파일 전방 오프셋 보정 (무릎·발목 X 좌표 후방 이동)
 *
 * 유효성 검사가 없으면, 먼 쪽(far-side) 다리가 러닝머신 기구나 배경 구조물에
 * 감지되어도 그대로 표시된다.
 */
class PostureLandmarkCorrector {
    private val smoother = PostureSkeletonSmoother(
        minCutoff = 1.2f,
        beta = 4.0f,
        lowVisFreezeThreshold = 0.5f,
    )
    private var facingDirection = 0f

    // 해부학적으로 유효했던 마지막 무릎·발목 위치 (fallback용)
    private var prevLKnee:  SkeletonPoint? = null
    private var prevRKnee:  SkeletonPoint? = null
    private var prevLAnkle: SkeletonPoint? = null
    private var prevRAnkle: SkeletonPoint? = null

    fun correct(raw: List<SkeletonPoint>, timestampMs: Long): List<SkeletonPoint> {
        if (raw.size <= SKEL_R_ANKLE) return raw

        val stable = smoother.smooth(raw, timestampMs)

        // ── 방향 추정 ─────────────────────────────────────────────────────────
        val hipMidX      = (stable[SKEL_L_HIP].x + stable[SKEL_R_HIP].x) / 2f
        val hipMidY      = (stable[SKEL_L_HIP].y + stable[SKEL_R_HIP].y) / 2f
        val shoulderMidY = (stable[SKEL_L_SHOULDER].y + stable[SKEL_R_SHOULDER].y) / 2f
        val torsoHeight  = abs(hipMidY - shoulderMidY).coerceAtLeast(MIN_BODY_SCALE)
        val noseOffset   = stable[SKEL_NOSE].x - hipMidX
        val sideProfileRatio = abs(noseOffset) / torsoHeight

        if (sideProfileRatio >= FACING_UPDATE_RATIO && stable[SKEL_NOSE].v >= MIN_DIRECTION_VISIBILITY) {
            facingDirection = if (noseOffset >= 0f) 1f else -1f
        }

        // ── 해부학적 유효성 검사 ───────────────────────────────────────────────
        // 골반을 anchor로 사용한다. 무릎·발목이 골반에서 비정상적으로 멀면
        // 러닝머신 기구 등 배경 오감지로 판단하고 이전 유효값으로 대체한다.
        val lHip = stable[SKEL_L_HIP]
        val rHip = stable[SKEL_R_HIP]

        val lKneeRaw  = stable[SKEL_L_KNEE]
        val rKneeRaw  = stable[SKEL_R_KNEE]
        val lAnkleRaw = stable[SKEL_L_ANKLE]
        val rAnkleRaw = stable[SKEL_R_ANKLE]

        val lKnee  = anatomicallyValid(lKneeRaw,  lHip, isKnee = true,  prev = prevLKnee,  label = "L_KNEE")
        val rKnee  = anatomicallyValid(rKneeRaw,  rHip, isKnee = true,  prev = prevRKnee,  label = "R_KNEE")
        val lAnkle = anatomicallyValid(lAnkleRaw, lHip, isKnee = false, prev = prevLAnkle, label = "L_ANKLE")
        val rAnkle = anatomicallyValid(rAnkleRaw, rHip, isKnee = false, prev = prevRAnkle, label = "R_ANKLE")

        // 이번 프레임의 유효값을 다음 프레임 fallback으로 저장
        prevLKnee  = lKnee
        prevRKnee  = rKnee
        prevLAnkle = lAnkle
        prevRAnkle = rAnkle

        val validated = stable.toMutableList().apply {
            this[SKEL_L_KNEE]  = lKnee
            this[SKEL_R_KNEE]  = rKnee
            this[SKEL_L_ANKLE] = lAnkle
            this[SKEL_R_ANKLE] = rAnkle
        }

        // ── 측면 프로파일 전방 오프셋 보정 ────────────────────────────────────
        if (facingDirection == 0f || sideProfileRatio <= SIDE_PROFILE_START_RATIO) {
            return validated
        }

        val correctionStrength = (
            (sideProfileRatio - SIDE_PROFILE_START_RATIO) /
                (SIDE_PROFILE_FULL_RATIO - SIDE_PROFILE_START_RATIO)
        ).coerceIn(0f, 1f)

        val lFar = facingDirection > 0f
        val lKStr  = if (lFar) correctionStrength * FAR_SIDE_SCALE else correctionStrength
        val rKStr  = if (!lFar) correctionStrength * FAR_SIDE_SCALE else correctionStrength
        val lAStr  = if (lFar) correctionStrength * FAR_SIDE_SCALE else correctionStrength
        val rAStr  = if (!lFar) correctionStrength * FAR_SIDE_SCALE else correctionStrength

        return validated.toMutableList().apply {
            this[SKEL_L_KNEE]  = offsetJoint(validated[SKEL_L_KNEE],  lHip, validated[SKEL_L_ANKLE], lKStr,  KNEE_OFFSET_RATIO,   MIN_KNEE_OFFSET,   MAX_KNEE_OFFSET)
            this[SKEL_R_KNEE]  = offsetJoint(validated[SKEL_R_KNEE],  rHip, validated[SKEL_R_ANKLE], rKStr,  KNEE_OFFSET_RATIO,   MIN_KNEE_OFFSET,   MAX_KNEE_OFFSET)
            this[SKEL_L_ANKLE] = offsetJoint(validated[SKEL_L_ANKLE], lHip, validated[SKEL_L_ANKLE], lAStr,  ANKLE_OFFSET_RATIO,  MIN_ANKLE_OFFSET,  MAX_ANKLE_OFFSET)
            this[SKEL_R_ANKLE] = offsetJoint(validated[SKEL_R_ANKLE], rHip, validated[SKEL_R_ANKLE], rAStr,  ANKLE_OFFSET_RATIO,  MIN_ANKLE_OFFSET,  MAX_ANKLE_OFFSET)
        }
    }

    fun reset() {
        smoother.reset()
        facingDirection = 0f
        prevLKnee  = null; prevRKnee  = null
        prevLAnkle = null; prevRAnkle = null
    }

    /**
     * [joint] 가 [hip] 으로부터 해부학적으로 가능한 범위 내에 있으면 그대로 반환하고,
     * 벗어나면 [prev] (이전 유효값) 을 반환한다.
     *
     * 무릎: 골반 X 기준 ±MAX_KNEE_X, 골반 Y 보다 아래여야 함.
     * 발목: 골반 X 기준 ±MAX_ANKLE_X.
     */
    private fun anatomicallyValid(
        joint: SkeletonPoint,
        hip: SkeletonPoint,
        isKnee: Boolean,
        prev: SkeletonPoint?,
        label: String,
    ): SkeletonPoint {
        val xDist = abs(joint.x - hip.x)
        val maxX  = if (isKnee) MAX_KNEE_X_FROM_HIP else MAX_ANKLE_X_FROM_HIP

        // 무릎 Y 검사:
        //   - 골반보다 MIN_KNEE_BELOW_HIP 이상 아래에 있어야 유효 (너무 높은 감지 제거)
        //   - 달리기 고강도 무릎 드라이브에서도 무릎은 항상 골반보다 아래에 위치한다
        val yOk = if (isKnee) joint.y >= hip.y + MIN_KNEE_BELOW_HIP else true

        val valid = xDist <= maxX && yOk

        if (!valid) {
            val fallback = prev ?: run {
                // 첫 프레임에 오감지된 경우: 골반 아래 추정 위치로 초기화
                val estimatedY = if (isKnee) hip.y + KNEE_INIT_BELOW_HIP
                                 else        hip.y + ANKLE_INIT_BELOW_HIP
                hip.copy(y = estimatedY.coerceIn(0f, 1f), v = 0.3f)
            }
            Log.d(TAG, "[$label] rejected: xDist=${"%.3f".format(xDist)} maxX=$maxX " +
                "joint.y=${joint.y} hip.y=${hip.y} yOk=$yOk → " +
                if (prev != null) "prev(${prev.x},${prev.y})" else "estimated(${fallback.x},${fallback.y})")
            return fallback
        }
        return joint
    }

    /** 측면 프로파일 기준 관절 X 좌표를 달리기 방향 반대로 이동 (전방 바이어스 제거). */
    private fun offsetJoint(
        joint: SkeletonPoint,
        hip: SkeletonPoint,
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

        // 해부학적 유효성 임계값 (normalized 0-1 좌표 기준)
        // 무릎: 골반 X ± 20% 이내, 골반 Y 보다 최소 3% 아래이어야 유효
        const val MAX_KNEE_X_FROM_HIP   = 0.20f
        const val MIN_KNEE_BELOW_HIP    = 0.03f  // knee.y >= hip.y + 0.03 이어야 유효
        const val KNEE_INIT_BELOW_HIP   = 0.15f  // 첫 프레임 오감지 시 초기 추정값
        // 발목: 달리기 보폭만큼 더 큰 X 범위 허용
        const val MAX_ANKLE_X_FROM_HIP  = 0.30f
        const val ANKLE_INIT_BELOW_HIP  = 0.30f  // 첫 프레임 오감지 시 초기 추정값

        // 전방 바이어스 오프셋 보정값
        const val KNEE_OFFSET_RATIO   = 0.06f
        const val MIN_KNEE_OFFSET     = 0.006f
        const val MAX_KNEE_OFFSET     = 0.030f
        const val ANKLE_OFFSET_RATIO  = 0.04f
        const val MIN_ANKLE_OFFSET    = 0.004f
        const val MAX_ANKLE_OFFSET    = 0.022f
        const val FAR_SIDE_SCALE      = 1.5f
    }
}
