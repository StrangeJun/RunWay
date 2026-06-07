package com.runway.android.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.runway.android.BuildConfig
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.data.running.model.RunSummaryResponse
import com.runway.android.data.running.model.RunningStatsResponse
import com.runway.android.domain.course.CourseRepository
import com.runway.android.domain.running.RunningRepository
import com.runway.android.domain.user.UserRepository
import com.runway.android.core.map.MapPoint
import com.runway.android.ui.components.RecentRun
import com.runway.android.ui.components.WeatherInfo
import com.runway.android.ui.components.WeeklyStats
import com.runway.android.core.util.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runningRepository: RunningRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    @Named("noAuth") private val okHttpClient: OkHttpClient,
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
    var nearbyCourses by mutableStateOf<List<NearbyCourseItem>>(emptyList())
        private set
    var isLoadingNearbyCourses by mutableStateOf(false)
        private set
    var weatherInfo by mutableStateOf<WeatherInfo?>(null)
        private set
    var currentLocation by mutableStateOf<MapPoint?>(null)
        private set
    var selectedGoal by mutableStateOf<RunGoal?>(null)
        private set
    var showGoalSheet by mutableStateOf(false)

    val locationPermissionGranted: Boolean
        get() = hasLocationPermission()

    fun openGoalSheet()  { showGoalSheet = true }
    fun closeGoalSheet() { showGoalSheet = false }

    fun removeRecentRun(runId: String) {
        recentRuns = recentRuns.filterNot { it.runId == runId }
    }

    var isRefreshing by mutableStateOf(false)
        private set

    fun reloadRecentRuns() {
        viewModelScope.launch {
            val result = runningRepository.getMyRuns(page = 0, size = 20)
            if (result is NetworkResult.Success) {
                recentRuns = result.data.content.map { it.toRecentRun() }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            nearbyCourses = emptyList()
            weatherInfo = null
            // Inline the essential awaits to correctly track completion
            val runsResult = runningRepository.getMyRuns(page = 0, size = HOME_RUN_FETCH_SIZE)
            if (runsResult is NetworkResult.Success) {
                recentRuns = runsResult.data.content.take(RECENT_RUN_COUNT).map { it.toRecentRun() }
            }
            val statsResult = runningRepository.getRunningStats("weekly")
            weeklyStats = resolveWeeklyStats(runsResult, statsResult)
            loadNearbyCoursesIfPermitted()
            loadWeatherIfPermitted()
            isRefreshing = false
        }
    }
    fun setGoal(goal: RunGoal) {
        selectedGoal = goal
        showGoalSheet = false
    }
    fun clearGoal() { selectedGoal = null }

    init {
        loadData()
    }

    fun tryLoadNearbyCourses() {
        if (isLoadingNearbyCourses || nearbyCourses.isNotEmpty()) return
        loadNearbyCoursesIfPermitted()
    }

    // Called separately from HomeScreen to trigger weather load independently
    fun tryLoadWeather() {
        if (weatherInfo != null) return
        loadWeatherIfPermitted()
    }

    @SuppressLint("MissingPermission")
    private fun loadWeatherIfPermitted() {
        if (!hasLocationPermission()) return
        viewModelScope.launch {
            try {
                val cts = CancellationTokenSource()
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(0)
                    .setDurationMillis(12_000)
                    .build()
                val location = fusedLocationClient
                    .getCurrentLocation(request, cts.token)
                    .await() ?: return@launch
                currentLocation = MapPoint(location.latitude, location.longitude)
                fetchWeatherData(location.latitude, location.longitude)
            } catch (_: Exception) { }
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadNearbyCoursesIfPermitted() {
        if (!hasLocationPermission()) return

        viewModelScope.launch {
            isLoadingNearbyCourses = true
            try {
                val cts = CancellationTokenSource()
                val location = fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .await() ?: return@launch
                if (currentLocation == null) {
                    currentLocation = MapPoint(location.latitude, location.longitude)
                }

                when (val result = courseRepository.getNearbyCourses(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusMeters = 3000,
                    size = 5,
                )) {
                    is NetworkResult.Success -> nearbyCourses = result.data.content
                    else -> {}
                }
            } catch (_: Exception) {
                // silent fail on home screen
            } finally {
                isLoadingNearbyCourses = false
            }
        }
    }

    private suspend fun fetchWeatherData(lat: Double, lon: Double) {
        val apiKey = BuildConfig.OPENWEATHER_API_KEY
        if (apiKey.isBlank()) return

        try {
            val weatherResp = withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&lang=kr&appid=$apiKey")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) Gson().fromJson(resp.body?.string(), OWMWeatherResponse::class.java)
                    else null
                }
            }

            val airResp = withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url("https://api.openweathermap.org/data/2.5/air_pollution?lat=$lat&lon=$lon&appid=$apiKey")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) Gson().fromJson(resp.body?.string(), OWMAirPollutionResponse::class.java)
                    else null
                }
            }

            if (weatherResp != null) {
                val condition = weatherResp.weather.firstOrNull()
                weatherInfo = WeatherInfo(
                    tempCelsius = weatherResp.main.temp.toInt(),
                    humidity = weatherResp.main.humidity,
                    pm10 = airResp?.list?.firstOrNull()?.components?.pm10?.roundToInt() ?: 0,
                    pm25 = airResp?.list?.firstOrNull()?.components?.pm25?.roundToInt() ?: 0,
                    conditionId = condition?.id ?: 800,
                    condition = condition?.main.orEmpty(),
                    description = condition?.description.orEmpty(),
                )
            }
        } catch (_: Exception) {
            // silent fail — weather is non-critical
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                val result = userRepository.getMe()
                if (result is NetworkResult.Success) {
                    nickname = result.data.nickname
                }
            }

            val runsResult = runningRepository.getMyRuns(page = 0, size = HOME_RUN_FETCH_SIZE)
            val statsResult = runningRepository.getRunningStats("weekly")

            if (runsResult is NetworkResult.Success) {
                recentRuns = runsResult.data.content.take(RECENT_RUN_COUNT).map { it.toRecentRun() }
            }

            weeklyStats = resolveWeeklyStats(runsResult, statsResult)
            isLoadingRuns = false
        }
    }

    private fun resolveWeeklyStats(
        runsResult: NetworkResult<com.runway.android.core.model.PageResponse<RunSummaryResponse>>,
        statsResult: NetworkResult<RunningStatsResponse>,
    ): WeeklyStats {
        val runs = (runsResult as? NetworkResult.Success)?.data?.content
        val localStats = runs?.let(::calculateWeeklyStats)
        val localRunCount = runs?.count(::isCurrentWeekCompletedRun) ?: 0
        val serverStats = (statsResult as? NetworkResult.Success)?.data

        return when {
            localRunCount > 0 -> localStats!!
            serverStats != null -> serverStats.toWeeklyStats()
            localStats != null -> localStats
            else -> WeeklyStats("0.0", "0", "--'--\"", "0")
        }
    }

    private fun RunningStatsResponse.toWeeklyStats(): WeeklyStats {
        val pace = if (averagePaceSecondsPerKm > 0) {
            "%d'%02d\"".format(averagePaceSecondsPerKm / 60, averagePaceSecondsPerKm % 60)
        } else {
            "--'--\""
        }
        val streakSuffix = if (currentStreakDays > 0) " · ${currentStreakDays}day streak" else ""
        return WeeklyStats(
            distanceKm = if (totalDistanceMeters < 1000) {
                "%.2f".format(totalDistanceMeters / 1000.0)
            } else {
                "%.1f".format(totalDistanceMeters / 1000.0)
            },
            runs = "$totalRuns$streakSuffix",
            avgPace = pace,
            calories = totalCaloriesBurned.toString(),
        )
    }

    private fun calculateWeeklyStats(runs: List<RunSummaryResponse>): WeeklyStats {
        val weekRuns = runs.filter(::isCurrentWeekCompletedRun)

        val totalDistanceKm = weekRuns.sumOf { it.distanceMeters ?: 0.0 } / 1000.0
        val totalSeconds = weekRuns.sumOf { it.durationSeconds ?: 0 }
        val avgPaceSecsPerKm = if (totalDistanceKm > 0.001) (totalSeconds / totalDistanceKm).toInt() else 0
        val recordedCalories = weekRuns.mapNotNull { it.caloriesBurned }
        val calories = if (recordedCalories.isNotEmpty()) {
            recordedCalories.sum()
        } else {
            (totalDistanceKm * 72).toInt()
        }

        return WeeklyStats(
            distanceKm = "%.1f".format(totalDistanceKm),
            runs = weekRuns.size.toString(),
            avgPace = if (avgPaceSecsPerKm > 0) "%d'%02d\"".format(avgPaceSecsPerKm / 60, avgPaceSecsPerKm % 60) else "--'--\"",
            calories = calories.toString(),
        )
    }

    private fun isCurrentWeekCompletedRun(run: RunSummaryResponse): Boolean {
        if (!run.status.equals("completed", ignoreCase = true)) return false

        val zone = ZoneId.systemDefault()
        val weekStart = LocalDate.now(zone)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()
        val nextWeekStart = weekStart.plusSeconds(7 * 24 * 60 * 60)
        val startedAt = runCatching { Instant.parse(run.startedAt) }.getOrNull() ?: return false
        return startedAt >= weekStart && startedAt < nextWeekStart
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
        val dur = formatDuration(durationSeconds)
        val pace = avgPaceSecondsPerKm?.let { secs ->
            "%d'%02d\"".format(secs / 60, secs % 60)
        } ?: "--'--\""

        return RecentRun(runId = runId, day = dateLabel, distanceKm = distKm, pace = pace, duration = dur)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun buildGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning,"
            in 12..17 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    // ── OpenWeatherMap response models ────────────────────────────────────

    private data class OWMWeatherResponse(
        val main: OWMMain,
        val weather: List<OWMWeatherCondition> = emptyList(),
    )
    private data class OWMMain(val temp: Double, val humidity: Int)
    private data class OWMWeatherCondition(
        val id: Int,
        val main: String,
        val description: String,
    )

    private data class OWMAirPollutionResponse(val list: List<OWMAirEntry>)
    private data class OWMAirEntry(val components: OWMComponents)
    private data class OWMComponents(
        val pm10: Double,
        @SerializedName("pm2_5") val pm25: Double,
    )

    private companion object {
        const val HOME_RUN_FETCH_SIZE = 200
        const val RECENT_RUN_COUNT = 20
    }
}
