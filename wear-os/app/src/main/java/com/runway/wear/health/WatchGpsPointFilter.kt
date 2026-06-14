package com.runway.wear.health

import java.time.Duration
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class FilteredWatchGpsPoint(
    val sample: HealthLocationSample,
    val shouldRecord: Boolean,
)

class WatchGpsPointFilter {
    private var anchor: HealthLocationSample? = null
    private var smoothedSpeedMps: Double? = null

    fun reset() {
        anchor = null
        smoothedSpeedMps = null
    }

    fun filter(candidate: HealthLocationSample): FilteredWatchGpsPoint? {
        if (!candidate.hasValidCoordinates()) return null

        val previous = anchor
        if (previous == null) {
            val accepted = candidate.copy(speedMps = candidate.speedMps.coerceIn(0.0, MAX_RUNNING_SPEED_MPS))
            anchor = accepted
            smoothedSpeedMps = accepted.speedMps
            return FilteredWatchGpsPoint(accepted, shouldRecord = true)
        }

        val elapsedSeconds = runCatching {
            Duration.between(
                Instant.parse(previous.recordedAt),
                Instant.parse(candidate.recordedAt),
            ).toNanos() / NANOS_PER_SECOND
        }.getOrNull() ?: return null
        if (elapsedSeconds <= 0.0) return null

        val distanceMeters = distanceMeters(previous, candidate)
        val impliedSpeedMps = distanceMeters / elapsedSeconds
        if (distanceMeters > MAX_JUMP_METERS || impliedSpeedMps > MAX_RUNNING_SPEED_MPS) {
            return null
        }

        val measuredSpeed = candidate.speedMps
            .takeIf { it.isFinite() && it in 0.0..MAX_RUNNING_SPEED_MPS }
            ?: impliedSpeedMps
        val filteredSpeed = lowPassSpeed(measuredSpeed)

        if (distanceMeters < MIN_MOVEMENT_METERS) {
            val stationary = candidate.copy(
                latitude = previous.latitude,
                longitude = previous.longitude,
                speedMps = filteredSpeed.coerceAtMost(STATIONARY_SPEED_CAP_MPS),
            )
            anchor = stationary
            return FilteredWatchGpsPoint(stationary, shouldRecord = false)
        }

        val accepted = candidate.copy(speedMps = filteredSpeed)
        anchor = accepted
        return FilteredWatchGpsPoint(accepted, shouldRecord = true)
    }

    private fun lowPassSpeed(measuredSpeedMps: Double): Double {
        val previousSpeed = smoothedSpeedMps
        val filtered = if (previousSpeed == null) {
            measuredSpeedMps
        } else {
            SPEED_FILTER_ALPHA * measuredSpeedMps +
                (1.0 - SPEED_FILTER_ALPHA) * previousSpeed
        }
        smoothedSpeedMps = filtered
        return filtered
    }

    private fun HealthLocationSample.hasValidCoordinates(): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun distanceMeters(
        from: HealthLocationSample,
        to: HealthLocationSample,
    ): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(deltaLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).let { it * it }
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private companion object {
        const val MAX_RUNNING_SPEED_MPS = 12.0
        const val MAX_JUMP_METERS = 300.0
        const val MIN_MOVEMENT_METERS = 1.5
        const val STATIONARY_SPEED_CAP_MPS = 0.3
        const val SPEED_FILTER_ALPHA = 0.4
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
