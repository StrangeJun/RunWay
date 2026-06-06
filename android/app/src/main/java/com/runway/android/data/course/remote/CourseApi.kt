package com.runway.android.data.course.remote

import com.runway.android.core.model.ApiResponse
import com.runway.android.core.model.PageResponse
import com.runway.android.data.course.model.CourseDetailResponse
import com.runway.android.data.course.model.CoursePointsResponse
import com.runway.android.data.course.model.CourseRatingRequest
import com.runway.android.data.course.model.CourseRatingResponse
import com.runway.android.data.course.model.CourseReportRequest
import com.runway.android.data.course.model.CourseReportResponse
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.CreateCourseFromRunRequest
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.data.course.model.ParticipatedCourseItem
import com.runway.android.data.course.model.CourseStatusResponse
import com.runway.android.data.course.model.PublishCourseRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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
        @Query("keyword") keyword: String? = null,
        @Query("includeRoutePoints") includeRoutePoints: Boolean? = null,
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

    @POST("api/courses/{courseId}/ratings")
    suspend fun rateCourse(
        @Path("courseId") courseId: String,
        @Body request: CourseRatingRequest,
    ): ApiResponse<CourseRatingResponse>

    @GET("api/courses/favorites")
    suspend fun getFavoriteCourses(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiResponse<PageResponse<CourseResponse>>

    @GET("api/courses/participated")
    suspend fun getParticipatedCourses(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiResponse<PageResponse<ParticipatedCourseItem>>

    @POST("api/courses/{courseId}/favorite")
    suspend fun addFavorite(
        @Path("courseId") courseId: String,
    ): ApiResponse<Unit>

    @DELETE("api/courses/{courseId}/favorite")
    suspend fun removeFavorite(
        @Path("courseId") courseId: String,
    ): ApiResponse<Unit>

    @PATCH("api/courses/{courseId}/publish")
    suspend fun publishCourse(
        @Path("courseId") courseId: String,
        @Body request: PublishCourseRequest,
    ): ApiResponse<CourseStatusResponse>

    @PATCH("api/courses/{courseId}/archive")
    suspend fun archiveCourse(
        @Path("courseId") courseId: String,
    ): ApiResponse<CourseStatusResponse>
}
