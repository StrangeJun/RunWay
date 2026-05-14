package com.runway.android.data.auth

import com.runway.android.core.datastore.TokenDataStore
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.result.safeApiCall
import com.runway.android.data.auth.model.LoginRequest
import com.runway.android.data.auth.model.LoginResponse
import com.runway.android.data.auth.model.LogoutRequest
import com.runway.android.data.auth.model.ReissueRequest
import com.runway.android.data.auth.model.ReissueResponse
import com.runway.android.data.auth.model.SignupRequest
import com.runway.android.data.auth.model.SignupResponse
import com.runway.android.data.auth.remote.AuthApi
import com.runway.android.domain.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDataStore: TokenDataStore,
) : AuthRepository {

    override suspend fun signup(
        email: String,
        password: String,
        nickname: String,
    ): NetworkResult<SignupResponse> {
        return safeApiCall { authApi.signup(SignupRequest(email, password, nickname)) }
    }

    override suspend fun login(email: String, password: String): NetworkResult<LoginResponse> {
        val result = safeApiCall { authApi.login(LoginRequest(email, password)) }
        if (result is NetworkResult.Success) {
            tokenDataStore.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return result
    }

    override suspend fun logout(): NetworkResult<Unit> {
        val refreshToken = tokenDataStore.getRefreshTokenBlocking()
            ?: return NetworkResult.ApiError(401, "UNAUTHORIZED", "로그인 상태가 아닙니다.")
        return try {
            authApi.logout(LogoutRequest(refreshToken))
            tokenDataStore.clearTokens()
            NetworkResult.Success(Unit)
        } catch (e: HttpException) {
            // 서버 오류가 발생해도 로컬 토큰은 항상 삭제 (보안)
            tokenDataStore.clearTokens()
            NetworkResult.ApiError(e.code(), null, e.message() ?: "로그아웃 실패")
        } catch (e: Exception) {
            tokenDataStore.clearTokens()
            NetworkResult.NetworkError(e)
        }
    }

    override suspend fun reissue(): NetworkResult<ReissueResponse> {
        val refreshToken = tokenDataStore.getRefreshTokenBlocking()
            ?: return NetworkResult.ApiError(401, "UNAUTHORIZED", "로그인 상태가 아닙니다.")
        val result = safeApiCall { authApi.reissue(ReissueRequest(refreshToken)) }
        if (result is NetworkResult.Success) {
            tokenDataStore.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return result
    }

    override fun isLoggedInFlow(): Flow<Boolean> {
        return tokenDataStore.accessTokenFlow.map { it != null }
    }
}
