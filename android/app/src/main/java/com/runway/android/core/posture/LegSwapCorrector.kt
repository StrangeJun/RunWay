package com.runway.android.core.posture

import kotlin.math.sqrt

/**
 * Two-track cost-based leg identity corrector for MediaPipe Pose Landmarker.
 *
 * MediaPipe can assign "rightKnee" / "rightAnkle" to the anatomically-left leg
 * when legs cross in a side-view gait recording. This class treats the problem as
 * tracking-by-detection: it maintains two persistent leg tracks (A and B) and, on
 * each frame, assigns the two MediaPipe observations to tracks by cost rather than
 * by MediaPipe's own left/right labels.
 *
 * A swap is only confirmed after [N_CONFIRM] consecutive frames where the swapped
 * assignment is cheaper by more than [HYSTERESIS_MARGIN]. This prevents false
 * corrections on normal frames.
 *
 * Pipeline position: apply AFTER [PostureSkeletonSmoother], BEFORE joint-angle
 * calculation.
 *
 * Usage:
 *   val corrector = LegSwapCorrector()
 *   corrector.reset()                             // call on seek or new video
 *   val corrected = corrector.correct(smoothed, frameTimestampMs)
 */
class LegSwapCorrector {

    companion object {
        /** Total-cost margin required for a swap to be considered (normalized coords). */
        private const val HYSTERESIS_MARGIN = 0.20f

        /** Consecutive frames with swap advantage required before switching identity. */
        private const val N_CONFIRM = 2

        /** Minimum landmark visibility to include in track updates. */
        private const val VIS_MIN = 0.40f

        /** EMA weight for velocity update (higher = faster velocity adaptation). */
        private const val ALPHA_VEL = 0.30f

        /** Clamp dt so large pauses don't produce huge velocity-extrapolation jumps. */
        private const val MAX_DT = 0.20f
    }

    // Internal state per leg track ─────────────────────────────────────────────

    private data class LegState(
        var kneeX: Float = 0f, var kneeY: Float = 0f,
        var ankleX: Float = 0f, var ankleY: Float = 0f,
        var kneeVx: Float = 0f, var kneeVy: Float = 0f,
        var ankleVx: Float = 0f, var ankleVy: Float = 0f,
        var initialized: Boolean = false,
    )

    // trackA starts assigned to MediaPipe "left"; trackB to "right".
    private val trackA = LegState()
    private val trackB = LegState()

    // When true, the current best assignment has A←right / B←left.
    private var isSwapped = false

    // Frames where the swap hypothesis has been cheaper (cleared on keep-wins frame).
    private var pendingSwapCount = 0

    private var lastTimestampMs: Long = -1L

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns [pts] with lower-body landmarks (hip/knee/ankle) swapped if a
     * persistent left/right identity switch is detected.
     *
     * [pts] must be in KEY_LANDMARK_INDICES order (≥ 13 points).
     * [timestampMs] is the video-frame timestamp from [PostureVideoFrame.t].
     */
    fun correct(pts: List<SkeletonPoint>, timestampMs: Long = -1L): List<SkeletonPoint> {
        if (pts.size <= SKEL_R_ANKLE) return pts

        // Same timestamp → replay or polling duplicate; apply existing decision, skip update.
        if (timestampMs >= 0L && timestampMs == lastTimestampMs) {
            return if (isSwapped) swapLowerBody(pts) else pts
        }

        val dt = computeDt(timestampMs).coerceAtMost(MAX_DT)
        if (timestampMs >= 0L) lastTimestampMs = timestampMs

        val mpLeft  = ObsLeg(pts[SKEL_L_HIP],  pts[SKEL_L_KNEE],  pts[SKEL_L_ANKLE])
        val mpRight = ObsLeg(pts[SKEL_R_HIP], pts[SKEL_R_KNEE], pts[SKEL_R_ANKLE])

        // First frame: initialize both tracks, no correction applied.
        if (!trackA.initialized) {
            initTrack(trackA, mpLeft.knee, mpLeft.ankle)
            initTrack(trackB, mpRight.knee, mpRight.ankle)
            return pts
        }

        // Velocity-based prediction for each track's knee and ankle.
        val (pAKx, pAKy) = trackA.run { Pair(kneeX + kneeVx * dt, kneeY + kneeVy * dt) }
        val (pAAx, pAAy) = trackA.run { Pair(ankleX + ankleVx * dt, ankleY + ankleVy * dt) }
        val (pBKx, pBKy) = trackB.run { Pair(kneeX + kneeVx * dt, kneeY + kneeVy * dt) }
        val (pBAx, pBAy) = trackB.run { Pair(ankleX + ankleVx * dt, ankleY + ankleVy * dt) }

        // obsA / obsB: observations currently assigned to track A / B.
        val (obsA, obsB) = if (!isSwapped) Pair(mpLeft, mpRight) else Pair(mpRight, mpLeft)

        val keepCost =
            dist(obsA.knee.x, obsA.knee.y,   pAKx, pAKy) +
            dist(obsA.ankle.x, obsA.ankle.y,  pAAx, pAAy) +
            dist(obsB.knee.x, obsB.knee.y,   pBKx, pBKy) +
            dist(obsB.ankle.x, obsB.ankle.y,  pBAx, pBAy)

        val swapCost =
            dist(obsB.knee.x, obsB.knee.y,   pAKx, pAKy) +
            dist(obsB.ankle.x, obsB.ankle.y,  pAAx, pAAy) +
            dist(obsA.knee.x, obsA.knee.y,   pBKx, pBKy) +
            dist(obsA.ankle.x, obsA.ankle.y,  pBAx, pBAy)

        // Accumulate pending swap count; reset on any keep-wins frame.
        if (swapCost + HYSTERESIS_MARGIN < keepCost) {
            pendingSwapCount++
        } else {
            pendingSwapCount = 0
        }

        if (pendingSwapCount >= N_CONFIRM) {
            isSwapped = !isSwapped
            pendingSwapCount = 0
        }

        // Update tracks from the (possibly newly corrected) assignment.
        val (finalA, finalB) = if (!isSwapped) Pair(mpLeft, mpRight) else Pair(mpRight, mpLeft)
        updateTrack(trackA, finalA.knee, finalA.ankle, dt)
        updateTrack(trackB, finalB.knee, finalB.ankle, dt)

        return if (isSwapped) swapLowerBody(pts) else pts
    }

    /** Reset all track state — call on seek, replay start, or new video. */
    fun reset() {
        with(trackA) { initialized = false; kneeVx = 0f; kneeVy = 0f; ankleVx = 0f; ankleVy = 0f }
        with(trackB) { initialized = false; kneeVx = 0f; kneeVy = 0f; ankleVx = 0f; ankleVy = 0f }
        isSwapped = false
        pendingSwapCount = 0
        lastTimestampMs = -1L
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private data class ObsLeg(
        val hip: SkeletonPoint,
        val knee: SkeletonPoint,
        val ankle: SkeletonPoint,
    )

    private fun swapLowerBody(pts: List<SkeletonPoint>): List<SkeletonPoint> {
        val out = pts.toMutableList()
        out[SKEL_L_HIP]   = pts[SKEL_R_HIP];   out[SKEL_R_HIP]   = pts[SKEL_L_HIP]
        out[SKEL_L_KNEE]  = pts[SKEL_R_KNEE];  out[SKEL_R_KNEE]  = pts[SKEL_L_KNEE]
        out[SKEL_L_ANKLE] = pts[SKEL_R_ANKLE]; out[SKEL_R_ANKLE] = pts[SKEL_L_ANKLE]
        return out
    }

    private fun initTrack(s: LegState, knee: SkeletonPoint, ankle: SkeletonPoint) {
        s.kneeX = knee.x; s.kneeY = knee.y
        s.ankleX = ankle.x; s.ankleY = ankle.y
        s.kneeVx = 0f; s.kneeVy = 0f
        s.ankleVx = 0f; s.ankleVy = 0f
        s.initialized = true
    }

    private fun updateTrack(s: LegState, knee: SkeletonPoint, ankle: SkeletonPoint, dt: Float) {
        if (knee.v >= VIS_MIN) {
            val vx = (knee.x - s.kneeX) / dt
            val vy = (knee.y - s.kneeY) / dt
            s.kneeVx = ALPHA_VEL * vx + (1f - ALPHA_VEL) * s.kneeVx
            s.kneeVy = ALPHA_VEL * vy + (1f - ALPHA_VEL) * s.kneeVy
            s.kneeX = knee.x; s.kneeY = knee.y
        }
        if (ankle.v >= VIS_MIN) {
            val vx = (ankle.x - s.ankleX) / dt
            val vy = (ankle.y - s.ankleY) / dt
            s.ankleVx = ALPHA_VEL * vx + (1f - ALPHA_VEL) * s.ankleVx
            s.ankleVy = ALPHA_VEL * vy + (1f - ALPHA_VEL) * s.ankleVy
            s.ankleX = ankle.x; s.ankleY = ankle.y
        }
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    private fun computeDt(timestampMs: Long): Float {
        if (timestampMs < 0L || lastTimestampMs < 0L) return 1f / 10f
        return ((timestampMs - lastTimestampMs) / 1000f).coerceIn(0.001f, 0.5f)
    }
}
