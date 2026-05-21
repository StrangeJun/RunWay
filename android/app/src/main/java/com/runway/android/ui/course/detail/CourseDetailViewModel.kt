package com.runway.android.ui.course.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.attempt.model.LeaderboardItem
import com.runway.android.data.attempt.model.StartAttemptRequest
import com.runway.android.data.course.model.CourseDetailResponse
import com.runway.android.data.course.model.CoursePointResponse
import com.runway.android.data.course.model.CourseRatingRequest
import com.runway.android.data.course.model.CourseReportRequest
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
    var previewLeaderboard by mutableStateOf<List<LeaderboardItem>>(emptyList())
        private set
    var isLoadingLeaderboard by mutableStateOf(false)
        private set

    var isStartingAttempt by mutableStateOf(false)
        private set
    var startAttemptError by mutableStateOf<String?>(null)
        private set

    var showReportDialog by mutableStateOf(false)
        private set
    var reportReason by mutableStateOf("")
        private set
    var reportDescription by mutableStateOf("")
        private set
    var isSubmittingReport by mutableStateOf(false)
        private set
    var reportError by mutableStateOf<String?>(null)
        private set
    var reportSuccess by mutableStateOf(false)
        private set

    var showRateDialog by mutableStateOf(false)
        private set
    var ratingValue by mutableStateOf(0)
        private set
    var ratingComment by mutableStateOf("")
        private set
    var isSubmittingRating by mutableStateOf(false)
        private set
    var ratingError by mutableStateOf<String?>(null)
        private set
    var ratingSuccess by mutableStateOf(false)
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

    fun clearReportSuccess() {
        reportSuccess = false
    }

    fun openReportDialog() {
        reportReason = ""
        reportDescription = ""
        reportError = null
        reportSuccess = false
        showReportDialog = true
    }

    fun dismissReportDialog() {
        if (!isSubmittingReport) showReportDialog = false
    }

    fun onReportReasonChange(value: String) {
        reportReason = value
        reportError = null
    }

    fun onReportDescriptionChange(value: String) {
        reportDescription = value
    }

    fun submitReport() {
        if (isSubmittingReport || reportReason.isEmpty()) return
        isSubmittingReport = true
        reportError = null

        viewModelScope.launch {
            when (val result = courseRepository.reportCourse(
                courseId = courseId,
                request = CourseReportRequest(
                    reason = reportReason,
                    description = reportDescription.trim().ifBlank { null },
                ),
            )) {
                is NetworkResult.Success -> {
                    reportSuccess = true
                    showReportDialog = false
                }
                is NetworkResult.ApiError -> reportError = when (result.errorCode) {
                    "ALREADY_REPORTED" -> "이미 신고한 코스입니다."
                    else -> result.message
                }
                is NetworkResult.NetworkError -> reportError = "네트워크 연결을 확인해 주세요."
            }
            isSubmittingReport = false
        }
    }

    fun openRateDialog() {
        ratingValue = 0
        ratingComment = ""
        ratingError = null
        showRateDialog = true
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

    fun clearRatingSuccess() {
        ratingSuccess = false
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
                    ratingSuccess = true
                    showRateDialog = false
                    // 평균 평점 반영을 위해 상세 정보 새로고침
                    load()
                }
                is NetworkResult.ApiError -> ratingError = result.message
                is NetworkResult.NetworkError -> ratingError = "네트워크 연결을 확인해 주세요."
            }
            isSubmittingRating = false
        }
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
            val leaderboardDeferred = async {
                isLoadingLeaderboard = true
                courseAttemptRepository.getLeaderboard(courseId, page = 0, size = 5)
            }

            when (val result = detailDeferred.await()) {
                is NetworkResult.Success -> courseDetail = result.data
                is NetworkResult.ApiError -> errorMessage = result.message
                is NetworkResult.NetworkError -> errorMessage = "네트워크 연결을 확인해 주세요."
            }

            when (val result = pointsDeferred.await()) {
                is NetworkResult.Success -> coursePoints = result.data.points
                else -> { /* points 로드 실패는 detail 표시에 영향 없음 */ }
            }

            when (val result = leaderboardDeferred.await()) {
                is NetworkResult.Success -> previewLeaderboard = result.data.items.take(5)
                else -> { /* 리더보드 로드 실패 시 조용히 빈 상태 유지 */ }
            }
            isLoadingLeaderboard = false

            isLoading = false
        }
    }
}
