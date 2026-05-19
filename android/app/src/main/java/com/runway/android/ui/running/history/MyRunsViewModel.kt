package com.runway.android.ui.running.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.RunSummaryResponse
import com.runway.android.domain.running.RunningRepository
import com.runway.android.ui.components.RunHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
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

    private fun loadRuns() {
        viewModelScope.launch {
            val result = runningRepository.getMyRuns(page = 0, size = 50)
            if (result is NetworkResult.Success) {
                totalCount = result.data.totalElements
                runs = result.data.content.map { it.toHistoryItem() }
            } else {
                hasError = true
            }
            isLoading = false
        }
    }

    private fun RunSummaryResponse.toHistoryItem(): RunHistoryItem {
        val zone = ZoneId.systemDefault()
        val dateLabel = runCatching {
            val dt = Instant.parse(startedAt).atZone(zone)
            val month = dt.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            "$month ${dt.dayOfMonth}, ${dt.year}"
        }.getOrDefault("")

        val distKm = "%.2f".format((distanceMeters ?: 0.0) / 1000.0)
        val dur = durationSeconds?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "--:--"
        val pace = avgPaceSecondsPerKm?.let { secs ->
            "%d'%02d\"".format(secs / 60, secs % 60)
        } ?: "--'--\""

        return RunHistoryItem(
            runId = runId,
            dateLabel = dateLabel,
            distanceKm = distKm,
            duration = dur,
            pace = pace,
        )
    }
}
