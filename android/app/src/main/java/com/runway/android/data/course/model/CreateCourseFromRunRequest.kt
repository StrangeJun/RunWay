package com.runway.android.data.course.model

data class CreateCourseFromRunRequest(
    val name: String,
    val description: String?,
    val isLoop: Boolean,
    val publish: Boolean,
)
