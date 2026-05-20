package com.runway.android.data.course.model

data class CourseRatingResponse(
    val ratingId: String,
    val courseId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val updatedAt: String,
)
