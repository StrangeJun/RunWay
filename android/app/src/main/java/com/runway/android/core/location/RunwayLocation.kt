package com.runway.android.core.location

import java.time.Instant

data class RunwayLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val speedMps: Float?,
    val recordedAt: Instant,
    val horizontalAccuracyMeters: Float? = null,
    val speedAccuracyMps: Float? = null,
    val elapsedRealtimeNanos: Long? = null,
)
