package com.runway.android.core.posture

/**
 * Applies Exponential Moving Average (EMA) smoothing to skeleton landmark
 * positions frame-by-frame during video replay.
 *
 * Each landmark is smoothed independently. Low-visibility landmarks receive a
 * smaller alpha so that uncertain detections cause less jitter. A jump-clamp
 * step runs before EMA to discard physically impossible coordinate deltas
 * (e.g. a landmark teleporting 20 % of the frame in a single tick).
 *
 * Usage:
 *   val smoother = PostureSkeletonSmoother()
 *   smoother.reset()          // call on seek or new video
 *   val stable = smoother.smooth(rawFrame.pts)
 */
class PostureSkeletonSmoother(
    private val alpha: Float = 0.4f,
    private val maxDelta: Float = 0.12f,    // max normalized coordinate jump per tick
    private val lowVisAlpha: Float = 0.15f, // alpha for landmarks with visibility < threshold
    private val lowVisThreshold: Float = 0.5f,
) {
    init {
        require(alpha in 0f..1f)       { "alpha must be in [0, 1], got $alpha" }
        require(lowVisAlpha in 0f..1f) { "lowVisAlpha must be in [0, 1], got $lowVisAlpha" }
        require(maxDelta >= 0f)        { "maxDelta must be non-negative, got $maxDelta" }
    }

    private var prev: List<SkeletonPoint>? = null

    fun smooth(raw: List<SkeletonPoint>): List<SkeletonPoint> {
        val p = prev
        val result = if (p == null || p.size != raw.size) {
            raw
        } else {
            raw.zip(p).map { (r, s) ->
                // Clamp sudden large jumps before EMA to avoid the filter
                // slowly tracking toward a clearly erroneous detection.
                val clampedX = s.x + (r.x - s.x).coerceIn(-maxDelta, maxDelta)
                val clampedY = s.y + (r.y - s.y).coerceIn(-maxDelta, maxDelta)

                // Low-visibility landmarks (occluded, out of frame) get a smaller
                // alpha so the previous stable position is retained more strongly.
                val effectiveAlpha = if (r.v < lowVisThreshold) lowVisAlpha else alpha

                SkeletonPoint(
                    x = lerp(s.x, clampedX, effectiveAlpha),
                    y = lerp(s.y, clampedY, effectiveAlpha),
                    v = lerp(s.v, r.v, alpha),
                )
            }
        }
        prev = result
        return result
    }

    /** Must be called when the user seeks to a new position so EMA state does not
     *  bleed across discontinuous video positions. */
    fun reset() {
        prev = null
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
