package com.runway.android.core.location

import android.location.Location

object GpsPointValidator {
    private const val MAX_SPEED_MPS = 12.0        // ~43 km/h
    private const val MAX_JUMP_METERS = 200.0
    private const val MIN_DISTANCE_METERS = 1.0

    fun isValid(new: RunwayLocation, prev: RunwayLocation?): Boolean {
        if (prev == null) return true

        val results = FloatArray(1)
        Location.distanceBetween(
            prev.latitude, prev.longitude,
            new.latitude, new.longitude,
            results,
        )
        val distance = results[0].toDouble()

        if (distance < MIN_DISTANCE_METERS) return false
        if (distance > MAX_JUMP_METERS) return false

        val timeDeltaSecs = (new.recordedAt.epochSecond - prev.recordedAt.epochSecond)
            .coerceAtLeast(1L)
        val impliedSpeed = distance / timeDeltaSecs
        if (impliedSpeed > MAX_SPEED_MPS) return false

        return true
    }
}
