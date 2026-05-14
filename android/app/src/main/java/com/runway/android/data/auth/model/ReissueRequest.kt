package com.runway.android.data.auth.model

/** POST /api/auth/reissue request body */
data class ReissueRequest(
    val refreshToken: String,
)
