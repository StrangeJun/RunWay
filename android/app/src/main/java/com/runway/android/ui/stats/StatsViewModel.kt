package com.runway.android.ui.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.RunningStatsResponse
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val runningRepository: RunningRepository,
) : ViewModel() {

    val periods = listOf("weekly", "monthly", "yearly", "all")
    val periodLabels = listOf("이번 주", "이번 달", "올해", "전체")

    var selectedPeriodIndex by mutableStateOf(1) // monthly default
        private set
    var stats by mutableStateOf<RunningStatsResponse?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadStats()
    }

    fun selectPeriod(index: Int) {
        if (selectedPeriodIndex == index) return
        selectedPeriodIndex = index
        loadStats()
    }

    fun retry() = loadStats()

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            loadStats()
            isRefreshing = false
        }
    }

    private fun loadStats() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            when (val result = runningRepository.getRunningStats(periods[selectedPeriodIndex])) {
                is NetworkResult.Success -> stats = result.data
                is NetworkResult.ApiError -> errorMessage = result.message
                is NetworkResult.NetworkError -> errorMessage = "네트워크 연결을 확인해 주세요."
            }
            isLoading = false
        }
    }
}
