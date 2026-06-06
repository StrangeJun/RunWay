package com.runway.android.core.posture

import kotlin.math.PI
import kotlin.math.abs

/**
 * One Euro Filter smoothing for skeleton landmark positions during video replay.
 *
 * Adapts its cutoff frequency based on movement speed:
 * - Slow / stationary landmarks → low cutoff → strong smoothing, no jitter
 * - Fast-moving landmarks (swing phase) → high cutoff → responsive, no lag
 *
 * Low-visibility landmarks are frozen at their last known position so occluded
 * joints don't drift toward bad detections.
 *
 * Reference: Casiez et al. "1€ Filter: A Simple Speed-based Low-pass Filter
 * for Noisy Input in Interactive Systems." CHI 2012.
 *
 * Usage:
 *   val smoother = PostureSkeletonSmoother()
 *   smoother.reset()                        // call on seek or new video
 *   val stable = smoother.smooth(raw, timestampMs)
 */
class PostureSkeletonSmoother(
    private val minCutoff: Float = 1.0f,   // Hz — smoothing strength at rest
    private val beta: Float = 3.0f,         // speed coefficient — higher = more responsive to fast motion
    private val dCutoff: Float = 1.0f,      // Hz — cutoff for the derivative low-pass filter
    private val lowVisFreezeThreshold: Float = 0.45f,
) {

    private inner class OneDEuroFilter {
        private var initialized = false
        private var xPrev = 0f
        private var dxFiltered = 0f
        var lastOutput = 0f
            private set

        fun filter(x: Float, dt: Float): Float {
            if (!initialized) {
                initialized = true
                xPrev = x
                lastOutput = x
                return x
            }
            // Derivative, smoothed to prevent noisy cutoff jumps
            val dx = (x - xPrev) / dt
            val aDeriv = alpha(dCutoff, dt)
            dxFiltered = aDeriv * dx + (1f - aDeriv) * dxFiltered

            // Adaptive cutoff: fast movement raises the cutoff → more responsive
            val cutoff = minCutoff + beta * abs(dxFiltered)
            val a = alpha(cutoff, dt)
            lastOutput = a * x + (1f - a) * lastOutput
            xPrev = x
            return lastOutput
        }

        fun reset() {
            initialized = false
            xPrev = 0f
            dxFiltered = 0f
            lastOutput = 0f
        }

        val isInitialized get() = initialized

        private fun alpha(cutoff: Float, dt: Float): Float {
            val r = 2f * PI.toFloat() * cutoff * dt
            return r / (r + 1f)
        }
    }

    private var filtersX: Array<OneDEuroFilter>? = null
    private var filtersY: Array<OneDEuroFilter>? = null
    private var visEma: FloatArray? = null
    private var lastTimestampMs: Long = -1L

    /**
     * @param raw         Raw landmark list from MediaPipe (or interpolated frame).
     * @param timestampMs Video timestamp in ms. Pass [PostureVideoFrame.t] for accurate dt.
     *                    Omit (or pass -1) to fall back to a fixed 30 fps assumption.
     */
    fun smooth(raw: List<SkeletonPoint>, timestampMs: Long = -1L): List<SkeletonPoint> {
        val n = raw.size
        ensureFilters(n)

        val dt = computeDt(timestampMs)
        if (timestampMs >= 0L) lastTimestampMs = timestampMs

        val fx = filtersX!!
        val fy = filtersY!!
        val ve = visEma!!

        return raw.mapIndexed { i, pt ->
            val visibleEnough = pt.v >= lowVisFreezeThreshold || !fx[i].isInitialized
            val x = if (visibleEnough) fx[i].filter(pt.x, dt) else fx[i].lastOutput
            val y = if (visibleEnough) fy[i].filter(pt.y, dt) else fy[i].lastOutput
            // Visibility: simple EMA (no freeze needed — we want it to track confidence changes)
            ve[i] = ve[i] + 0.4f * (pt.v - ve[i])
            SkeletonPoint(x = x, y = y, v = ve[i])
        }
    }

    fun reset() {
        filtersX?.forEach { it.reset() }
        filtersY?.forEach { it.reset() }
        visEma?.fill(0f)
        lastTimestampMs = -1L
    }

    private fun ensureFilters(n: Int) {
        if (filtersX?.size == n) return
        filtersX = Array(n) { OneDEuroFilter() }
        filtersY = Array(n) { OneDEuroFilter() }
        visEma = FloatArray(n)
    }

    private fun computeDt(timestampMs: Long): Float {
        if (timestampMs < 0L || lastTimestampMs < 0L) return 1f / 30f
        return ((timestampMs - lastTimestampMs) / 1000f).coerceIn(0.001f, 0.5f)
    }
}
