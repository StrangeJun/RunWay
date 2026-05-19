package com.runway.android.core.tracking

data class TrackingSessionSnapshot(
    val runningRecordId: String,   // running_records.id — used for GPS point upload
    val courseAttemptId: String?,  // null = free run, non-null = course attempt
    val elapsedSeconds: Int,
    val distanceMeters: Double,
)
