package com.runway.android.data.auth.model

/** POST /api/auth/login → ApiResponse<LoginResponse>.data */
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val user: UserSummary,
)
