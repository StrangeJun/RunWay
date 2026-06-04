package com.runway.android.data.course.model

data class PublishCourseRequest(
    val difficulty: String,
    val slopeLevel: String,
    val riskLevel: String,
    val surfaceType: String,
    val recommendedTime: String,
    val warnings: String?,
    val description: String?,
)
