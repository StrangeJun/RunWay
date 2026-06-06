package com.runway.android.domain.course

import com.runway.android.core.model.PageResponse
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.CourseDetailResponse
import com.runway.android.data.course.model.CoursePointsResponse
import com.runway.android.data.course.model.CourseRatingRequest
import com.runway.android.data.course.model.CourseRatingResponse
import com.runway.android.data.course.model.CourseReportRequest
import com.runway.android.data.course.model.CourseReportResponse
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.CreateCourseFromRunRequest
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.data.course.model.CourseStatusResponse
import com.runway.android.data.course.model.ParticipatedCourseItem
import com.runway.android.data.course.model.PublishCourseRequest

interface CourseRepository {

    suspend fun createCourseFromRun(
        runId: String,
        request: CreateCourseFromRunRequest,
    ): NetworkResult<CourseResponse>

    suspend fun getNearbyCourses(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 3000,
        minDistanceMeters: Double? = null,
        maxDistanceMeters: Double? = null,
        isLoop: Boolean? = null,
        keyword: String? = null,
        includeRoutePoints: Boolean = true,
        includeExactStartPoint: Boolean = false,
        page: Int = 0,
        size: Int = 20,
    ): NetworkResult<PageResponse<NearbyCourseItem>>

    suspend fun getMyCourses(
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): NetworkResult<PageResponse<CourseResponse>>

    suspend fun getCourseDetail(courseId: String): NetworkResult<CourseDetailResponse>

    suspend fun getCoursePoints(courseId: String): NetworkResult<CoursePointsResponse>

    suspend fun reportCourse(courseId: String, request: CourseReportRequest): NetworkResult<CourseReportResponse>

    suspend fun rateCourse(courseId: String, request: CourseRatingRequest): NetworkResult<CourseRatingResponse>

    suspend fun getFavoriteCourses(page: Int = 0, size: Int = 20): NetworkResult<PageResponse<CourseResponse>>

    suspend fun getParticipatedCourses(page: Int = 0, size: Int = 20): NetworkResult<PageResponse<ParticipatedCourseItem>>

    suspend fun addFavorite(courseId: String): NetworkResult<Unit>

    suspend fun removeFavorite(courseId: String): NetworkResult<Unit>

    suspend fun publishCourse(courseId: String, request: PublishCourseRequest): NetworkResult<CourseStatusResponse>

    suspend fun archiveCourse(courseId: String): NetworkResult<CourseStatusResponse>
}
