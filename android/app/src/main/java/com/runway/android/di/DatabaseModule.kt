package com.runway.android.di

import android.content.Context
import androidx.room.Room
import com.runway.android.core.tracking.local.PendingRunPointDao
import com.runway.android.core.tracking.local.TrackingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTrackingDatabase(
        @ApplicationContext context: Context,
    ): TrackingDatabase = Room.databaseBuilder(
        context,
        TrackingDatabase::class.java,
        "tracking_database",
    ).build()

    @Provides
    @Singleton
    fun providePendingRunPointDao(db: TrackingDatabase): PendingRunPointDao =
        db.pendingRunPointDao()
}
