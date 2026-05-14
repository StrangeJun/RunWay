package com.runway.android.data.auth.model

/** 로그인 응답 내 user 필드. 프로필 조회(UserApi)의 전체 응답과는 다른 축약형이다. */
data class UserSummary(
    val userId: String,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
)
