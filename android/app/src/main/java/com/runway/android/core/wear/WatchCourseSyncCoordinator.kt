package com.runway.android.core.wear

import android.content.Context
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.runway.android.core.result.NetworkResult
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchCourseSyncCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val courseRepository: CourseRepository,
) {
    private val dataClient = Wearable.getDataClient(context)

    suspend fun syncNearby(latitude: Double, longitude: Double) {
        // 1. 주변 공개 코스 (route points 포함)
        val nearbyResult = courseRepository.getNearbyCourses(
            latitude = latitude,
            longitude = longitude,
            radiusMeters = SEARCH_RADIUS_METERS,
            includeRoutePoints = true,
            includeExactStartPoint = true,
            size = MAX_COURSES,
        )
        val nearbyCourses = if (nearbyResult is NetworkResult.Success) {
            nearbyResult.data.content.filter { it.routePoints.size >= 2 }
        } else emptyList()
        val nearbyIds = nearbyCourses.map { it.courseId }.toSet()

        // 2. 내 코스 (draft + published, route points 별도 조회)
        val myCoursesResult = courseRepository.getMyCourses(size = MAX_MY_COURSES)
        val myCourseItems = if (myCoursesResult is NetworkResult.Success) {
            myCoursesResult.data.content.filter { it.courseId !in nearbyIds }
        } else emptyList()

        // 3. 내 코스 포인트 조회 (병렬 아닌 순차 — 부하 최소화)
        val myCoursesWithPoints = myCourseItems.mapNotNull { course ->
            val pointsResult = courseRepository.getCoursePoints(course.courseId)
            if (pointsResult is NetworkResult.Success && pointsResult.data.points.size >= 2) {
                Pair(course, pointsResult.data.points)
            } else null
        }

        // 4. 합쳐서 JSON 직렬화
        val allCourses = JSONArray().apply {
            // 주변 공개 코스
            nearbyCourses.take(MAX_COURSES).forEach { course ->
                put(buildCourseJson(
                    courseId = course.courseId,
                    name = course.name,
                    description = course.description,
                    distanceMeters = course.distanceMeters,
                    distanceFromMeMeters = course.distanceFromMeMeters,
                    isLoop = course.isLoop,
                    isPublic = true,
                    points = course.routePoints.map { Triple(it.latitude, it.longitude, 0.0) },
                ))
            }
            // 내 코스 (개인/공개 모두)
            myCoursesWithPoints.take(MAX_MY_COURSES).forEach { (course, points) ->
                put(buildCourseJson(
                    courseId = course.courseId,
                    name = course.name,
                    description = course.description,
                    distanceMeters = course.distanceMeters,
                    distanceFromMeMeters = 0.0,
                    isLoop = course.isLoop,
                    isPublic = course.status == "published",
                    points = points.map { Triple(it.latitude, it.longitude, 0.0) },
                ))
            }
        }

        val request = PutDataMapRequest.create(WatchCommandListenerService.COURSE_CATALOG_PATH).apply {
            dataMap.putLong("updatedAt", System.currentTimeMillis())
            dataMap.putAsset("courses", Asset.createFromBytes(allCourses.toString().encodeToByteArray()))
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }

    private fun buildCourseJson(
        courseId: String,
        name: String,
        description: String?,
        distanceMeters: Double,
        distanceFromMeMeters: Double,
        isLoop: Boolean,
        isPublic: Boolean,
        points: List<Triple<Double, Double, Double>>,
    ) = JSONObject()
        .put("courseId", courseId)
        .put("name", name)
        .put("description", description)
        .put("distanceMeters", distanceMeters)
        .put("distanceFromMeMeters", distanceFromMeMeters)
        .put("isLoop", isLoop)
        .put("isPublic", isPublic)
        .put("points", JSONArray().apply {
            points.forEach { (lat, lng, alt) ->
                put(JSONObject().put("latitude", lat).put("longitude", lng).put("altitudeMeters", alt))
            }
        })

    private companion object {
        const val SEARCH_RADIUS_METERS = 5_000
        const val MAX_COURSES = 10
        const val MAX_MY_COURSES = 5
    }
}
