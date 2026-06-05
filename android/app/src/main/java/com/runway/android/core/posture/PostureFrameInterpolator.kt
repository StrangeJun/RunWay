package com.runway.android.core.posture

/**
 * Binary search for the first frame with timestamp strictly greater than [posMs].
 * Mirrors Python's bisect_right. O(log n).
 */
internal fun bisectRight(frames: List<PostureVideoFrame>, posMs: Long): Int {
    var lo = 0; var hi = frames.size
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        if (frames[mid].t <= posMs) lo = mid + 1 else hi = mid
    }
    return lo
}

/**
 * Returns a [PostureVideoFrame] linearly interpolated to [posMs].
 * [sortedFrames] must be sorted by [PostureVideoFrame.t] ascending.
 * Returns null only when [sortedFrames] is empty.
 */
internal fun interpolateFrame(sortedFrames: List<PostureVideoFrame>, posMs: Long): PostureVideoFrame? {
    if (sortedFrames.isEmpty()) return null
    if (sortedFrames.size == 1) return sortedFrames[0]

    val idx       = bisectRight(sortedFrames, posMs)
    val beforeIdx = (idx - 1).coerceAtLeast(0)
    val before    = sortedFrames[beforeIdx]
    val after     = sortedFrames.getOrNull(idx) ?: return before

    val range = (after.t - before.t).toFloat()
    if (range <= 0f) return before

    if (before.pts.size != after.pts.size) return before

    val t   = ((posMs - before.t) / range).coerceIn(0f, 1f)
    val pts = before.pts.zip(after.pts).map { (a, b) ->
        SkeletonPoint(
            x = a.x + (b.x - a.x) * t,
            y = a.y + (b.y - a.y) * t,
            v = a.v + (b.v - a.v) * t,
        )
    }
    return PostureVideoFrame(posMs, pts)
}
