package com.runway.android.data.auth.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.data.auth.model.GoogleLoginRequest
import com.runway.android.data.auth.model.KakaoLoginRequest
import com.runway.android.data.auth.model.LoginRequest
import com.runway.android.data.auth.model.LoginResponse
import com.runway.android.data.auth.model.LogoutRequest
import com.runway.android.data.auth.model.ReissueRequest
import com.runway.android.data.auth.model.ReissueResponse
import com.runway.android.data.auth.model.SignupRequest
import com.runway.android.data.auth.model.SignupResponse
import com.runway.android.data.auth.model.ActionResponse
import com.runway.android.data.auth.model.EmailCodeRequest
import com.runway.android.data.auth.model.PasswordResetRequest
import com.runway.android.data.auth.model.VerificationTokenResponse
import com.runway.android.data.auth.model.VerifyEmailCodeRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): ApiResponse<SignupResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("api/auth/email-verification/signup/request")
    suspend fun requestSignupEmailCode(
        @Body request: EmailCodeRequest,
    ): ApiResponse<ActionResponse>

    @POST("api/auth/email-verification/signup/verify")
    suspend fun verifySignupEmailCode(
        @Body request: VerifyEmailCodeRequest,
    ): ApiResponse<VerificationTokenResponse>

    @POST("api/auth/password-reset/request")
    suspend fun requestPasswordResetCode(
        @Body request: EmailCodeRequest,
    ): ApiResponse<ActionResponse>

    @POST("api/auth/password-reset/verify")
    suspend fun verifyPasswordResetCode(
        @Body request: VerifyEmailCodeRequest,
    ): ApiResponse<VerificationTokenResponse>

    @POST("api/auth/password-reset/confirm")
    suspend fun confirmPasswordReset(
        @Body request: PasswordResetRequest,
    ): ApiResponse<ActionResponse>

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

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): ApiResponse<LoginResponse>

    @POST("api/auth/kakao")
    suspend fun loginWithKakao(@Body request: KakaoLoginRequest): ApiResponse<LoginResponse>
}
