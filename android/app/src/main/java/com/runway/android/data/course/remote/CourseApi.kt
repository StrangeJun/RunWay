package com.runway.android.data.course.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.core.model.PageResponse
import com.runway.android.data.course.model.CourseDetailResponse
import com.runway.android.data.course.model.CoursePointsResponse
import com.runway.android.data.course.model.CourseReportRequest
import com.runway.android.data.course.model.CourseReportResponse
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.CreateCourseFromRunRequest
import com.runway.android.data.course.model.NearbyCourseItem
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CourseApi {

    @POST("api/courses/from-run/{runId}")
    suspend fun createCourseFromRun(
        @Path("runId") runId: String,
        @Body request: CreateCourseFromRunRequest,
    ): ApiResponse<CourseResponse>

    @GET("api/courses/nearby")
    suspend fun getNearbyCourses(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusMeters") radiusMeters: Int? = null,
        @Query("minDistanceMeters") minDistanceMeters: Double? = null,
        @Query("maxDistanceMeters") maxDistanceMeters: Double? = null,
        @Query("isLoop") isLoop: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): ApiResponse<PageResponse<NearbyCourseItem>>

    @GET("api/courses/{courseId}")
    suspend fun getCourseDetail(
        @Path("courseId") courseId: String,
    ): ApiResponse<CourseDetailResponse>

    @GET("api/courses/me")
    suspend fun getMyCourses(
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): ApiResponse<PageResponse<CourseResponse>>

    @GET("api/courses/{courseId}/points")
    suspend fun getCoursePoints(
        @Path("courseId") courseId: String,
    ): ApiResponse<CoursePointsResponse>

    @POST("api/courses/{courseId}/reports")
    suspend fun reportCourse(
        @Path("courseId") courseId: String,
        @Body request: CourseReportRequest,
    ): ApiResponse<CourseReportResponse>
}
