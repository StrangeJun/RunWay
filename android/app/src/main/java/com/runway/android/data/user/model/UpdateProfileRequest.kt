package com.runway.android.data.user.model

data class UpdateProfileRequest(
    val nickname: String,
    val profileImageUrl: String?,
    val bio: String?,
)
