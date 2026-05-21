package com.runway.android.ui.course.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.ParticipatedCourseItem
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoursesLibraryViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
) : ViewModel() {

    // ─── 만든 코스 ───
    var myCourses by mutableStateOf<List<CourseResponse>>(emptyList())
        private set
    var isLoadingMyCourses by mutableStateOf(false)
        private set
    var myCoursesError by mutableStateOf<String?>(null)
        private set

    // ─── 즐겨찾기 ───
    var favoriteCourses by mutableStateOf<List<CourseResponse>>(emptyList())
        private set
    var isLoadingFavorites by mutableStateOf(false)
        private set
    var favoritesError by mutableStateOf<String?>(null)
        private set

    // ─── 참여한 코스 ───
    var participatedCourses by mutableStateOf<List<ParticipatedCourseItem>>(emptyList())
        private set
    var isLoadingParticipated by mutableStateOf(false)
        private set
    var participatedError by mutableStateOf<String?>(null)
        private set

    init {
        loadMyCourses()
        loadFavoriteCourses()
        loadParticipatedCourses()
    }

    fun loadMyCourses() {
        viewModelScope.launch {
            isLoadingMyCourses = true
            myCoursesError = null
            when (val result = courseRepository.getMyCourses()) {
                is NetworkResult.Success -> myCourses = result.data.content
                is NetworkResult.ApiError -> myCoursesError = result.message
                is NetworkResult.NetworkError -> myCoursesError = "네트워크 오류가 발생했습니다."
            }
            isLoadingMyCourses = false
        }
    }

    fun loadFavoriteCourses() {
        viewModelScope.launch {
            isLoadingFavorites = true
            favoritesError = null
            when (val result = courseRepository.getFavoriteCourses()) {
                is NetworkResult.Success -> favoriteCourses = result.data.content
                is NetworkResult.ApiError -> favoritesError = result.message
                is NetworkResult.NetworkError -> favoritesError = "네트워크 오류가 발생했습니다."
            }
            isLoadingFavorites = false
        }
    }

    fun loadParticipatedCourses() {
        viewModelScope.launch {
            isLoadingParticipated = true
            participatedError = null
            when (val result = courseRepository.getParticipatedCourses()) {
                is NetworkResult.Success -> participatedCourses = result.data.content
                is NetworkResult.ApiError -> participatedError = result.message
                is NetworkResult.NetworkError -> participatedError = "네트워크 오류가 발생했습니다."
            }
            isLoadingParticipated = false
        }
    }
}
