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
    private val kneeAlpha: Float = 0.3f,
    private val kneeMaxDelta: Float = 0.08f,
    private val kneeLowVisThreshold: Float = 0.65f,
    private val ankleAlpha: Float = 0.55f,
    private val ankleMaxDelta: Float = 0.18f,
    private val ankleLowVisThreshold: Float = 0.62f,
) {
    init {
        require(alpha in 0f..1f)       { "alpha must be in [0, 1], got $alpha" }
        require(lowVisAlpha in 0f..1f) { "lowVisAlpha must be in [0, 1], got $lowVisAlpha" }
        require(kneeAlpha in 0f..1f)   { "kneeAlpha must be in [0, 1], got $kneeAlpha" }
        require(ankleAlpha in 0f..1f)  { "ankleAlpha must be in [0, 1], got $ankleAlpha" }
        require(maxDelta >= 0f)        { "maxDelta must be non-negative, got $maxDelta" }
        require(kneeMaxDelta >= 0f)    { "kneeMaxDelta must be non-negative, got $kneeMaxDelta" }
        require(ankleMaxDelta >= 0f)   { "ankleMaxDelta must be non-negative, got $ankleMaxDelta" }
    }

    private var prev: List<SkeletonPoint>? = null

    fun smooth(raw: List<SkeletonPoint>): List<SkeletonPoint> {
        val p = prev
        val stabilizedRaw = stabilizeLegCandidates(raw, p)
        val result = if (p == null || p.size != raw.size) {
            raw
        } else {
            stabilizedRaw.zip(p).mapIndexed { index, (r, s) ->
                // Clamp sudden large jumps before EMA to avoid the filter
                // slowly tracking toward a clearly erroneous detection.
                val isKnee = index == SKEL_L_KNEE || index == SKEL_R_KNEE
                val isAnkle = index == SKEL_L_ANKLE || index == SKEL_R_ANKLE
                val allowedDelta = when {
                    isKnee -> kneeMaxDelta
                    isAnkle -> ankleMaxDelta
                    else -> maxDelta
                }
                val clampedX = s.x + (r.x - s.x).coerceIn(-allowedDelta, allowedDelta)
                val clampedY = s.y + (r.y - s.y).coerceIn(-allowedDelta, allowedDelta)

                // Low-visibility landmarks (occluded, out of frame) get a smaller
                // alpha so the previous stable position is retained more strongly.
                val effectiveAlpha = when {
                    isKnee && r.v < kneeLowVisThreshold -> 0f
                    isKnee -> kneeAlpha
                    isAnkle && r.v < ankleLowVisThreshold -> 0f
                    isAnkle -> ankleAlpha
                    r.v < lowVisThreshold -> lowVisAlpha
                    else -> alpha
                }

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

    private fun stabilizeLegCandidates(
        raw: List<SkeletonPoint>,
        previous: List<SkeletonPoint>?,
    ): List<SkeletonPoint> {
        if (previous == null || previous.size != raw.size || raw.size <= SKEL_R_ANKLE) return raw

        val mutable = raw.toMutableList()
        stabilizeKnee(
            points = mutable,
            previous = previous,
            kneeIndex = SKEL_L_KNEE,
            hipIndex = SKEL_L_HIP,
            ankleIndex = SKEL_L_ANKLE,
        )
        stabilizeKnee(
            points = mutable,
            previous = previous,
            kneeIndex = SKEL_R_KNEE,
            hipIndex = SKEL_R_HIP,
            ankleIndex = SKEL_R_ANKLE,
        )
        stabilizeAnkle(
            points = mutable,
            previous = previous,
            ankleIndex = SKEL_L_ANKLE,
            hipIndex = SKEL_L_HIP,
            kneeIndex = SKEL_L_KNEE,
        )
        stabilizeAnkle(
            points = mutable,
            previous = previous,
            ankleIndex = SKEL_R_ANKLE,
            hipIndex = SKEL_R_HIP,
            kneeIndex = SKEL_R_KNEE,
        )
        return mutable
    }

    private fun stabilizeKnee(
        points: MutableList<SkeletonPoint>,
        previous: List<SkeletonPoint>,
        kneeIndex: Int,
        hipIndex: Int,
        ankleIndex: Int,
    ) {
        val prevKnee = previous[kneeIndex]
        val candidate = points[kneeIndex]

        val hip = points[hipIndex]
        val ankle = points[ankleIndex]
        val shouldHoldPrevious =
            candidate.v < kneeLowVisThreshold ||
                distance(candidate, prevKnee) > kneeMaxDelta * 2.5f ||
                !isKneeStructurallyPlausible(candidate, hip, ankle)

        points[kneeIndex] = if (shouldHoldPrevious) prevKnee else candidate
    }

    private fun stabilizeAnkle(
        points: MutableList<SkeletonPoint>,
        previous: List<SkeletonPoint>,
        ankleIndex: Int,
        hipIndex: Int,
        kneeIndex: Int,
    ) {
        val prevAnkle = previous[ankleIndex]
        val candidate = points[ankleIndex]
        val hip = points[hipIndex]
        val knee = points[kneeIndex]

        val shouldHoldPrevious =
            candidate.v < ankleLowVisThreshold ||
                distance(candidate, prevAnkle) > ankleMaxDelta * 2.8f ||
                !isAnkleStructurallyPlausible(candidate, hip, knee)

        points[ankleIndex] = if (shouldHoldPrevious) prevAnkle else candidate
    }

    private fun isKneeStructurallyPlausible(
        knee: SkeletonPoint,
        hip: SkeletonPoint,
        ankle: SkeletonPoint,
    ): Boolean {
        if (hip.v < lowVisThreshold || ankle.v < lowVisThreshold) return true

        val minX = minOf(hip.x, ankle.x) - 0.18f
        val maxX = maxOf(hip.x, ankle.x) + 0.18f
        val minY = minOf(hip.y, ankle.y) - 0.18f
        val maxY = maxOf(hip.y, ankle.y) + 0.18f

        return knee.x in minX..maxX && knee.y in minY..maxY
    }

    private fun isAnkleStructurallyPlausible(
        ankle: SkeletonPoint,
        hip: SkeletonPoint,
        knee: SkeletonPoint,
    ): Boolean {
        if (hip.v < lowVisThreshold || knee.v < kneeLowVisThreshold) return true

        val hipToKnee = distance(hip, knee).coerceAtLeast(0.04f)
        val kneeToAnkle = distance(knee, ankle)
        val maxLowerLeg = hipToKnee * 1.9f

        val minX = minOf(hip.x, knee.x) - 0.28f
        val maxX = maxOf(hip.x, knee.x) + 0.28f
        val minY = minOf(hip.y, knee.y) - 0.28f
        val maxY = maxOf(hip.y, knee.y) + 0.38f

        return kneeToAnkle <= maxLowerLeg &&
            ankle.x in minX..maxX &&
            ankle.y in minY..maxY
    }

    /** Must be called when the user seeks to a new position so EMA state does not
     *  bleed across discontinuous video positions. */
    fun reset() {
        prev = null
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun distance(a: SkeletonPoint, b: SkeletonPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
