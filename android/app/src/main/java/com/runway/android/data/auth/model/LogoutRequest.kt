package com.runway.android.data.auth.model

/** POST /api/auth/logout request body */
data class LogoutRequest(
    val refreshToken: String,
)
