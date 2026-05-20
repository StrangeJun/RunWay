package com.runway.android.data.user.model

data class AchievementItem(
    val code: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val unlockedAt: String?,
    val progress: Long,
    val target: Long,
)
