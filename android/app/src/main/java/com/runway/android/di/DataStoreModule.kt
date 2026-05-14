package com.runway.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 파일 레벨 delegate — Context 당 DataStore 인스턴스가 하나임을 보장
private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "runway_auth",
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideTokenDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.tokenDataStore
}
