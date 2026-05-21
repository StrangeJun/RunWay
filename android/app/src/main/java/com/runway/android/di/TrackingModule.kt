package com.runway.android.di

import com.runway.android.core.cadence.CadenceTracker
import com.runway.android.core.cadence.PhoneStepCadenceTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingModule {

    @Binds
    @Singleton
    abstract fun bindCadenceTracker(impl: PhoneStepCadenceTracker): CadenceTracker
}
