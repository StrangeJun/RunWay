package com.runway.android.data.course

import com.runway.android.core.model.PageResponse
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.result.safeApiCall
import com.runway.android.data.course.model.CourseDetailResponse
import com.runway.android.data.course.model.CoursePointsResponse
import com.runway.android.data.course.model.CourseRatingRequest
import com.runway.android.data.course.model.CourseRatingResponse
import com.runway.android.data.course.model.CourseReportRequest
import com.runway.android.data.course.model.CourseReportResponse
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.CreateCourseFromRunRequest
import com.runway.android.data.course.model.NearbyCourseItem
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

    override suspend fun getNearbyCourses(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        minDistanceMeters: Double?,
        maxDistanceMeters: Double?,
        isLoop: Boolean?,
        keyword: String?,
        page: Int,
        size: Int,
    ): NetworkResult<PageResponse<NearbyCourseItem>> = safeApiCall {
        courseApi.getNearbyCourses(
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            minDistanceMeters = minDistanceMeters,
            maxDistanceMeters = maxDistanceMeters,
            isLoop = isLoop,
            keyword = keyword.takeIf { !it.isNullOrBlank() },
            page = page,
            size = size,
        )
    }

    override suspend fun getMyCourses(
        status: String?,
        page: Int,
        size: Int,
    ): NetworkResult<PageResponse<CourseResponse>> = safeApiCall {
        courseApi.getMyCourses(status = status, page = page, size = size)
    }

    override suspend fun getCourseDetail(courseId: String): NetworkResult<CourseDetailResponse> =
        safeApiCall { courseApi.getCourseDetail(courseId) }

    override suspend fun getCoursePoints(courseId: String): NetworkResult<CoursePointsResponse> =
        safeApiCall { courseApi.getCoursePoints(courseId) }

    override suspend fun reportCourse(
        courseId: String,
        request: CourseReportRequest,
    ): NetworkResult<CourseReportResponse> = safeApiCall { courseApi.reportCourse(courseId, request) }

    override suspend fun rateCourse(
        courseId: String,
        request: CourseRatingRequest,
    ): NetworkResult<CourseRatingResponse> = safeApiCall { courseApi.rateCourse(courseId, request) }
}
