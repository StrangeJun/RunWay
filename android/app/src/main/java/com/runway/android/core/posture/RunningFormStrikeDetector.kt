package com.runway.android.core.posture

/**
 * Foot strike detector — exact port of running-form-analyzer
 * (henryczup/running-form-analyzer, AngleMetrics.kt line:
 *   StepMetrics(filter_type='temporal', detection_axis='y')).
 *
 * Uses Y-axis local MINIMUM detection:
 *   local min of Y (in image coords, Y=0 is top) = ankle at its highest
 *   screen position = peak of swing phase. Filters with a simple moving
 *   average (temporal filter, window=10) — matching the original's
 *   filter_type='temporal'. Adaptive threshold clamped to [0.005, 0.1].
 */
class RunningFormStrikeDetector(
    private val windowSize: Int = 10,
    private val adaptiveWindowSize: Int = 50,
    private val minIntervalMs: Long = 400L,
) {
    // Temporal filter (moving average) — matches filter_type='temporal'
    private val filterBuffer = ArrayDeque<Float>()

    private val positions = ArrayDeque<Float>()
    private val motionHistory = ArrayDeque<Float>()
    private var threshold = 0.02f
    private var lastStrikeMs = -1L

    /** Returns true on the frame a step event is detected. */
    fun update(ankle: SkeletonPoint, timestampMs: Long): Boolean {
        if (ankle.v < 0.3f) return false

        val filtered = temporalFilter(ankle.y)

        if (positions.size >= windowSize) positions.removeFirst()
        positions.addLast(filtered)
        if (motionHistory.size >= adaptiveWindowSize) motionHistory.removeFirst()
        motionHistory.addLast(filtered)

        updateThreshold()
        if (positions.size < 3) return false

        val p0 = positions[positions.size - 3]
        val p1 = positions[positions.size - 2]  // candidate
        val p2 = positions[positions.size - 1]

        // Local minimum: ankle was descending in Y (rising in screen), bottomed, now ascending
        // Matches original: positions[-2] < positions[-1] and positions[-2] < positions[-3]
        //                   and positions[-1] - positions[-2] > threshold
        val localMin = p1 < p0 && p1 < p2 && (p2 - p1) > threshold
        if (!localMin) return false

        val tooSoon = lastStrikeMs >= 0 && (timestampMs - lastStrikeMs) < minIntervalMs
        if (tooSoon) return false

        lastStrikeMs = timestampMs
        return true
    }

    private fun updateThreshold() {
        if (motionHistory.size < adaptiveWindowSize) return
        val range = (motionHistory.maxOrNull() ?: 0f) - (motionHistory.minOrNull() ?: 0f)
        threshold = (0.7f * threshold + 0.3f * range * 0.1f).coerceIn(0.005f, 0.1f)
    }

    private fun temporalFilter(value: Float): Float {
        if (filterBuffer.size >= windowSize) filterBuffer.removeFirst()
        filterBuffer.addLast(value)
        return if (filterBuffer.size < windowSize) value
        else filterBuffer.sum() / filterBuffer.size
    }

    fun reset() {
        positions.clear()
        motionHistory.clear()
        filterBuffer.clear()
        lastStrikeMs = -1L
        threshold = 0.02f
    }
}
