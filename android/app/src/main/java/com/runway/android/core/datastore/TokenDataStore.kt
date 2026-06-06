package com.runway.android.core.datastore

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JWT 토큰을 DataStore에 저장하고 읽는 클래스.
 *
 * - saveTokens(): 로그인 / reissue 성공 시 호출
 * - clearTokens(): 로그아웃 / token 만료 시 호출
 * - accessTokenFlow: SplashScreen에서 자동 로그인 여부 확인에 사용
 * - getAccessTokenBlocking(): OkHttp AuthInterceptor (suspend 불가) 에서 사용
 */
@Singleton
class TokenDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    // Flow — SplashScreen/ViewModel에서 로그인 상태 관찰 시 사용
    val accessTokenFlow: Flow<String?> = dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val refreshTokenFlow: Flow<String?> = dataStore.data.map { it[KEY_REFRESH_TOKEN] }
    val userIdFlow: Flow<String?> = accessTokenFlow.map(::decodeUserId)

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clearTokens() {
        dataStore.edit { it.clear() }
    }

    // Blocking 버전 — OkHttp Interceptor/Authenticator (suspend 컨텍스트 없음)에서만 사용
    // OkHttp thread pool에서 호출되므로 main thread 블로킹 위험 없음
    fun getAccessTokenBlocking(): String? = runBlocking {
        accessTokenFlow.first()
    }

    fun getRefreshTokenBlocking(): String? = runBlocking {
        refreshTokenFlow.first()
    }

    suspend fun getUserId(): String? = userIdFlow.first()

    private fun decodeUserId(token: String?): String? = runCatching {
        val payload = token?.split(".")?.getOrNull(1) ?: return null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(decoded, Charsets.UTF_8)).optString("sub").takeIf { it.isNotBlank() }
    }.getOrNull()
}
