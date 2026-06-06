package com.runway.android.core.posture

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

object PostureAngleCalculator {

    /**
     * [aspectRatio] = videoWidth / videoHeight (display 기준).
     *
     * MediaPipe normalized 좌표는 x ∈ [0,1] = 이미지 폭, y ∈ [0,1] = 이미지 높이로
     * 각각 독립 정규화되어 있다. Portrait 영상(1080×1920)에서는 y 1단위가 x 1단위보다
     * 물리적으로 1.78배 길다. 이를 보정하지 않으면 dot product 기반 각도 계산에서
     * 수직 성분이 과소평가되어 90° 팔꿈치가 ~62°로 표시되는 왜곡이 발생한다.
     *
     * 보정: x 성분에 aspectRatio(= W/H)를 곱해 물리 비율로 환산한 뒤 계산한다.
     */
    fun compute(landmarks: List<SkeletonPoint>, aspectRatio: Float = 1f): PostureFrameAngles? {
        if (landmarks.size <= SKEL_R_ANKLE) return null

        val ar = aspectRatio.coerceAtLeast(0.1f)

        val leftVis  = avg(landmarks, SKEL_L_SHOULDER, SKEL_L_ELBOW, SKEL_L_WRIST, SKEL_L_HIP, SKEL_L_KNEE, SKEL_L_ANKLE)
        val rightVis = avg(landmarks, SKEL_R_SHOULDER, SKEL_R_ELBOW, SKEL_R_WRIST, SKEL_R_HIP, SKEL_R_KNEE, SKEL_R_ANKLE)
        val bestVis  = maxOf(leftVis, rightVis)
        if (bestVis < 0.55f) return null

        val useLeft = leftVis >= rightVis
        val S = if (useLeft) SKEL_L_SHOULDER else SKEL_R_SHOULDER
        val E = if (useLeft) SKEL_L_ELBOW   else SKEL_R_ELBOW
        val W = if (useLeft) SKEL_L_WRIST   else SKEL_R_WRIST
        val H = if (useLeft) SKEL_L_HIP     else SKEL_R_HIP
        val K = if (useLeft) SKEL_L_KNEE    else SKEL_R_KNEE
        val A = if (useLeft) SKEL_L_ANKLE   else SKEL_R_ANKLE

        val kneeAngle  = threePointAngle(landmarks[H], landmarks[K], landmarks[A], ar)
        val elbowAngle = threePointAngle(landmarks[S], landmarks[E], landmarks[W], ar)
        val hipAngle   = threePointAngle(landmarks[S], landmarks[H], landmarks[K], ar)
        val trunkAngle = trunkLeanAngle(
            landmarks[SKEL_L_SHOULDER], landmarks[SKEL_R_SHOULDER],
            landmarks[SKEL_L_HIP],      landmarks[SKEL_R_HIP],
            ar,
        )

        val hipMidX      = (landmarks[SKEL_L_HIP].x      + landmarks[SKEL_R_HIP].x)      / 2f
        val hipMidY      = (landmarks[SKEL_L_HIP].y      + landmarks[SKEL_R_HIP].y)      / 2f
        val shoulderMidY = (landmarks[SKEL_L_SHOULDER].y + landmarks[SKEL_R_SHOULDER].y) / 2f
        val facingRight  = landmarks[SKEL_NOSE].x > hipMidX

        val leftAnkleX    = landmarks[SKEL_L_ANKLE].x
        val rightAnkleX   = landmarks[SKEL_R_ANKLE].x
        val leadingAnkleX = if (facingRight) maxOf(leftAnkleX, rightAnkleX) else minOf(leftAnkleX, rightAnkleX)
        val leadingAnkleY = when {
            facingRight -> if (leftAnkleX >= rightAnkleX) landmarks[SKEL_L_ANKLE].y else landmarks[SKEL_R_ANKLE].y
            else        -> if (leftAnkleX <= rightAnkleX) landmarks[SKEL_L_ANKLE].y else landmarks[SKEL_R_ANKLE].y
        }

        // 오버스트라이드: 수평 거리(X)와 수직 거리(Y)를 물리 단위로 통일해 비율 계산
        val bodyHeightPhys = abs(leadingAnkleY - shoulderMidY).coerceAtLeast(0.01f)
        val ankleOffsetPhys = abs(leadingAnkleX - hipMidX) * ar
        val overstrideRatio = (ankleOffsetPhys / bodyHeightPhys).coerceAtLeast(0f)
        val isLanding = leadingAnkleY > 0.65f && leadingAnkleY > hipMidY

        // 정강이 각도: 수직 방향 기준 shin vector 각도 (물리 비율 보정)
        val shankAngle = run {
            val shinX = (landmarks[A].x - landmarks[K].x) * ar
            val shinY =  landmarks[A].y - landmarks[K].y
            val mag   = sqrt(shinX * shinX + shinY * shinY)
            if (mag < 1e-6f) 0f
            else Math.toDegrees(acos((shinY / mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
        }

        // 팔 스윙 각도: torso 벡터와 upper-arm 벡터 사이 각도
        val armSwingAngle = run {
            val torsoX = (landmarks[H].x - landmarks[S].x) * ar; val torsoY = landmarks[H].y - landmarks[S].y
            val armX   = (landmarks[E].x - landmarks[S].x) * ar; val armY   = landmarks[E].y - landmarks[S].y
            val dot    = torsoX * armX + torsoY * armY
            val mag    = sqrt((torsoX*torsoX + torsoY*torsoY) * (armX*armX + armY*armY))
            if (mag < 1e-6f) 0f
            else Math.toDegrees(acos((dot / mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
        }

        return PostureFrameAngles(
            kneeFlexAngle     = kneeAngle,
            trunkLeanAngle    = trunkAngle,
            elbowAngle        = elbowAngle,
            hipExtensionAngle = hipAngle,
            overstrideRatio   = overstrideRatio,
            isLandingFrame    = isLanding,
            visibility        = bestVis,
            hipMidY           = hipMidY,
            nearAnkleY        = landmarks[A].y,
            shankAngle        = shankAngle,
            armSwingAngle     = armSwingAngle,
        )
    }

    private fun avg(landmarks: List<SkeletonPoint>, vararg indices: Int): Float =
        indices.map { landmarks[it].v }.average().toFloat()

    /** 세 점으로 이루어진 관절 각도. [ar] 로 x 를 물리 단위로 보정한다. */
    private fun threePointAngle(a: SkeletonPoint, b: SkeletonPoint, c: SkeletonPoint, ar: Float): Float {
        val ax = (a.x - b.x) * ar; val ay = a.y - b.y
        val cx = (c.x - b.x) * ar; val cy = c.y - b.y
        val dot  = ax * cx + ay * cy
        val magA = sqrt(ax * ax + ay * ay)
        val magC = sqrt(cx * cx + cy * cy)
        if (magA < 1e-6f || magC < 1e-6f) return 180f
        return Math.toDegrees(acos((dot / (magA * magC)).coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    private fun trunkLeanAngle(
        ls: SkeletonPoint, rs: SkeletonPoint,
        lh: SkeletonPoint, rh: SkeletonPoint,
        ar: Float,
    ): Float {
        val dx = ((ls.x + rs.x) / 2f - (lh.x + rh.x) / 2f) * ar
        val dy =  (lh.y + rh.y) / 2f - (ls.y + rs.y) / 2f
        return Math.toDegrees(atan2(dx.toDouble(), dy.toDouble())).toFloat()
    }
}
