package com.runway.android.data.auth.model

/** POST /api/auth/signup → ApiResponse<SignupResponse>.data */
data class SignupResponse(
    val userId: String,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
    val bio: String?,
    val createdAt: String,
)
