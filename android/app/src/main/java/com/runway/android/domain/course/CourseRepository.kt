package com.runway.android.domain.course

import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.CreateCourseFromRunRequest

interface CourseRepository {

    suspend fun createCourseFromRun(
        runId: String,
        request: CreateCourseFromRunRequest,
    ): NetworkResult<CourseResponse>
}
