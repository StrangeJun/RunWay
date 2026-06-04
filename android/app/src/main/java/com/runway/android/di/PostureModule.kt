package com.runway.android.di

import android.content.Context
import androidx.room.Room
import com.runway.android.core.posture.PostureEvaluator
import com.runway.android.core.posture.PosturePoseAnalyzer
import com.runway.android.core.posture.PostureRuleEngine
import com.runway.android.core.posture.local.PostureAnalysisDao
import com.runway.android.core.posture.local.PostureDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PostureModule {

    @Provides
    @Singleton
    fun providePostureDatabase(@ApplicationContext context: Context): PostureDatabase =
        Room.databaseBuilder(context, PostureDatabase::class.java, "posture_database")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun providePostureAnalysisDao(db: PostureDatabase): PostureAnalysisDao =
        db.postureAnalysisDao()

    @Provides
    @Singleton
    fun providePosturePoseAnalyzer(@ApplicationContext context: Context): PosturePoseAnalyzer =
        PosturePoseAnalyzer(context)

    @Provides
    @Singleton
    fun providePostureEvaluator(engine: PostureRuleEngine): PostureEvaluator = engine
}
