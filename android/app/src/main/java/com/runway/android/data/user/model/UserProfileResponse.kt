package com.runway.android.data.user.model

data class UserProfileResponse(
    val userId: String,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
    val bio: String?,
    val createdAt: String,
)
