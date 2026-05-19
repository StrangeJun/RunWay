package com.runway.android.di

import com.runway.android.data.attempt.CourseAttemptRepositoryImpl
import com.runway.android.data.auth.AuthRepositoryImpl
import com.runway.android.data.course.CourseRepositoryImpl
import com.runway.android.data.running.RunningRepositoryImpl
import com.runway.android.domain.attempt.CourseAttemptRepository
import com.runway.android.domain.auth.AuthRepository
import com.runway.android.domain.course.CourseRepository
import com.runway.android.domain.running.RunningRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRunningRepository(impl: RunningRepositoryImpl): RunningRepository

    @Binds
    @Singleton
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    @Singleton
    abstract fun bindCourseAttemptRepository(impl: CourseAttemptRepositoryImpl): CourseAttemptRepository
}
