package com.runway.android.ui.achievements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.user.model.AchievementItem
import com.runway.android.domain.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    var items by mutableStateOf<List<AchievementItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun retry() = load()

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            doLoad()
            isRefreshing = false
        }
    }

    private fun load() {
        viewModelScope.launch { doLoad() }
    }

    private suspend fun doLoad() {
        isLoading = true
        errorMessage = null
        when (val result = userRepository.getAchievements()) {
            is NetworkResult.Success -> items = result.data.items
            is NetworkResult.ApiError -> errorMessage = result.message
            is NetworkResult.NetworkError -> errorMessage = "네트워크 연결을 확인해 주세요."
        }
        isLoading = false
    }
}
