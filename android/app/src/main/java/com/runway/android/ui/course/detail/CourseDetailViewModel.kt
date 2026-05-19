package com.runway.android.ui.course.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.CourseDetailResponse
import com.runway.android.data.course.model.CoursePointResponse
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    private val courseId: String = checkNotNull(savedStateHandle["courseId"])

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var courseDetail by mutableStateOf<CourseDetailResponse?>(null)
        private set
    var coursePoints by mutableStateOf<List<CoursePointResponse>>(emptyList())
        private set

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val detailDeferred = async { courseRepository.getCourseDetail(courseId) }
            val pointsDeferred = async { courseRepository.getCoursePoints(courseId) }

            when (val result = detailDeferred.await()) {
                is NetworkResult.Success -> courseDetail = result.data
                is NetworkResult.ApiError -> errorMessage = result.message
                is NetworkResult.NetworkError -> errorMessage = "네트워크 연결을 확인해 주세요."
            }

            when (val result = pointsDeferred.await()) {
                is NetworkResult.Success -> coursePoints = result.data.points
                else -> { /* points 로드 실패는 detail 표시에 영향 없음 */ }
            }

            isLoading = false
        }
    }
}
