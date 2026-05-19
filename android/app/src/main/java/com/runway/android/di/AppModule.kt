package com.runway.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 앱 생명주기와 함께하는 CoroutineScope.
     * ViewModel의 onCleared() 이후에도 완료되어야 하는 cleanup 작업(ex. abandonRun)에 사용한다.
     */
    @Provides
    @Singleton
    @Named("appScope")
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
