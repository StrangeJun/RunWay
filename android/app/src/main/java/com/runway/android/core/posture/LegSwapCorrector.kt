package com.runway.android.core.posture

import kotlin.math.sqrt

/**
 * MediaPipe Pose Landmarker의 좌우 다리 식별 오류(특히 다리 교차 시점) 보정 레이어.
 *
 * 파이프라인 위치:
 *   raw landmarks → (confidence validation) → LegSwapCorrector → temporal smoothing → angles
 *
 * 동작:
 *   1. 이전 프레임의 보정된 좌/우 무릎·발목 위치를 보관한다.
 *   2. 현재 프레임에서
 *      keepCost = 거리(현재 L → 이전 L) + 거리(현재 R → 이전 R)   // 무릎+발목 합산
 *      swapCost = 거리(현재 L → 이전 R) + 거리(현재 R → 이전 L)
 *   3. swapCost + HYSTERESIS_MARGIN < keepCost 가 N_CONFIRM 프레임 연속이면 식별을 토글한다.
 *   4. 토글된 식별로 좌/우 hip/knee/ankle 을 함께 스왑한다.
 *
 * 안전장치:
 *   - 다리 평균 visibility 가 MIN_LEG_VISIBILITY 미만이면 swap 평가를 건너뛴다(이전 결정 유지).
 *   - 타임스탬프 갭이 MAX_DT_MS 를 넘으면 (seek/재시작) 상태를 리셋한다.
 *
 * 분석 파이프라인 1회 사용 가정. 재생 단계에서는 호출하지 않는다(분석 시점에 영구 적용).
 */
class LegSwapCorrector {

    companion object {
        /** swap 이 keep 보다 이만큼 작아야 후보로 인정 (normalized 좌표 합). */
        private const val HYSTERESIS_MARGIN = 0.15f

        /** swap 후보가 연속 충족되어야 하는 프레임 수. */
        private const val N_CONFIRM = 2

        /** 다리 4개 landmark 평균 visibility 가 이 값 미만이면 swap 평가 보류. */
        private const val MIN_LEG_VISIBILITY = 0.40f

        /** 이 값을 넘는 타임스탬프 갭(ms) 발생 시 상태 리셋. */
        private const val MAX_DT_MS = 500L
    }

    // 이전 프레임의 "보정된 식별 기준" 좌/우 무릎·발목 위치.
    private var prevLeftKnee: SkeletonPoint? = null
    private var prevRightKnee: SkeletonPoint? = null
    private var prevLeftAnkle: SkeletonPoint? = null
    private var prevRightAnkle: SkeletonPoint? = null

    // 현재 누적된 식별 상태. true 이면 MediaPipe 의 L/R 을 스왑해서 출력해야 한다.
    private var isSwapped = false
    private var pendingSwapVotes = 0
    private var lastTimestampMs: Long = -1L

    /**
     * [pts] 는 KEY_LANDMARK_INDICES 순서(13 keypoints). 길이는 R_ANKLE(12) 이상이어야 한다.
     * [timestampMs] 는 비디오 타임라인 기준 (분석 단계에서는 timeMs).
     */
    fun correct(pts: List<SkeletonPoint>, timestampMs: Long): List<SkeletonPoint> {
        if (pts.size <= SKEL_R_ANKLE) return pts

        // 큰 타임스탬프 갭 → seek/재시작으로 보고 상태 리셋.
        if (lastTimestampMs >= 0L) {
            val gap = timestampMs - lastTimestampMs
            if (gap < 0L || gap > MAX_DT_MS) reset()
        }

        // 현재 isSwapped 상태가 적용된 좌/우 식별값.
        val curLK = if (!isSwapped) pts[SKEL_L_KNEE]  else pts[SKEL_R_KNEE]
        val curRK = if (!isSwapped) pts[SKEL_R_KNEE]  else pts[SKEL_L_KNEE]
        val curLA = if (!isSwapped) pts[SKEL_L_ANKLE] else pts[SKEL_R_ANKLE]
        val curRA = if (!isSwapped) pts[SKEL_R_ANKLE] else pts[SKEL_L_ANKLE]

        val pLK = prevLeftKnee;  val pRK = prevRightKnee
        val pLA = prevLeftAnkle; val pRA = prevRightAnkle

        // 첫 프레임: 이전 값 초기화 후 통과.
        if (pLK == null || pRK == null || pLA == null || pRA == null) {
            prevLeftKnee = curLK;   prevRightKnee = curRK
            prevLeftAnkle = curLA;  prevRightAnkle = curRA
            lastTimestampMs = timestampMs
            return if (isSwapped) swapLowerBody(pts) else pts
        }

        // Confidence validation: 다리 평균 visibility 가 낮으면 swap 평가 보류.
        val avgVis = (pts[SKEL_L_KNEE].v + pts[SKEL_R_KNEE].v +
                      pts[SKEL_L_ANKLE].v + pts[SKEL_R_ANKLE].v) / 4f
        if (avgVis < MIN_LEG_VISIBILITY) {
            // 이전 상태 유지: pendingSwapVotes/prev 갱신하지 않는다.
            lastTimestampMs = timestampMs
            return if (isSwapped) swapLowerBody(pts) else pts
        }

        // 거리 기반 비용 비교.
        val keepCost = dist(curLK, pLK) + dist(curLA, pLA) +
                       dist(curRK, pRK) + dist(curRA, pRA)
        val swapCost = dist(curLK, pRK) + dist(curLA, pRA) +
                       dist(curRK, pLK) + dist(curRA, pLA)

        if (swapCost + HYSTERESIS_MARGIN < keepCost) {
            pendingSwapVotes++
            if (pendingSwapVotes >= N_CONFIRM) {
                isSwapped = !isSwapped
                pendingSwapVotes = 0
            }
        } else {
            pendingSwapVotes = 0
        }

        val output = if (isSwapped) swapLowerBody(pts) else pts

        // 다음 프레임 비교를 위해 "출력된 식별" 기준으로 이전 값 갱신.
        prevLeftKnee  = output[SKEL_L_KNEE]
        prevRightKnee = output[SKEL_R_KNEE]
        prevLeftAnkle = output[SKEL_L_ANKLE]
        prevRightAnkle = output[SKEL_R_ANKLE]
        lastTimestampMs = timestampMs

        return output
    }

    fun reset() {
        prevLeftKnee = null;  prevRightKnee = null
        prevLeftAnkle = null; prevRightAnkle = null
        isSwapped = false
        pendingSwapVotes = 0
        lastTimestampMs = -1L
    }

    /** 좌/우 hip + knee + ankle 을 한 번에 스왑. 다리 geometry 가 분리되지 않도록 함께 처리. */
    private fun swapLowerBody(pts: List<SkeletonPoint>): List<SkeletonPoint> {
        val out = pts.toMutableList()
        out[SKEL_L_HIP]   = pts[SKEL_R_HIP];   out[SKEL_R_HIP]   = pts[SKEL_L_HIP]
        out[SKEL_L_KNEE]  = pts[SKEL_R_KNEE];  out[SKEL_R_KNEE]  = pts[SKEL_L_KNEE]
        out[SKEL_L_ANKLE] = pts[SKEL_R_ANKLE]; out[SKEL_R_ANKLE] = pts[SKEL_L_ANKLE]
        return out
    }

    private fun dist(a: SkeletonPoint, b: SkeletonPoint): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
