package com.runway.android.core.posture

import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * running-form-analyzer(henryczup) 방식의 관절 각도 계산기.
 *
 * 각도는 반드시 물리 픽셀 좌표(x * videoWidth, y * videoHeight)로 계산한다.
 * normalized(0-1) 좌표를 그대로 사용하면 portrait 영상에서 y 성분이
 * 과소평가되어 90° 팔꿈치가 ~62°로 왜곡된다.
 *
 * confidence threshold: 0.30 (running-form-analyzer 동일)
 * 개별 랜드마크 visibility 가 0.30 미만이면 해당 각도만 건너뛴다.
 */
object PostureAngleCalculator {

    private const val CONF = 0.30f   // running-form-analyzer confidence_threshold

    /**
     * [aspectRatio] = videoWidth / videoHeight.
     * normalized x 에 이 값을 곱해 물리 비율로 보정한다.
     */
    fun compute(landmarks: List<SkeletonPoint>, aspectRatio: Float = 1f): PostureFrameAngles? {
        if (landmarks.size <= SKEL_R_ANKLE) return null

        val ar = aspectRatio.coerceAtLeast(0.1f)

        // ── visibility 기반 side 선택 ──────────────────────────────────────────
        val leftVis  = avg(landmarks, SKEL_L_SHOULDER, SKEL_L_ELBOW, SKEL_L_WRIST, SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE)
        val rightVis = avg(landmarks, SKEL_R_SHOULDER, SKEL_R_ELBOW, SKEL_R_WRIST, SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE)

        // 양쪽 모두 threshold 미달이면 이 프레임은 사용하지 않는다
        if (maxOf(leftVis, rightVis) < CONF) return null

        val useLeft = leftVis >= rightVis
        val S = if (useLeft) SKEL_L_SHOULDER else SKEL_R_SHOULDER
        val E = if (useLeft) SKEL_L_ELBOW   else SKEL_R_ELBOW
        val W = if (useLeft) SKEL_L_WRIST   else SKEL_R_WRIST
        val H = if (useLeft) SKEL_L_HIP     else SKEL_R_HIP
        val K = if (useLeft) SKEL_L_KNEE    else SKEL_R_KNEE
        val A = if (useLeft) SKEL_L_ANKLE   else SKEL_R_ANKLE

        // ── 물리 좌표 변환 헬퍼 ────────────────────────────────────────────────
        // running-form-analyzer: x = kp[1] * width, y = kp[0] * height
        // 여기서는 pt.x * ar, pt.y (ar = W/H 로 정규화, y = 1 기준)
        fun px(pt: SkeletonPoint) = floatArrayOf(pt.x * ar, pt.y)
        fun ok(idx: Int) = landmarks[idx].v >= CONF

        // ── 무릎 각도 ─────────────────────────────────────────────────────────
        // running-form-analyzer: thigh = knee - hip, lower_leg = knee - ankle
        val kneeFlexAngle = if (ok(H) && ok(K) && ok(A)) {
            val hip   = px(landmarks[H]); val knee = px(landmarks[K]); val ankle = px(landmarks[A])
            val thigh = floatArrayOf(knee[0]-hip[0],   knee[1]-hip[1])
            val shin  = floatArrayOf(knee[0]-ankle[0], knee[1]-ankle[1])
            angleBetween(thigh, shin)
        } else null

        // ── 팔꿈치 각도 ───────────────────────────────────────────────────────
        // running-form-analyzer: upper_arm = elbow - shoulder, forearm = elbow - wrist
        val elbowAngle = if (ok(S) && ok(E) && ok(W)) {
            val shoulder = px(landmarks[S]); val elbow = px(landmarks[E]); val wrist = px(landmarks[W])
            val upperArm = floatArrayOf(elbow[0]-shoulder[0], elbow[1]-shoulder[1])
            val forearm  = floatArrayOf(elbow[0]-wrist[0],    elbow[1]-wrist[1])
            angleBetween(upperArm, forearm)
        } else null

        // ── 고관절 각도 ───────────────────────────────────────────────────────
        // running-form-analyzer: torso = hip - shoulder, thigh = knee - hip
        val hipExtensionAngle = if (ok(S) && ok(H) && ok(K)) {
            val shoulder = px(landmarks[S]); val hip = px(landmarks[H]); val knee = px(landmarks[K])
            val torso = floatArrayOf(hip[0]-shoulder[0], hip[1]-shoulder[1])
            val thigh = floatArrayOf(knee[0]-hip[0],     knee[1]-hip[1])
            angleBetween(torso, thigh)
        } else null

        // ── 몸통 기울기 ───────────────────────────────────────────────────────
        // running-form-analyzer: trunk_line = shoulder - hip vs vertical [0,-1]
        val trunkLeanAngle = if (ok(S) && ok(H)) {
            val shoulder = px(landmarks[S]); val hip = px(landmarks[H])
            val dx = (shoulder[0] - hip[0])   // 수평
            val dy = (hip[1]      - shoulder[1])  // 수직 (아래로 양수)
            Math.toDegrees(atan2(dx.toDouble(), dy.toDouble())).toFloat()
        } else null

        // ── 정강이 각도 ───────────────────────────────────────────────────────
        // running-form-analyzer: shin = ankle - knee vs vertical [0,1]
        val shankAngle = if (ok(K) && ok(A)) {
            val knee = px(landmarks[K]); val ankle = px(landmarks[A])
            val shinX = ankle[0] - knee[0]; val shinY = ankle[1] - knee[1]
            val mag = sqrt(shinX*shinX + shinY*shinY)
            if (mag < 1e-6f) null
            else Math.toDegrees(acos((shinY / mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
        } else null

        // ── 팔 스윙 각도 ──────────────────────────────────────────────────────
        // running-form-analyzer: torso = shoulder - hip, upper_arm = shoulder - elbow
        val armSwingAngle = if (ok(S) && ok(E) && ok(H)) {
            val shoulder = px(landmarks[S]); val elbow = px(landmarks[E]); val hip = px(landmarks[H])
            val torso    = floatArrayOf(shoulder[0]-hip[0],    shoulder[1]-hip[1])
            val upperArm = floatArrayOf(shoulder[0]-elbow[0],  shoulder[1]-elbow[1])
            angleBetween(torso, upperArm)
        } else null

        // ── 오버스트라이드 ────────────────────────────────────────────────────
        val hipMidX      = (landmarks[SKEL_L_HIP].x + landmarks[SKEL_R_HIP].x) / 2f
        val hipMidY      = (landmarks[SKEL_L_HIP].y + landmarks[SKEL_R_HIP].y) / 2f
        val shoulderMidY = (landmarks[SKEL_L_SHOULDER].y + landmarks[SKEL_R_SHOULDER].y) / 2f
        val facingRight  = landmarks[SKEL_NOSE].x > hipMidX
        val lAX = landmarks[SKEL_L_ANKLE].x; val rAX = landmarks[SKEL_R_ANKLE].x
        val leadX = if (facingRight) maxOf(lAX, rAX) else minOf(lAX, rAX)
        val leadY = when {
            facingRight -> if (lAX >= rAX) landmarks[SKEL_L_ANKLE].y else landmarks[SKEL_R_ANKLE].y
            else        -> if (lAX <= rAX) landmarks[SKEL_L_ANKLE].y else landmarks[SKEL_R_ANKLE].y
        }
        val bodyH = (leadY - shoulderMidY).coerceAtLeast(0.01f)
        val overstrideRatio = (kotlin.math.abs(leadX - hipMidX) * ar / bodyH).coerceAtLeast(0f)
        val isLanding = leadY > 0.65f && leadY > hipMidY

        return PostureFrameAngles(
            kneeFlexAngle     = kneeFlexAngle ?: 0f,
            trunkLeanAngle    = trunkLeanAngle ?: 0f,
            elbowAngle        = elbowAngle ?: 0f,
            hipExtensionAngle = hipExtensionAngle ?: 0f,
            overstrideRatio   = overstrideRatio,
            isLandingFrame    = isLanding,
            visibility        = maxOf(leftVis, rightVis),
            hipMidY           = hipMidY,
            nearAnkleY        = landmarks[A].y,
            shankAngle        = shankAngle ?: 0f,
            armSwingAngle     = armSwingAngle ?: 0f,
        )
    }

    /** 두 벡터 사이의 각도 (도). */
    private fun angleBetween(v1: FloatArray, v2: FloatArray): Float {
        val dot  = v1[0]*v2[0] + v1[1]*v2[1]
        val mag1 = sqrt(v1[0]*v1[0] + v1[1]*v1[1])
        val mag2 = sqrt(v2[0]*v2[0] + v2[1]*v2[1])
        if (mag1 < 1e-6f || mag2 < 1e-6f) return 180f
        return Math.toDegrees(acos((dot / (mag1 * mag2)).coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    private fun avg(landmarks: List<SkeletonPoint>, vararg indices: Int): Float =
        indices.map { landmarks[it].v }.average().toFloat()
}
