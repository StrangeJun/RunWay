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
import javax.inject.Named
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "runway_auth")
private val Context.trackingDataStore: DataStore<Preferences> by preferencesDataStore(name = "runway_tracking")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideTokenDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.tokenDataStore

    @Provides
    @Singleton
    @Named("trackingDataStore")
    fun provideTrackingDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.trackingDataStore
}
