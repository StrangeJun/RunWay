package com.runway.android.core.tracking.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PendingRunPointEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class TrackingDatabase : RoomDatabase() {
    abstract fun pendingRunPointDao(): PendingRunPointDao
}
