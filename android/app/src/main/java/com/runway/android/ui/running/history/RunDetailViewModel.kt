package com.runway.android.ui.running.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.running.RunChartCalculator
import com.runway.android.core.running.RunChartPoint
import com.runway.android.core.running.RunSplit
import com.runway.android.core.running.SplitCalculator
import com.runway.android.data.course.model.CreateCourseFromRunRequest
import com.runway.android.data.running.model.RunDetailResponse
import com.runway.android.domain.course.CourseRepository
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runningRepository: RunningRepository,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    val runId: String = checkNotNull(savedStateHandle["runId"])

    var detail by mutableStateOf<RunDetailResponse?>(null)
        private set
    var splits by mutableStateOf<List<RunSplit>>(emptyList())
        private set
    var chartPoints by mutableStateOf<List<RunChartPoint>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var hasError by mutableStateOf(false)
        private set
    var showDeleteDialog by mutableStateOf(false)
    var showTrimDialog by mutableStateOf(false)
    var trimTargetKm by mutableStateOf(0f)
    var isActionLoading by mutableStateOf(false)
        private set
    var isDeleted by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)

    val maxTrimKm: Float get() = ((detail?.distanceMeters ?: 0.0) / 1000.0).toFloat()

    init {
        loadDetail()
    }

    fun openDeleteDialog() { showDeleteDialog = true }
    fun dismissDeleteDialog() { showDeleteDialog = false }
    fun openTrimDialog() {
        trimTargetKm = maxTrimKm
        showTrimDialog = true
    }
    fun dismissTrimDialog() { showTrimDialog = false }

    fun confirmDelete() {
        viewModelScope.launch {
            isActionLoading = true
            showDeleteDialog = false
            val result = runningRepository.deleteRun(runId)
            if (result is com.runway.android.core.result.NetworkResult.Success) {
                isDeleted = true
            } else {
                actionError = "삭제에 실패했습니다."
            }
            isActionLoading = false
        }
    }

    fun confirmTrim() {
        val targetMeters = (trimTargetKm * 1000.0).toDouble()
        viewModelScope.launch {
            isActionLoading = true
            showTrimDialog = false
            when (runningRepository.trimRun(runId, targetMeters)) {
                is com.runway.android.core.result.NetworkResult.Success -> {
                    isLoading = true
                    detail = null
                    loadDetail()
                }
                else -> actionError = "수정에 실패했습니다."
            }
            isActionLoading = false
        }
    }

    fun clearActionError() { actionError = null }

    // ─── 코스 생성 ───

    var showCreateCourseDialog by mutableStateOf(false)
    var courseName by mutableStateOf("")
    var courseDescription by mutableStateOf("")
    var isLoop by mutableStateOf(false)
    var publish by mutableStateOf(true)
    var isCreatingCourse by mutableStateOf(false)
    var createCourseError by mutableStateOf<String?>(null)

    private val _courseCreated = Channel<String>(Channel.BUFFERED)
    val courseCreated = _courseCreated.receiveAsFlow()

    val canCreateCourse: Boolean get() = detail?.status == "completed"

    fun openCreateCourseDialog() {
        courseName = ""
        courseDescription = ""
        isLoop = false
        publish = true
        createCourseError = null
        showCreateCourseDialog = true
    }

    fun dismissCreateCourseDialog() {
        if (!isCreatingCourse) showCreateCourseDialog = false
    }

    fun createCourse() {
        if (courseName.isBlank()) {
            createCourseError = "코스 이름을 입력해 주세요."
            return
        }
        viewModelScope.launch {
            isCreatingCourse = true
            createCourseError = null
            val request = CreateCourseFromRunRequest(
                name = courseName.trim(),
                description = courseDescription.trim().ifBlank { null },
                isLoop = isLoop,
                publish = publish,
            )
            when (val result = courseRepository.createCourseFromRun(runId, request)) {
                is NetworkResult.Success -> {
                    showCreateCourseDialog = false
                    _courseCreated.send(result.data.courseId)
                }
                is NetworkResult.ApiError -> createCourseError = when (result.errorCode) {
                    "NOT_COMPLETED_RUN" -> "완료된 러닝만 코스로 만들 수 있습니다."
                    else -> result.message
                }
                is NetworkResult.NetworkError -> createCourseError = "네트워크 오류가 발생했습니다."
            }
            isCreatingCourse = false
        }
    }

    fun retry() {
        isLoading = true
        hasError = false
        detail = null
        splits = emptyList()
        chartPoints = emptyList()
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            when (val result = runningRepository.getRunDetail(runId)) {
                is NetworkResult.Success -> {
                    detail = result.data
                    splits = SplitCalculator.calculate(result.data.points)
                    chartPoints = RunChartCalculator.calculate(result.data.points)
                }
                else -> hasError = true
            }
            isLoading = false
        }
    }
}
