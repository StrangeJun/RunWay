package com.runway.android.data.auth.model

data class EmailCodeRequest(val email: String)

data class VerifyEmailCodeRequest(
    val email: String,
    val code: String,
)

data class VerificationTokenResponse(val verificationToken: String)

data class PasswordResetRequest(
    val verificationToken: String,
    val newPassword: String,
)

data class ActionResponse(val accepted: Boolean)
