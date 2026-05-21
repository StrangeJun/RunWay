package com.runway.android.data.attempt.model

data class LeaderboardResponse(
    val courseId: String,
    val items: List<LeaderboardItem>,
    val sortBy: String = "fastest_time",
    val myRank: Long? = null,
)
