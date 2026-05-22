package com.runway.android.ui.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.attempt.model.LeaderboardResponse
import com.runway.android.domain.attempt.CourseAttemptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseLeaderboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseAttemptRepository: CourseAttemptRepository,
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])

    val isPR: Boolean = savedStateHandle["isPR"] ?: false
    val previousBestSeconds: Int? = (savedStateHandle["previousBestSeconds"] as? Int)?.takeIf { it >= 0 }
    val improvementSeconds: Int? = (savedStateHandle["improvementSeconds"] as? Int)?.takeIf { it >= 0 }

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var leaderboard by mutableStateOf<LeaderboardResponse?>(null)
        private set
    var sortBy by mutableStateOf("fastest_time")
        private set

    init {
        load()
    }

    fun refresh() = load()

    fun updateSortBy(value: String) {
        if (sortBy == value) return
        sortBy = value
        load()
    }

    private fun load() {
        viewModelScope.launch {
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
}
