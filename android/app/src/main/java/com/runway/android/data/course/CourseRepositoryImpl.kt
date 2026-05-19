package com.runway.android.data.course

import com.runway.android.core.result.NetworkResult
import com.runway.android.core.result.safeApiCall
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.CreateCourseFromRunRequest
import com.runway.android.data.course.remote.CourseApi
import com.runway.android.domain.course.CourseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val courseApi: CourseApi,
) : CourseRepository {

    override suspend fun createCourseFromRun(
        runId: String,
        request: CreateCourseFromRunRequest,
    ): NetworkResult<CourseResponse> = safeApiCall { courseApi.createCourseFromRun(runId, request) }
}
