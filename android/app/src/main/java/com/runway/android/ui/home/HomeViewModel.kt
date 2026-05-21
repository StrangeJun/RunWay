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
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.runway.android.BuildConfig
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.data.running.model.RunSummaryResponse
import com.runway.android.domain.course.CourseRepository
import com.runway.android.domain.running.RunningRepository
import com.runway.android.domain.user.UserRepository
import com.runway.android.ui.components.RecentRun
import com.runway.android.ui.components.WeatherInfo
import com.runway.android.ui.components.WeeklyStats
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

    init {
        loadData()
    }

    fun tryLoadNearbyCourses() {
        if (isLoadingNearbyCourses || nearbyCourses.isNotEmpty()) return
        loadNearbyCoursesIfPermitted()
    }

    @SuppressLint("MissingPermission")
    private fun loadNearbyCoursesIfPermitted() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        viewModelScope.launch {
            isLoadingNearbyCourses = true
            try {
                val location = fusedLocationClient.lastLocation.await() ?: return@launch

                // Fetch weather concurrently while nearby courses load
                launch { fetchWeatherData(location.latitude, location.longitude) }

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
                    .url("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$apiKey")
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
                weatherInfo = WeatherInfo(
                    tempCelsius = weatherResp.main.temp.toInt(),
                    humidity = weatherResp.main.humidity,
                    pm10 = airResp?.list?.firstOrNull()?.components?.pm10?.toInt() ?: 0,
                    pm25 = airResp?.list?.firstOrNull()?.components?.pm25?.toInt() ?: 0,
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

            val runsResult = runningRepository.getMyRuns(page = 0, size = 20)
            val statsResult = runningRepository.getRunningStats("weekly")

            if (runsResult is NetworkResult.Success) {
                recentRuns = runsResult.data.content.map { it.toRecentRun() }
            }

            if (statsResult is NetworkResult.Success) {
                val s = statsResult.data
                val pace = if (s.averagePaceSecondsPerKm > 0)
                    "%d'%02d\"".format(s.averagePaceSecondsPerKm / 60, s.averagePaceSecondsPerKm % 60)
                else "--'--\""
                val streakSuffix = if (s.currentStreakDays > 0) " · ${s.currentStreakDays}day streak" else ""
                weeklyStats = WeeklyStats(
                    distanceKm = if (s.totalDistanceMeters < 1000) "%.2f".format(s.totalDistanceMeters / 1000.0)
                                 else "%.1f".format(s.totalDistanceMeters / 1000.0),
                    runs = "${s.totalRuns}$streakSuffix",
                    avgPace = pace,
                    calories = s.totalCaloriesBurned.toString(),
                )
            } else if (runsResult is NetworkResult.Success) {
                weeklyStats = calculateWeeklyStats(runsResult.data.content)
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

        return RecentRun(runId = runId, day = dateLabel, distanceKm = distKm, pace = pace, duration = dur)
    }

    private fun buildGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning,"
            in 12..17 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    // ── OpenWeatherMap response models ────────────────────────────────────

    private data class OWMWeatherResponse(val main: OWMMain)
    private data class OWMMain(val temp: Double, val humidity: Int)

    private data class OWMAirPollutionResponse(val list: List<OWMAirEntry>)
    private data class OWMAirEntry(val components: OWMComponents)
    private data class OWMComponents(
        val pm10: Double,
        @SerializedName("pm2_5") val pm25: Double,
    )
}
