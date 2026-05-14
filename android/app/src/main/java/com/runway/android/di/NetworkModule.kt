package com.runway.android.di

import com.runway.android.core.datastore.TokenDataStore
import com.runway.android.core.network.AuthInterceptor
import com.runway.android.core.network.NetworkConstants
import com.runway.android.core.network.TokenAuthenticator
import com.runway.android.data.auth.remote.AuthApi
import com.runway.android.data.running.remote.RunningApi
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 네트워크 레이어 DI 모듈.
 *
 * OkHttpClient 두 종류:
 *   1. @Named("noAuth") — AuthInterceptor/Authenticator 없음. reissue, signup, login 전용.
 *   2. (기본)          — AuthInterceptor + TokenAuthenticator 포함. 인증 필요 API 전용.
 *
 * TokenAuthenticator가 noAuth Retrofit을 주입받아 reissue를 호출하므로
 * Main OkHttpClient ↔ AuthApi 간 순환 의존성이 발생하지 않는다.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun buildLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    // ─── noAuth 클라이언트 (reissue, signup, login) ───

    @Provides
    @Singleton
    @Named("noAuth")
    fun provideNoAuthOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(buildLoggingInterceptor())
            .connectTimeout(NetworkConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("noAuth")
    fun provideNoAuthRetrofit(
        @Named("noAuth") okHttpClient: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // ─── Interceptor / Authenticator ───

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenDataStore: TokenDataStore): AuthInterceptor =
        AuthInterceptor(tokenDataStore)

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenDataStore: TokenDataStore,
        @Named("noAuth") noAuthRetrofit: Retrofit,
    ): TokenAuthenticator =
        TokenAuthenticator(tokenDataStore, noAuthRetrofit)

    // ─── Main 클라이언트 (인증 필요 API) ───

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(buildLoggingInterceptor())
            .connectTimeout(NetworkConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // ─── API 인터페이스 ───

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideRunningApi(retrofit: Retrofit): RunningApi =
        retrofit.create(RunningApi::class.java)
}
