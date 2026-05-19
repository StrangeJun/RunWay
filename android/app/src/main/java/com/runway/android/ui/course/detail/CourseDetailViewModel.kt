package com.runway.android.ui.course.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.attempt.model.StartAttemptRequest
import com.runway.android.data.course.model.CourseDetailResponse
import com.runway.android.data.course.model.CoursePointResponse
import com.runway.android.domain.attempt.CourseAttemptRepository
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class AttemptStartedEvent(
    val courseId: String,
    val courseAttemptId: String,
    val runningRecordId: String,
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
    private val courseAttemptRepository: CourseAttemptRepository,
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var courseDetail by mutableStateOf<CourseDetailResponse?>(null)
        private set
    var coursePoints by mutableStateOf<List<CoursePointResponse>>(emptyList())
        private set

    var isStartingAttempt by mutableStateOf(false)
        private set
    var startAttemptError by mutableStateOf<String?>(null)
        private set

    private val _navigateToAttempt = MutableSharedFlow<AttemptStartedEvent>()
    val navigateToAttempt = _navigateToAttempt.asSharedFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun clearStartAttemptError() {
        startAttemptError = null
    }

    fun startAttempt() {
        if (isStartingAttempt) return
        isStartingAttempt = true
        startAttemptError = null

        viewModelScope.launch {
            when (val result = courseAttemptRepository.startAttempt(
                courseId = courseId,
                request = StartAttemptRequest(startedAt = Instant.now().toString()),
            )) {
                is NetworkResult.Success -> {
                    _navigateToAttempt.emit(
                        AttemptStartedEvent(
                            courseId = courseId,
                            courseAttemptId = result.data.courseAttemptId,
                            runningRecordId = result.data.runningRecordId,
                        )
                    )
                }
                is NetworkResult.ApiError -> startAttemptError = result.message
                is NetworkResult.NetworkError -> startAttemptError = "네트워크 연결을 확인해 주세요."
            }
            isStartingAttempt = false
        }
    }

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
