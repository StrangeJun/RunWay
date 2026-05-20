package com.runway.android.core.running

import android.location.Location
import com.runway.android.data.running.model.RunPointResponse
import java.time.Instant

object RunChartCalculator {

    private const val MIN_POINTS = 5
    private const val MAX_PACE = 1800     // 30 min/km — filter out stopped/paused noise
    private const val SMOOTH_WINDOW = 5

    fun calculate(points: List<RunPointResponse>): List<RunChartPoint> {
        if (points.size < MIN_POINTS) return emptyList()

        val sorted = points.sortedWith(compareBy({ it.sequence }, { it.recordedAt }))
        val startSecs = parseEpochSecs(sorted.first().recordedAt) ?: return emptyList()

        val raw = mutableListOf<RunChartPoint>()

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]

            val currSecs = parseEpochSecs(curr.recordedAt) ?: continue
            val elapsed = (currSecs - startSecs).toInt()
            if (elapsed <= 0) continue

            val pace: Int = if (curr.speedMps != null && curr.speedMps > 0.1f) {
                (1000f / curr.speedMps).toInt()
            } else {
                val dist = distanceBetween(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
                val dt = (currSecs - (parseEpochSecs(prev.recordedAt) ?: currSecs)).toInt()
                if (dist < 1.0 || dt <= 0) continue
                (dt / (dist / 1000.0)).toInt()
            }

            if (pace in 1..MAX_PACE) {
                raw.add(RunChartPoint(elapsedSeconds = elapsed, paceSecondsPerKm = pace))
            }
        }

        if (raw.isEmpty()) return emptyList()
        return smooth(raw)
    }

    private fun smooth(points: List<RunChartPoint>): List<RunChartPoint> {
        val half = SMOOTH_WINDOW / 2
        return points.mapIndexed { i, p ->
            val window = points.subList(
                maxOf(0, i - half),
                minOf(points.size, i + half + 1),
            )
            p.copy(paceSecondsPerKm = window.map { it.paceSecondsPerKm }.average().toInt())
        }
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == lat2 && lon1 == lon2) return 0.0
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0].toDouble()
    }

    private fun parseEpochSecs(isoString: String?): Long? =
        runCatching { Instant.parse(isoString).epochSecond }.getOrNull()
}
