package com.runway.android.data.auth.model

/** POST /api/auth/reissue → ApiResponse<ReissueResponse>.data */
data class ReissueResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int,
)
