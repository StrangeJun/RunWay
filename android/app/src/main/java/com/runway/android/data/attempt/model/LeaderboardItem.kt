package com.runway.android.data.attempt.model

data class LeaderboardItem(
    val rank: Int,
    val userId: String,
    val nickname: String,
    val profileImageUrl: String?,
    val bestTimeSeconds: Int,
    val completionCount: Int,
)
