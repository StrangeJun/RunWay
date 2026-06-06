package com.runway.android.core.posture.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PostureAnalysisEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class PostureDatabase : RoomDatabase() {
    abstract fun postureAnalysisDao(): PostureAnalysisDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN cadenceScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN cadenceSpm REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN cadenceFeedback TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN cadenceTip TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN verticalOscScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN verticalOscPercent REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN verticalOscFeedback TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN verticalOscTip TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE posture_analyses ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
