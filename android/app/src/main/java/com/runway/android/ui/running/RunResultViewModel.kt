package com.runway.android.ui.running

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.map.MapPoint
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.CreateCourseFromRunRequest
import com.runway.android.domain.course.CourseRepository
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RunResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
    private val runningRepository: RunningRepository,
) : ViewModel() {

    val runId: String? = savedStateHandle.get<String>("runId")?.takeIf { it != "none" }

    var routePoints by mutableStateOf<List<MapPoint>>(emptyList())
        private set

    var isLoadingRoute by mutableStateOf(runId != null)
        private set

    private val elapsedSeconds: Int = savedStateHandle.get<Int>("elapsedSeconds") ?: 0
    private val distanceKm: Float = savedStateHandle.get<Float>("distanceKm") ?: 0f

    init {
        runId?.let { id ->
            viewModelScope.launch {
                val result = runningRepository.getRunDetail(id)
                if (result is NetworkResult.Success) {
                    routePoints = result.data.points.map { MapPoint(it.latitude, it.longitude) }
                }
                isLoadingRoute = false
            }
        }
    }

    val timerText: String = run {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
    val distanceText: String = "%.2f".format(distanceKm)
    val paceText: String = if (distanceKm > 0.001f) {
        val secsPerKm = (elapsedSeconds / distanceKm).toInt()
        "%d'%02d\"/km".format(secsPerKm / 60, secsPerKm % 60)
    } else {
        "--'--\"/km"
    }
    val caloriesText: String = "${(distanceKm * 72).toInt()} kcal"
    val stepsText: String = String.format(Locale.US, "%,d", (distanceKm * 1320).toInt())

    // ─── 코스 생성 다이얼로그 상태 ───

    var showCreateDialog by mutableStateOf(false)
        private set
    var courseName by mutableStateOf("")
        private set
    var courseDescription by mutableStateOf("")
        private set
    var isLoop by mutableStateOf(false)
        private set
    var publish by mutableStateOf(true)
        private set
    var isCreating by mutableStateOf(false)
        private set
    var createError by mutableStateOf<String?>(null)
        private set

    var courseCreatedSuccess by mutableStateOf(false)
        private set

    private val _courseCreated = MutableSharedFlow<String>()
    val courseCreated = _courseCreated.asSharedFlow()

    fun onShowCreateDialog() {
        createError = null
        showCreateDialog = true
    }

    fun onDismissCreateDialog() {
        if (!isCreating) showCreateDialog = false
    }

    fun onCourseNameChange(value: String) {
        courseName = value
        createError = null
    }

    fun onDescriptionChange(value: String) {
        courseDescription = value
    }

    fun onIsLoopChange(value: Boolean) {
        isLoop = value
    }

    fun onPublishChange(value: Boolean) {
        publish = value
    }

    fun createCourse() {
        val id = runId ?: return
        if (courseName.isBlank()) {
            createError = "코스 이름을 입력해 주세요."
            return
        }
        viewModelScope.launch {
            isCreating = true
            createError = null
            val request = CreateCourseFromRunRequest(
                name = courseName.trim(),
                description = courseDescription.trim().ifBlank { null },
                isLoop = isLoop,
                publish = publish,
            )
            when (val result = courseRepository.createCourseFromRun(id, request)) {
                is NetworkResult.Success -> {
                    showCreateDialog = false
                    courseCreatedSuccess = true
                    kotlinx.coroutines.delay(1_500)
                    _courseCreated.emit(result.data.courseId)
                }
                is NetworkResult.ApiError -> {
                    createError = when {
                        result.errorCode == "NOT_COMPLETED_RUN" ->
                            "완료된 러닝만 코스로 만들 수 있습니다."
                        result.statusCode == 401 ->
                            "로그인이 만료되었습니다. 다시 로그인해주세요."
                        else -> result.message
                    }
                }
                is NetworkResult.NetworkError ->
                    createError = "네트워크 오류가 발생했습니다."
            }
            isCreating = false
        }
    }
}
