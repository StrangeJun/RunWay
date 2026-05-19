package com.runway.android.ui.running.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.util.formatDistance
import com.runway.android.core.util.formatDuration
import com.runway.android.core.util.formatPace
import com.runway.android.core.util.formatRunDateShort
import com.runway.android.data.running.model.RunSummaryResponse
import com.runway.android.domain.running.RunningRepository
import com.runway.android.ui.components.RunHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRunsViewModel @Inject constructor(
    private val runningRepository: RunningRepository,
) : ViewModel() {

    var runs by mutableStateOf<List<RunHistoryItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var hasError by mutableStateOf(false)
        private set
    var totalCount by mutableStateOf(0L)
        private set

    init {
        loadRuns()
    }

    fun retry() {
        isLoading = true
        hasError = false
        runs = emptyList()
        loadRuns()
    }

    private fun loadRuns() {
        viewModelScope.launch {
            val result = runningRepository.getMyRuns(page = 0, size = 50)
            when (result) {
                is NetworkResult.Success -> {
                    totalCount = result.data.totalElements
                    runs = result.data.content.map { it.toHistoryItem() }
                }
                else -> hasError = true
            }
            isLoading = false
        }
    }

    private fun RunSummaryResponse.toHistoryItem() = RunHistoryItem(
        runId = runId,
        dateLabel = formatRunDateShort(startedAt),
        distanceFormatted = formatDistance(distanceMeters),
        duration = formatDuration(durationSeconds),
        pace = formatPace(avgPaceSecondsPerKm),
        status = status,
        calories = caloriesBurned,
    )
}
