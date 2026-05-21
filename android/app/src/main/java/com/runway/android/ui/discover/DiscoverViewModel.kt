package com.runway.android.ui.discover

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class CourseSortOption(val label: String) {
    NEAREST("가까운 순"),
    POPULAR("인기 순"),
    COMPLETION_RATE("완주율 순"),
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var radiusMeters by mutableStateOf(3000)
        private set
    var isLoopFilter by mutableStateOf<Boolean?>(null)
        private set
    var isLocationRequired by mutableStateOf(false)
        private set
    var keyword by mutableStateOf("")
        private set
    var sortOption by mutableStateOf(CourseSortOption.NEAREST)
        private set

    private var rawCourses = listOf<NearbyCourseItem>()
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var hasLocation = false
    private var loadJob: Job? = null

    val courses: List<NearbyCourseItem>
        get() = when (sortOption) {
            CourseSortOption.NEAREST -> rawCourses.sortedBy { it.distanceFromMeMeters }
            CourseSortOption.POPULAR -> rawCourses.sortedByDescending { it.completionCount }
            CourseSortOption.COMPLETION_RATE -> rawCourses.sortedByDescending { completionRate(it) }
        }

    fun onSortChange(option: CourseSortOption) {
        sortOption = option
    }

    fun onRadiusChange(meters: Int) {
        if (radiusMeters == meters) return
        radiusMeters = meters
        if (hasLocation) loadCourses()
    }

    fun onIsLoopFilterChange(value: Boolean?) {
        if (isLoopFilter == value) return
        isLoopFilter = value
        if (hasLocation) loadCourses()
    }

    fun onKeywordChange(value: String) {
        keyword = value
    }

    fun onSearch() {
        if (hasLocation) loadCourses()
    }

    fun clearKeyword() {
        keyword = ""
        if (hasLocation) loadCourses()
    }

    fun refresh() {
        if (isLocationRequired || !hasLocation) return
        loadCourses()
    }

    @SuppressLint("MissingPermission")
    fun onLocationPermissionGranted() {
        isLocationRequired = false
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    hasLocation = true
                    loadCourses()
                } else {
                    isLoading = false
                    errorMessage = "위치를 가져올 수 없습니다. 잠시 후 다시 시도해 주세요."
                }
            } catch (_: Exception) {
                isLoading = false
                errorMessage = "위치를 가져올 수 없습니다."
            }
        }
    }

    fun onLocationPermissionDenied() {
        isLocationRequired = true
        isLoading = false
        errorMessage = "주변 코스 탐색에 위치 권한이 필요합니다."
    }

    private fun loadCourses() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = courseRepository.getNearbyCourses(
                latitude = currentLatitude,
                longitude = currentLongitude,
                radiusMeters = radiusMeters,
                isLoop = isLoopFilter,
                keyword = keyword.trim().takeIf { it.isNotBlank() },
            )) {
                is NetworkResult.Success -> rawCourses = result.data.content
                is NetworkResult.ApiError -> errorMessage = result.message
                is NetworkResult.NetworkError -> errorMessage = "네트워크 오류가 발생했습니다."
            }
            isLoading = false
        }
    }

    companion object {
        fun completionRate(course: NearbyCourseItem): Float =
            if (course.attemptCount > 0) course.completionCount.toFloat() / course.attemptCount else 0f

        fun isPopular(course: NearbyCourseItem): Boolean =
            course.completionCount >= 10 && completionRate(course) >= 0.6f

        fun isNew(course: NearbyCourseItem): Boolean =
            course.attemptCount == 0
    }
}
