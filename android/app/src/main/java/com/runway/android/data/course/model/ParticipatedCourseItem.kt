package com.runway.android.data.course.model

data class ParticipatedCourseItem(
    val courseId: String,
    val name: String,
    val description: String?,
    val distanceMeters: Double,
    val isLoop: Boolean,
    val status: String,
    val attemptCountByMe: Int,
    val completionCountByMe: Int,
    val bestTimeSecondsByMe: Int?,
    val lastAttemptAt: String?,
)
