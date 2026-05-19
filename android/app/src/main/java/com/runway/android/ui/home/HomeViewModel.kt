package com.runway.android.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.RunSummaryResponse
import com.runway.android.domain.running.RunningRepository
import com.runway.android.domain.user.UserRepository
import com.runway.android.ui.components.RecentRun
import com.runway.android.ui.components.WeeklyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val runningRepository: RunningRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    val greeting: String = buildGreeting()

    var nickname by mutableStateOf("")
        private set
    var weeklyStats by mutableStateOf(WeeklyStats("--", "--", "--'--\"", "--"))
        private set
    var recentRuns by mutableStateOf<List<RecentRun>>(emptyList())
        private set
    var isLoadingRuns by mutableStateOf(true)
        private set

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                val result = userRepository.getMe()
                if (result is NetworkResult.Success) {
                    nickname = result.data.nickname
                }
            }

            val runsResult = runningRepository.getMyRuns(page = 0, size = 20)
            if (runsResult is NetworkResult.Success) {
                val runs = runsResult.data.content
                recentRuns = runs.map { it.toRecentRun() }
                weeklyStats = calculateWeeklyStats(runs)
            }
            isLoadingRuns = false
        }
    }

    private fun calculateWeeklyStats(runs: List<RunSummaryResponse>): WeeklyStats {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weekStart = today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()

        val weekRuns = runs.filter { run ->
            val startedAt = runCatching { Instant.parse(run.startedAt) }.getOrNull() ?: return@filter false
            startedAt >= weekStart
        }

        val totalDistanceKm = weekRuns.sumOf { it.distanceMeters ?: 0.0 } / 1000.0
        val totalSeconds = weekRuns.sumOf { it.durationSeconds ?: 0 }
        val avgPaceSecsPerKm = if (totalDistanceKm > 0.001) (totalSeconds / totalDistanceKm).toInt() else 0
        val calories = (weekRuns.sumOf { it.distanceMeters ?: 0.0 } / 1000.0 * 72).toInt()

        return WeeklyStats(
            distanceKm = "%.1f".format(totalDistanceKm),
            runs = weekRuns.size.toString(),
            avgPace = if (avgPaceSecsPerKm > 0) "%d'%02d\"".format(avgPaceSecsPerKm / 60, avgPaceSecsPerKm % 60) else "--'--\"",
            calories = calories.toString(),
        )
    }

    private fun RunSummaryResponse.toRecentRun(): RecentRun {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dateLabel = runCatching {
            val dt = Instant.parse(startedAt).atZone(zone).toLocalDate()
            when {
                dt == today -> "Today"
                dt == today.minusDays(1) -> "Yesterday"
                else -> dt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            }
        }.getOrDefault("")

        val distKm = "%.2f".format((distanceMeters ?: 0.0) / 1000.0)
        val dur = durationSeconds?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "--:--"
        val pace = avgPaceSecondsPerKm?.let { secs ->
            "%d'%02d\"".format(secs / 60, secs % 60)
        } ?: "--'--\""

        return RecentRun(day = dateLabel, distanceKm = distKm, pace = pace, duration = dur)
    }

    private fun buildGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning,"
            in 12..17 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }
}
