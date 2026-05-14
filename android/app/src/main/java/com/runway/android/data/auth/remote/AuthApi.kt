package com.runway.android.data.auth.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.data.auth.model.LoginRequest
import com.runway.android.data.auth.model.LoginResponse
import com.runway.android.data.auth.model.LogoutRequest
import com.runway.android.data.auth.model.ReissueRequest
import com.runway.android.data.auth.model.ReissueResponse
import com.runway.android.data.auth.model.SignupRequest
import com.runway.android.data.auth.model.SignupResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): ApiResponse<SignupResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    /**
     * Access Token 재발급.
     * TokenAuthenticator(noAuth 클라이언트)와 AuthRepositoryImpl 양쪽에서 호출된다.
     */
    @POST("api/auth/reissue")
    suspend fun reissue(@Body request: ReissueRequest): ApiResponse<ReissueResponse>

    /**
     * 로그아웃. 백엔드가 data: null을 반환하므로 Unit?로 선언한다.
     * Gson은 null data를 Unit?으로 역직렬화한다.
     */
    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): ApiResponse<Unit?>
}
