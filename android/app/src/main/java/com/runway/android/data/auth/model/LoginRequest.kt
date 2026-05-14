package com.runway.android.data.auth.model

/** POST /api/auth/login request body */
data class LoginRequest(
    val email: String,
    val password: String,
)
