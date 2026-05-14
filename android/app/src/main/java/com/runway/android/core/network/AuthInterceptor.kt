package com.runway.android.core.network

import com.runway.android.core.datastore.TokenDataStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 모든 인증 필요 요청에 "Authorization: Bearer <accessToken>" header를 자동으로 추가한다.
 *
 * 토큰이 없으면(미로그인 상태) header를 추가하지 않는다.
 * 401 응답 시 token 갱신은 TokenAuthenticator가 담당한다.
 */
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenDataStore.getAccessTokenBlocking()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
