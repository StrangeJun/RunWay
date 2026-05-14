package com.runway.android.data.running.model

data class RunPointRequest(
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMps: Double,
    val recordedAt: String,
)
