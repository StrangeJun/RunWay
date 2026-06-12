package com.runway.wear.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class OfflineCoursePoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
) {
    fun toJson() = JSONObject()
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("altitudeMeters", altitudeMeters)
}

data class OfflineCourse(
    val courseId: String,
    val name: String,
    val description: String?,
    val distanceMeters: Double,
    val distanceFromMeMeters: Double,
    val isLoop: Boolean,
    val isPublic: Boolean,
    val points: List<OfflineCoursePoint>,
) {
    fun toJson() = JSONObject()
        .put("courseId", courseId)
        .put("name", name)
        .put("description", description)
        .put("distanceMeters", distanceMeters)
        .put("distanceFromMeMeters", distanceFromMeMeters)
        .put("isLoop", isLoop)
        .put("isPublic", isPublic)
        .put("points", JSONArray().apply { points.forEach { put(it.toJson()) } })

    companion object {
        fun fromJson(json: JSONObject) = OfflineCourse(
            courseId = json.getString("courseId"),
            name = json.getString("name"),
            description = json.optString("description").takeUnless { it.isBlank() || it == "null" },
            distanceMeters = json.getDouble("distanceMeters"),
            distanceFromMeMeters = json.optDouble("distanceFromMeMeters", 0.0),
            isLoop = json.optBoolean("isLoop"),
            isPublic = json.optBoolean("isPublic", true),
            points = json.getJSONArray("points").let { points ->
                buildList {
                    for (index in 0 until points.length()) {
                        val point = points.getJSONObject(index)
                        add(
                            OfflineCoursePoint(
                                latitude = point.getDouble("latitude"),
                                longitude = point.getDouble("longitude"),
                                altitudeMeters = point.optDouble("altitudeMeters", 0.0),
                            ),
                        )
                    }
                }
            },
        )
    }
}

object OfflineCourseRepository {
    private val _courses = MutableStateFlow<List<OfflineCourse>>(emptyList())
    val courses = _courses.asStateFlow()

    fun update(courses: List<OfflineCourse>) {
        _courses.value = courses
    }
}

class OfflineCourseStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "runway_offline_courses",
        Context.MODE_PRIVATE,
    )

    fun load(): List<OfflineCourse> {
        val array = runCatching { JSONArray(preferences.getString(KEY_COURSES, "[]")) }
            .getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until array.length()) {
                runCatching { OfflineCourse.fromJson(array.getJSONObject(index)) }
                    .getOrNull()
                    ?.let(::add)
            }
        }
    }

    fun replace(courses: List<OfflineCourse>) {
        val array = JSONArray().apply { courses.forEach { put(it.toJson()) } }
        preferences.edit().putString(KEY_COURSES, array.toString()).commit()
        OfflineCourseRepository.update(courses)
    }

    private companion object {
        const val KEY_COURSES = "courses"
    }
}
