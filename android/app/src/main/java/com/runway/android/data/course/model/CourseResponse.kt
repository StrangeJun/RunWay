package com.runway.android.data.course.model

data class CourseResponse(
    val courseId: String,
    val name: String,
    val description: String?,
    val status: String,
    val distanceMeters: Double,
    val isLoop: Boolean,
    val startPoint: GeoPoint,
    val endPoint: GeoPoint,
    val attemptCount: Int,
    val completionCount: Int,
    val createdAt: String,
)
