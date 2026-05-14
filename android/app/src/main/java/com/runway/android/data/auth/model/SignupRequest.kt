package com.runway.android.data.auth.model

/** POST /api/auth/signup request body */
data class SignupRequest(
    val email: String,
    val password: String,
    val nickname: String,
)
