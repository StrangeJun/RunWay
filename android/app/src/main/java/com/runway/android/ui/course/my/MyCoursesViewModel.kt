package com.runway.android.ui.course.my

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.domain.course.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyCoursesViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
) : ViewModel() {

    var isLoading by mutableStateOf(true)
        private set
    var hasError by mutableStateOf(false)
        private set
    var courses by mutableStateOf<List<CourseResponse>>(emptyList())
        private set

    init {
        load()
    }

    fun retry() {
        isLoading = true
        hasError = false
        courses = emptyList()
        load()
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = courseRepository.getMyCourses()) {
                is NetworkResult.Success -> courses = result.data.content
                else -> hasError = true
            }
            isLoading = false
        }
    }
}
