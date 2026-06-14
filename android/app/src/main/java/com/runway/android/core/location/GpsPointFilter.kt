package com.runway.android.core.location

import java.time.Duration
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class FilteredGpsPoint(
    val location: RunwayLocation,
    val distanceMeters: Double,
    val shouldRecord: Boolean,
)

/**
 * Filters poor fixes, stationary drift, stale samples, and physically impossible jumps.
 * The accepted anchor is never replaced by a rejected point, so one spike cannot poison
 * the following comparison.
 */
class GpsPointFilter {
    private var anchor: RunwayLocation? = null
    private var smoothedSpeedMps: Float? = null

    fun reset() {
        anchor = null
        smoothedSpeedMps = null
    }

    fun filter(candidate: RunwayLocation): FilteredGpsPoint? {
        if (!candidate.hasValidCoordinates()) return null
        if ((candidate.horizontalAccuracyMeters ?: 0f) > MAX_HORIZONTAL_ACCURACY_METERS) {
            return null
        }

        val previous = anchor
        if (previous == null) {
            val normalized = candidate.copy(speedMps = usableProviderSpeed(candidate) ?: 0f)
            anchor = normalized
            smoothedSpeedMps = normalized.speedMps
            return FilteredGpsPoint(normalized, distanceMeters = 0.0, shouldRecord = true)
        }

        val elapsedSeconds = elapsedSeconds(previous, candidate)
        if (elapsedSeconds <= 0.0) return null

        val distanceMeters = distanceMeters(previous, candidate)
        val impliedSpeedMps = distanceMeters / elapsedSeconds
        if (distanceMeters > MAX_JUMP_METERS || impliedSpeedMps > MAX_RUNNING_SPEED_MPS) {
            return null
        }

        val measuredSpeed = usableProviderSpeed(candidate)
            ?.takeIf { it <= MAX_RUNNING_SPEED_MPS }
            ?: impliedSpeedMps.toFloat()
        val filteredSpeed = lowPassSpeed(measuredSpeed)
        val noiseFloorMeters = noiseFloor(previous, candidate)

        if (distanceMeters < noiseFloorMeters) {
            val stationary = candidate.copy(
                latitude = previous.latitude,
                longitude = previous.longitude,
                speedMps = filteredSpeed.coerceAtMost(STATIONARY_SPEED_CAP_MPS),
            )
            anchor = stationary
            return FilteredGpsPoint(stationary, distanceMeters = 0.0, shouldRecord = false)
        }

        val accepted = candidate.copy(speedMps = filteredSpeed)
        anchor = accepted
        return FilteredGpsPoint(accepted, distanceMeters, shouldRecord = true)
    }

    private fun usableProviderSpeed(location: RunwayLocation): Float? {
        val speed = location.speedMps ?: return null
        if (!speed.isFinite() || speed < 0f) return null
        val accuracy = location.speedAccuracyMps
        return speed.takeIf { accuracy == null || accuracy <= MAX_SPEED_ACCURACY_MPS }
    }

    private fun lowPassSpeed(measuredSpeedMps: Float): Float {
        val previousSpeed = smoothedSpeedMps
        val filtered = if (previousSpeed == null) {
            measuredSpeedMps
        } else {
            SPEED_FILTER_ALPHA * measuredSpeedMps +
                (1f - SPEED_FILTER_ALPHA) * previousSpeed
        }
        smoothedSpeedMps = filtered
        return filtered
    }

    private fun noiseFloor(previous: RunwayLocation, candidate: RunwayLocation): Double {
        val uncertainty = max(
            previous.horizontalAccuracyMeters ?: MIN_NOISE_FLOOR_METERS.toFloat(),
            candidate.horizontalAccuracyMeters ?: MIN_NOISE_FLOOR_METERS.toFloat(),
        )
        return max(
            MIN_NOISE_FLOOR_METERS,
            min(MAX_NOISE_FLOOR_METERS, uncertainty * ACCURACY_NOISE_RATIO),
        )
    }

    private fun elapsedSeconds(from: RunwayLocation, to: RunwayLocation): Double {
        val fromElapsed = from.elapsedRealtimeNanos
        val toElapsed = to.elapsedRealtimeNanos
        if (fromElapsed != null && toElapsed != null) {
            return (toElapsed - fromElapsed) / NANOS_PER_SECOND
        }
        return Duration.between(from.recordedAt, to.recordedAt).toNanos() / NANOS_PER_SECOND
    }

    private fun RunwayLocation.hasValidCoordinates(): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun distanceMeters(from: RunwayLocation, to: RunwayLocation): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(deltaLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).let { it * it }
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private companion object {
        const val MAX_HORIZONTAL_ACCURACY_METERS = 50f
        const val MAX_SPEED_ACCURACY_MPS = 2.5f
        const val MAX_RUNNING_SPEED_MPS = 12.0
        const val MAX_JUMP_METERS = 300.0
        const val MIN_NOISE_FLOOR_METERS = 1.5
        const val MAX_NOISE_FLOOR_METERS = 5.0
        const val ACCURACY_NOISE_RATIO = 0.15
        const val STATIONARY_SPEED_CAP_MPS = 0.3f
        const val SPEED_FILTER_ALPHA = 0.4f
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
