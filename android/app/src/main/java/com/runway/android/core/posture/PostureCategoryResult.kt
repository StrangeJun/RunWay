package com.runway.android.core.posture

data class PostureCategoryResult(
    val score: Int,
    val measuredValue: Float,
    val idealMin: Float,
    val idealMax: Float,
    val unit: String,
    val feedback: String,
    val tip: String,
)
