package com.runway.android.ui.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.attempt.model.LeaderboardResponse
import com.runway.android.data.course.model.CourseRatingRequest
import com.runway.android.domain.attempt.CourseAttemptRepository
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseLeaderboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseAttemptRepository: CourseAttemptRepository,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])

    val isPR: Boolean = savedStateHandle["isPR"] ?: false
    val previousBestSeconds: Int? = (savedStateHandle["previousBestSeconds"] as? Int)?.takeIf { it != Int.MIN_VALUE }
    val improvementSeconds: Int? = (savedStateHandle["improvementSeconds"] as? Int)?.takeIf { it != Int.MIN_VALUE }
    val justCompleted: Boolean = savedStateHandle["justCompleted"] ?: false

    var isLoading by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var leaderboard by mutableStateOf<LeaderboardResponse?>(null)
        private set
    var sortBy by mutableStateOf("fastest_time")
        private set

    var showRateDialog by mutableStateOf(justCompleted)
        private set
    var ratingValue by mutableStateOf(0)
        private set
    var ratingComment by mutableStateOf("")
        private set
    var isSubmittingRating by mutableStateOf(false)
        private set
    var ratingError by mutableStateOf<String?>(null)
        private set
    var ratingSubmitted by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            doLoad()
            isRefreshing = false
        }
    }

    fun dismissRateDialog() {
        if (!isSubmittingRating) showRateDialog = false
    }

    fun onRatingValueChange(value: Int) {
        ratingValue = value
        ratingError = null
    }

    fun onRatingCommentChange(value: String) {
        ratingComment = value
    }

    fun submitRating() {
        if (isSubmittingRating || ratingValue == 0) return
        isSubmittingRating = true
        ratingError = null
        viewModelScope.launch {
            when (val result = courseRepository.rateCourse(
                courseId = courseId,
                request = CourseRatingRequest(
                    rating = ratingValue,
                    comment = ratingComment.trim().ifBlank { null },
                ),
            )) {
                is NetworkResult.Success -> {
                    ratingSubmitted = true
                    showRateDialog = false
                }
                is NetworkResult.ApiError -> ratingError = when (result.errorCode) {
                    "ALREADY_RATED" -> "이미 평가한 코스입니다."
                    else -> result.message
                }
                is NetworkResult.NetworkError -> ratingError = "네트워크 연결을 확인해 주세요."
            }
            isSubmittingRating = false
        }
    }

    fun updateSortBy(value: String) {
        if (sortBy == value) return
        sortBy = value
        load()
    }

    private fun load() {
        viewModelScope.launch { doLoad() }
    }

    private suspend fun doLoad() {
        isLoading = true
        errorMessage = null
        when (val result = courseAttemptRepository.getLeaderboard(courseId, sortBy = sortBy)) {
            is NetworkResult.Success -> leaderboard = result.data
            is NetworkResult.ApiError -> errorMessage = result.message
            is NetworkResult.NetworkError -> errorMessage = "네트워크 연결을 확인해 주세요."
        }
        isLoading = false
    }
}
