package com.runway.android.ui.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
) : ViewModel() {

    companion object {
        // backend-test-guide 기준 고정 좌표 — Phase B-9 이후 실제 GPS로 대체 예정
        private const val TEST_LATITUDE = 36.9706
        private const val TEST_LONGITUDE = 127.8718
    }

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var courses by mutableStateOf<List<NearbyCourseItem>>(emptyList())
        private set
    var radiusMeters by mutableStateOf(3000)
        private set
    var isLoopFilter by mutableStateOf<Boolean?>(null)
        private set

    init {
        loadCourses()
    }

    fun onRadiusChange(meters: Int) {
        if (radiusMeters == meters) return
        radiusMeters = meters
        loadCourses()
    }

    fun onIsLoopFilterChange(value: Boolean?) {
        if (isLoopFilter == value) return
        isLoopFilter = value
        loadCourses()
    }

    fun refresh() = loadCourses()

    private fun loadCourses() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = courseRepository.getNearbyCourses(
                latitude = TEST_LATITUDE,
                longitude = TEST_LONGITUDE,
                radiusMeters = radiusMeters,
                isLoop = isLoopFilter,
            )) {
                is NetworkResult.Success -> courses = result.data.content
                is NetworkResult.ApiError -> errorMessage = result.message
                is NetworkResult.NetworkError -> errorMessage = "네트워크 오류가 발생했습니다."
            }
            isLoading = false
        }
    }
}
