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
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "runway_onboarding")
private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "runway_reminder")
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "runway_settings")

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

    @Provides
    @Singleton
    @Named("onboardingDataStore")
    fun provideOnboardingDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.onboardingDataStore

    @Provides
    @Singleton
    @Named("reminderDataStore")
    fun provideReminderDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.reminderDataStore

    @Provides
    @Singleton
    @Named("settingsDataStore")
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore
}
