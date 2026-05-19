package com.runway.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// RunTrackingManager and RunTrackingNotification are provided via @Inject constructor;
// no explicit bindings are needed.
@Module
@InstallIn(SingletonComponent::class)
object TrackingModule
