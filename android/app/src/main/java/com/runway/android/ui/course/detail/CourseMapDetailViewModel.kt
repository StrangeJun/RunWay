package com.runway.android.ui.course.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.map.MapPoint
import com.runway.android.core.result.NetworkResult
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseMapDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])

    var points by mutableStateOf<List<MapPoint>>(emptyList())
        private set
    var isLoop by mutableStateOf(false)
        private set
    var courseName by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            val pointsDeferred = async { courseRepository.getCoursePoints(courseId) }
            val detailDeferred = async { courseRepository.getCourseDetail(courseId) }

            when (val r = pointsDeferred.await()) {
                is NetworkResult.Success -> points = r.data.points.map { MapPoint(it.latitude, it.longitude) }
                else -> {}
            }
            when (val r = detailDeferred.await()) {
                is NetworkResult.Success -> {
                    isLoop = r.data.isLoop
                    courseName = r.data.name
                }
                else -> {}
            }
            isLoading = false
        }
    }
}
