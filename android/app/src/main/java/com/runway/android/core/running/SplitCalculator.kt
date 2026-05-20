package com.runway.android.core.running

import android.location.Location
import com.runway.android.data.running.model.RunPointResponse
import java.time.Instant

object SplitCalculator {

    private const val SPLIT_DISTANCE_METERS = 1000.0
    private const val MIN_PARTIAL_METERS = 10.0  // ignore sub-10m trailing segment

    /**
     * Calculates 1km splits from an ordered list of GPS points.
     *
     * Returns an empty list when:
     *   - fewer than 2 points provided
     *   - total distance < 10m
     *   - timestamps cannot be parsed
     *
     * The last entry may be a partial split (distanceKm < 1.0).
     */
    fun calculate(points: List<RunPointResponse>): List<RunSplit> {
        if (points.size < 2) return emptyList()

        // Sort defensively; backend should return ordered by sequence
        val sorted = points.sortedWith(compareBy({ it.sequence }, { it.recordedAt }))

        val first = sorted.first()
        var splitStartSecs = parseEpochSecs(first.recordedAt) ?: return emptyList()

        val splits = mutableListOf<RunSplit>()
        var splitNumber = 1
        var splitDistanceMeters = 0.0

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]

            val currSecs = parseEpochSecs(curr.recordedAt) ?: continue
            if (currSecs < splitStartSecs) continue  // skip out-of-order timestamps

            val segment = distanceBetweenMeters(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude,
            )
            splitDistanceMeters += segment

            if (splitDistanceMeters >= SPLIT_DISTANCE_METERS) {
                val durationSecs = (currSecs - splitStartSecs).toInt().coerceAtLeast(1)
                splits.add(
                    RunSplit(
                        splitNumber = splitNumber++,
                        distanceKm = splitDistanceMeters / 1000.0,
                        durationSeconds = durationSecs,
                        paceSecondsPerKm = paceSecsPerKm(durationSecs, splitDistanceMeters),
                    )
                )
                splitStartSecs = currSecs
                splitDistanceMeters = 0.0
            }
        }

        // Partial last split
        if (splitDistanceMeters >= MIN_PARTIAL_METERS) {
            val endSecs = parseEpochSecs(sorted.last().recordedAt)
            if (endSecs != null && endSecs > splitStartSecs) {
                val durationSecs = (endSecs - splitStartSecs).toInt().coerceAtLeast(1)
                splits.add(
                    RunSplit(
                        splitNumber = splitNumber,
                        distanceKm = splitDistanceMeters / 1000.0,
                        durationSeconds = durationSecs,
                        paceSecondsPerKm = paceSecsPerKm(durationSecs, splitDistanceMeters),
                    )
                )
            }
        }

        return splits
    }

    private fun paceSecsPerKm(durationSecs: Int, distanceMeters: Double): Int {
        if (distanceMeters <= 0) return 0
        return (durationSecs / (distanceMeters / 1000.0)).toInt()
    }

    private fun distanceBetweenMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
    ): Double {
        if (lat1 == lat2 && lon1 == lon2) return 0.0
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0].toDouble()
    }

    private fun parseEpochSecs(isoString: String?): Long? =
        runCatching { Instant.parse(isoString).epochSecond }.getOrNull()
}
