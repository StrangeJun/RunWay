package com.runway.android.core.posture.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PostureAnalysisEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PostureDatabase : RoomDatabase() {
    abstract fun postureAnalysisDao(): PostureAnalysisDao
}
