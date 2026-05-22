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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MyRunsViewModel @Inject constructor(
    private val runningRepository: RunningRepository,
) : ViewModel() {

    var allRuns by mutableStateOf<List<RunHistoryItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var hasError by mutableStateOf(false)
        private set
    var totalCount by mutableStateOf(0L)
        private set

    var selectedMonth by mutableStateOf(YearMonth.now())
        private set
    var selectedDate by mutableStateOf<LocalDate?>(null)
        private set

    /** 현재 월에서 런이 있는 날짜 집합 */
    val datesWithRuns: Set<LocalDate>
        get() = allRuns
            .filter { it.localDate?.let { d -> YearMonth.from(d) == selectedMonth } ?: false }
            .mapNotNull { it.localDate }
            .toSet()

    /** 화면에 표시할 런 목록 — 날짜 선택 시 해당 날짜만, 아니면 현재 월 전체 */
    val runs: List<RunHistoryItem>
        get() {
            val date = selectedDate
            return if (date != null) {
                allRuns.filter { it.localDate == date }
            } else {
                allRuns.filter { it.localDate?.let { d -> YearMonth.from(d) == selectedMonth } ?: false }
            }
        }

    /** 현재 월 요약: 완주 횟수, 총 거리(m), 총 시간(초) */
    val monthlySummary: MonthlySummary
        get() {
            val monthRuns = allRuns.filter {
                it.localDate?.let { d -> YearMonth.from(d) == selectedMonth } ?: false
            }
            val count = monthRuns.size
            val totalDistanceM = monthRuns.sumOf { it.rawDistanceMeters ?: 0.0 }
            val totalDurationSec = monthRuns.sumOf { it.rawDurationSeconds ?: 0 }
            return MonthlySummary(count, totalDistanceM, totalDurationSec)
        }

    init {
        loadRuns()
    }

    fun retry() {
        isLoading = true
        hasError = false
        allRuns = emptyList()
        viewModelScope.launch { doLoad() }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            hasError = false
            doLoad()
            isRefreshing = false
        }
    }

    fun deleteRun(runId: String) {
        viewModelScope.launch {
            runningRepository.deleteRun(runId)
            allRuns = allRuns.filterNot { it.runId == runId }
        }
    }

    fun removeRunLocally(runId: String) {
        allRuns = allRuns.filterNot { it.runId == runId }
    }

    fun previousMonth() {
        selectedMonth = selectedMonth.minusMonths(1)
        selectedDate = null
    }

    fun nextMonth() {
        val next = selectedMonth.plusMonths(1)
        if (!next.isAfter(YearMonth.now())) {
            selectedMonth = next
            selectedDate = null
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate = if (selectedDate == date) null else date
    }

    private fun loadRuns() {
        viewModelScope.launch { doLoad() }
    }

    private suspend fun doLoad() {
        val result = runningRepository.getMyRuns(page = 0, size = 200)
        when (result) {
            is NetworkResult.Success -> {
                totalCount = result.data.totalElements
                allRuns = result.data.content.map { it.toHistoryItem() }
            }
            else -> hasError = true
        }
        isLoading = false
    }

    private fun RunSummaryResponse.toHistoryItem(): RunHistoryItem {
        val date = startedAt.toLocalDate()
        return RunHistoryItem(
            runId = runId,
            dateLabel = formatRunDateShort(startedAt),
            distanceFormatted = formatDistance(distanceMeters),
            duration = formatDuration(durationSeconds),
            pace = formatPace(avgPaceSecondsPerKm),
            status = status,
            calories = caloriesBurned,
            localDate = date,
            rawDistanceMeters = distanceMeters,
            rawDurationSeconds = durationSeconds,
        )
    }
}

data class MonthlySummary(
    val runCount: Int,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Int,
)

private fun String?.toLocalDate(): LocalDate? {
    if (this == null) return null
    return runCatching {
        Instant.parse(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
}
