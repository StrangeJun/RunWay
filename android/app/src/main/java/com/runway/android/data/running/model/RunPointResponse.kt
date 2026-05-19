package com.runway.android.data.running.model

data class RunPointResponse(
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val speedMps: Float?,
    val recordedAt: String,
)
