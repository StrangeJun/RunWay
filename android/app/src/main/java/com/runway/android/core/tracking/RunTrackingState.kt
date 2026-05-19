package com.runway.android.core.tracking

import com.runway.android.core.location.RunwayLocation

data class RunTrackingState(
    val mode: RunTrackingMode = RunTrackingMode.FREE_RUN,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val hasFirstFix: Boolean = false,
    val elapsedSeconds: Int = 0,
    val distanceMeters: Double = 0.0,
    val currentSpeedMps: Float? = null,
    val lastLocation: RunwayLocation? = null,
)
