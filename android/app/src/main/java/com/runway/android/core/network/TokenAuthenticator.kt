package com.runway.android.core.network

import com.runway.android.core.datastore.TokenDataStore
import com.runway.android.data.auth.model.ReissueRequest
import com.runway.android.data.auth.remote.AuthApi
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

/**
 * 401 Unauthorized 응답 수신 시 토큰을 자동으로 갱신하는 OkHttp Authenticator.
 *
 * 순환 의존성 방지 설계:
 *   - TokenAuthenticator는 @Named("noAuth") Retrofit (AuthInterceptor 없는 클라이언트)을 주입받는다.
 *   - 이 Retrofit으로 AuthApi를 lazy하게 생성하여 reissue API만 호출한다.
 *   - Main OkHttpClient(AuthInterceptor + TokenAuthenticator 포함)와 분리되므로 순환 없음.
 *
 * 재시도 루프 방지:
 *   - response.request에 Authorization header가 없으면 재시도하지 않는다.
 *     (signup/login처럼 인증 불필요 요청이 401을 받은 경우)
 */
class TokenAuthenticator @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    @Named("noAuth") private val noAuthRetrofit: Retrofit,
) : Authenticator {

    private val authApi: AuthApi by lazy { noAuthRetrofit.create(AuthApi::class.java) }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Authorization header가 없는 요청의 401은 재시도하지 않는다 (무한 루프 방지)
        if (response.request.header("Authorization") == null) return null

        // 같은 요청이 이미 한 번 실패했으면 포기 (refresh token도 만료)
        if (response.responseCount() >= 2) {
            runBlocking { tokenDataStore.clearTokens() }
            return null
        }

        val refreshToken = tokenDataStore.getRefreshTokenBlocking() ?: run {
            runBlocking { tokenDataStore.clearTokens() }
            return null
        }

        val newTokens = runBlocking {
            runCatching {
                authApi.reissue(ReissueRequest(refreshToken)).data
            }.getOrNull()
        }

        if (newTokens == null) {
            runBlocking { tokenDataStore.clearTokens() }
            return null
        }

        runBlocking {
            tokenDataStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }

    // 같은 URL에 대한 연속 재시도 횟수 계산 (OkHttp 내부 패턴)
    private fun Response.responseCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
