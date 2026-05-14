package com.runway.android.domain.auth

import com.runway.android.core.result.NetworkResult
import com.runway.android.data.auth.model.LoginResponse
import com.runway.android.data.auth.model.ReissueResponse
import com.runway.android.data.auth.model.SignupResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** 회원가입. 성공 시 사용자 기본 정보 반환. */
    suspend fun signup(
        email: String,
        password: String,
        nickname: String,
    ): NetworkResult<SignupResponse>

    /**
     * 로그인. 성공 시 accessToken + refreshToken을 DataStore에 저장하고 LoginResponse를 반환.
     * ViewModel에서 저장 여부를 신경 쓰지 않아도 된다.
     */
    suspend fun login(email: String, password: String): NetworkResult<LoginResponse>

    /**
     * 로그아웃. DataStore의 토큰을 삭제한다.
     * 서버 응답이 실패해도 로컬 토큰은 항상 삭제한다.
     */
    suspend fun logout(): NetworkResult<Unit>

    /**
     * Access Token 재발급. 성공 시 새 토큰을 DataStore에 저장.
     * TokenAuthenticator가 자동으로 호출하므로 일반적으로 직접 호출할 필요 없음.
     */
    suspend fun reissue(): NetworkResult<ReissueResponse>

    /** 현재 로그인 상태를 관찰한다. SplashScreen → 자동 로그인 분기에 사용. */
    fun isLoggedInFlow(): Flow<Boolean>
}
