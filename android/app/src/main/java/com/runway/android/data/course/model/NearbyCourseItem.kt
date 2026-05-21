package com.runway.android.data.course.model

data class NearbyCourseItem(
    val courseId: String,
    val name: String,
    val description: String?,
    val status: String,
    val distanceMeters: Double,
    val isLoop: Boolean,
    val startPoint: GeoPoint,
    val endPoint: GeoPoint?,
    val attemptCount: Int,
    val completionCount: Int,
    val distanceFromMeMeters: Double,
    val createdAt: String,
    val avgRating: Double?,
    val ratingCount: Long?,
)
