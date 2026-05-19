package com.runway.android.data.course.model

data class CoursePointsResponse(
    val courseId: String,
    val points: List<CoursePointResponse>,
)
