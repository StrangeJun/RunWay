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
        val result = courseRepository.getNearbyCourses(
            latitude = latitude,
            longitude = longitude,
            radiusMeters = SEARCH_RADIUS_METERS,
            includeRoutePoints = true,
            includeExactStartPoint = true,
            size = MAX_COURSES,
        )
        if (result !is NetworkResult.Success) return

        val courses = JSONArray().apply {
            result.data.content
                .filter { it.routePoints.size >= 2 }
                .take(MAX_COURSES)
                .forEach { course ->
                    put(
                        JSONObject()
                            .put("courseId", course.courseId)
                            .put("name", course.name)
                            .put("description", course.description)
                            .put("distanceMeters", course.distanceMeters)
                            .put("distanceFromMeMeters", course.distanceFromMeMeters)
                            .put("isLoop", course.isLoop)
                            .put(
                                "points",
                                JSONArray().apply {
                                    course.routePoints.forEach { point ->
                                        put(
                                            JSONObject()
                                                .put("latitude", point.latitude)
                                                .put("longitude", point.longitude)
                                                .put("altitudeMeters", 0.0),
                                        )
                                    }
                                },
                            ),
                    )
                }
        }
        val request = PutDataMapRequest.create(WatchCommandListenerService.COURSE_CATALOG_PATH).apply {
            dataMap.putLong("updatedAt", System.currentTimeMillis())
            dataMap.putAsset("courses", Asset.createFromBytes(courses.toString().encodeToByteArray()))
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }

    private companion object {
        const val SEARCH_RADIUS_METERS = 5_000
        const val MAX_COURSES = 10
    }
}
