package com.runway.android.core.location

import android.location.Location

object DistanceCalculator {

    // Returns distance in meters between two GPS points, or 0.0 if the point should be discarded.
    fun calculate(from: RunwayLocation, to: RunwayLocation): Double {
        if (from.latitude == to.latitude && from.longitude == to.longitude) return 0.0

        val results = FloatArray(1)
        Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results,
        )
        val meters = results[0].toDouble()

        // Filter GPS teleportation: > 10 m/s implied speed is impossible for a runner.
        // Clamp time delta to at least 1 second to avoid division by zero on same-timestamp points.
        val timeDeltaSecs = (to.recordedAt.epochSecond - from.recordedAt.epochSecond)
            .coerceAtLeast(1L)
        val impliedSpeedMps = meters / timeDeltaSecs
        if (impliedSpeedMps > 10.0) return 0.0

        return meters
    }
}
