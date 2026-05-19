package com.runway.android.core.tracking.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_run_points")
data class PendingRunPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runningRecordId: String,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMps: Double,
    val recordedAt: String,
)
