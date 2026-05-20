package com.runway.android.core.running

data class RunSplit(
    val splitNumber: Int,         // 1-based sequence
    val distanceKm: Double,       // 1.0 for full km splits; < 1.0 for the partial last split
    val durationSeconds: Int,
    val paceSecondsPerKm: Int,
)
