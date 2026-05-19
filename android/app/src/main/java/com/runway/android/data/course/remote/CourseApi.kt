package com.runway.android.data.course.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.CreateCourseFromRunRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface CourseApi {

    @POST("api/courses/from-run/{runId}")
    suspend fun createCourseFromRun(
        @Path("runId") runId: String,
        @Body request: CreateCourseFromRunRequest,
    ): ApiResponse<CourseResponse>
}
