package com.runway.android.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.SystemClock
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
import com.runway.android.BuildConfig
import com.runway.android.core.datastore.VoiceGuideDataStore
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.attempt.model.MyBestAttemptResponse
import com.runway.android.data.course.model.GeoPoint
import com.runway.android.data.course.model.NearbyCourseItem
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.domain.attempt.CourseAttemptRepository
import com.runway.android.data.running.model.RunSummaryResponse
import com.runway.android.data.running.model.RunningStatsResponse
import com.runway.android.domain.course.CourseRepository
import com.runway.android.domain.running.RunningRepository
import com.runway.android.domain.training.TrainingRecommendation
import com.runway.android.domain.training.TrainingRecommendationEngine
import com.runway.android.domain.training.TrainingRunSample
import com.runway.android.domain.user.UserRepository
import com.runway.android.core.map.MapPoint
import com.runway.android.ui.components.RecentRun
import com.runway.android.ui.components.WeatherInfo
import com.runway.android.ui.components.WeeklyStats
import com.runway.android.core.util.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
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
    private val attemptRepository: CourseAttemptRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val voiceGuideDataStore: VoiceGuideDataStore,
    @Named("noAuth") private val okHttpClient: OkHttpClient,
) : ViewModel() {
    private val airKoreaClient = AirKoreaClient(okHttpClient)
    private val kmaWeatherClient = KmaWeatherClient(okHttpClient)
    private var weatherLoadJob: Job? = null
    private var lastWeatherUpdatedAt = 0L
    private var trainingRunSamples = emptyList<TrainingRunSample>()

    val greeting: String = buildGreeting()

    var nickname by mutableStateOf("")
        private set
    var weeklyStats by mutableStateOf(WeeklyStats("--", "--", "--'--\"", "--"))
        private set
    var recentRuns by mutableStateOf<List<RecentRun>>(emptyList())
        private set
    var trainingRecommendation by mutableStateOf(
        TrainingRecommendationEngine.calculate(emptyList()),
    )
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
    var isVoiceGuideEnabled by mutableStateOf(true)
        private set
    var savedCourses by mutableStateOf<List<CourseResponse>>(emptyList())
        private set
    var savedCourseBestTimes by mutableStateOf<Map<String, Int?>>(emptyMap())
        private set
    var isLoadingSavedCourses by mutableStateOf(false)
        private set
    var savedCoursesError by mutableStateOf<String?>(null)
        private set
    var showCoursePicker by mutableStateOf(false)
        private set
    var selectedGoal by mutableStateOf<RunGoal?>(null)
        private set
    var showGoalSheet by mutableStateOf(false)

    var coursePreview by mutableStateOf<CourseResponse?>(null)
        private set
    var previewPoints by mutableStateOf<List<GeoPoint>>(emptyList())
        private set
    var previewBestTimeSeconds by mutableStateOf<Int?>(null)
        private set
    var previewAvgRating by mutableStateOf<Double?>(null)
        private set
    var previewRatingCount by mutableStateOf<Long?>(null)
        private set
    var isLoadingPreview by mutableStateOf(false)
        private set

    val locationPermissionGranted: Boolean
        get() = hasLocationPermission()

    init {
        viewModelScope.launch {
            voiceGuideDataStore.enabledFlow.collect { isVoiceGuideEnabled = it }
        }
    }

    fun updateVoiceGuideEnabled(enabled: Boolean) {
        isVoiceGuideEnabled = enabled
        viewModelScope.launch { voiceGuideDataStore.setEnabled(enabled) }
    }

    fun openCoursePicker() {
        showCoursePicker = true
        loadSavedCourses()
    }

    fun closeCoursePicker() {
        showCoursePicker = false
    }

    fun selectCoursePreview(course: CourseResponse) {
        coursePreview = course
        previewPoints = emptyList()
        previewBestTimeSeconds = null
        previewAvgRating = null
        previewRatingCount = null
        isLoadingPreview = true
        viewModelScope.launch {
            val j1 = launch {
                when (val r = courseRepository.getCoursePoints(course.courseId)) {
                    is NetworkResult.Success -> previewPoints = r.data.points.map { GeoPoint(it.latitude, it.longitude) }
                    else -> {}
                }
            }
            val j2 = launch {
                when (val r = attemptRepository.getMyBestAttempt(course.courseId)) {
                    is NetworkResult.Success -> previewBestTimeSeconds = r.data.bestTimeSeconds
                    else -> {}
                }
            }
            val j3 = launch {
                when (val r = courseRepository.getCourseDetail(course.courseId)) {
                    is NetworkResult.Success -> {
                        previewAvgRating = r.data.avgRating
                        previewRatingCount = r.data.ratingCount
                    }
                    else -> {}
                }
            }
            j1.join(); j2.join(); j3.join()
            isLoadingPreview = false
        }
    }

    fun dismissCoursePreview() {
        coursePreview = null
        previewPoints = emptyList()
    }

    fun loadSavedCourses() {
        viewModelScope.launch {
            isLoadingSavedCourses = true
            savedCoursesError = null
            val favoritesResult = courseRepository.getFavoriteCourses(size = 50)
            val participatedResult = courseRepository.getParticipatedCourses(size = 100)

            when (favoritesResult) {
                is NetworkResult.Success -> savedCourses = favoritesResult.data.content
                is NetworkResult.ApiError -> savedCoursesError = favoritesResult.message
                is NetworkResult.NetworkError -> savedCoursesError = "네트워크 연결을 확인해 주세요."
            }
            savedCourseBestTimes = if (participatedResult is NetworkResult.Success) {
                participatedResult.data.content.associate {
                    it.courseId to it.bestTimeSecondsByMe
                }
            } else {
                emptyMap()
            }
            isLoadingSavedCourses = false
        }
    }

    fun openGoalSheet()  { showGoalSheet = true }
    fun closeGoalSheet() { showGoalSheet = false }

    fun removeRecentRun(runId: String) {
        recentRuns = recentRuns.filterNot { it.runId == runId }
        trainingRunSamples = trainingRunSamples.filterNot { it.runId == runId }
        trainingRecommendation = TrainingRecommendationEngine.calculate(trainingRunSamples)
    }

    var isRefreshing by mutableStateOf(false)
        private set

    fun reloadRecentRuns() {
        viewModelScope.launch {
            val result = runningRepository.getMyRuns(page = 0, size = HOME_RUN_FETCH_SIZE)
            if (result is NetworkResult.Success) {
                recentRuns = result.data.content.take(RECENT_RUN_COUNT).map { it.toRecentRun() }
                updateTrainingRecommendation(result.data.content)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            nearbyCourses = emptyList()
            // Inline the essential awaits to correctly track completion
            val runsResult = runningRepository.getMyRuns(page = 0, size = HOME_RUN_FETCH_SIZE)
            if (runsResult is NetworkResult.Success) {
                recentRuns = runsResult.data.content.take(RECENT_RUN_COUNT).map { it.toRecentRun() }
                updateTrainingRecommendation(runsResult.data.content)
            }
            val statsResult = runningRepository.getRunningStats("weekly")
            weeklyStats = resolveWeeklyStats(runsResult, statsResult)
            loadNearbyCoursesIfPermitted()
            weatherLoadJob?.cancelAndJoin()
            weatherLoadJob = null
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

    fun tryLoadWeather(forceRefresh: Boolean = false) {
        val isFresh = weatherInfo != null &&
            SystemClock.elapsedRealtime() - lastWeatherUpdatedAt < WEATHER_REFRESH_INTERVAL_MILLIS
        if (!forceRefresh && isFresh) return
        if (isRefreshing) return
        if (weatherLoadJob?.isActive == true) return

        weatherLoadJob = viewModelScope.launch {
            try {
                loadWeatherIfPermitted()
            } finally {
                weatherLoadJob = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun loadWeatherIfPermitted() {
        if (!hasLocationPermission()) return
        try {
            val cts = CancellationTokenSource()
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0)
                .setDurationMillis(12_000)
                .build()
            val location = fusedLocationClient
                .getCurrentLocation(request, cts.token)
                .await() ?: return
            currentLocation = MapPoint(location.latitude, location.longitude)
            if (fetchWeatherData(location.latitude, location.longitude)) {
                lastWeatherUpdatedAt = SystemClock.elapsedRealtime()
            }
        } catch (_: Exception) { }
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

    private suspend fun fetchWeatherData(lat: Double, lon: Double): Boolean {
        if (BuildConfig.KMA_API_KEY.isBlank()) return false

        return try {
            val weatherResp = withContext(Dispatchers.IO) {
                kmaWeatherClient.getCurrentWeather(
                    latitude = lat,
                    longitude = lon,
                    serviceKey = BuildConfig.KMA_API_KEY,
                )
            }

            val airKoreaResp = withContext(Dispatchers.IO) {
                airKoreaClient.getAirQuality(
                    latitude = lat,
                    longitude = lon,
                    serviceKey = BuildConfig.AIRKOREA_API_KEY,
                    stationCandidates = resolveAirKoreaStationCandidates(lat, lon),
                )
            }

            if (weatherResp != null) {
                weatherInfo = WeatherInfo(
                    tempCelsius = weatherResp.temperatureCelsius,
                    humidity = weatherResp.humidity,
                    pm10 = airKoreaResp?.pm10 ?: 0,
                    pm25 = airKoreaResp?.pm25 ?: 0,
                    condition = weatherResp.condition,
                )
                true
            } else {
                false
            }
        } catch (_: Exception) {
            // silent fail — weather is non-critical
            false
        }
    }


    @Suppress("DEPRECATION")
    private fun resolveAirKoreaStationCandidates(
        latitude: Double,
        longitude: Double,
    ): List<String> {
        if (!Geocoder.isPresent()) return emptyList()
        val address = runCatching {
            Geocoder(context, Locale.KOREA)
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
        }.getOrNull() ?: return emptyList()

        return listOfNotNull(
            address.subLocality,
            address.subAdminArea,
            address.locality,
        ).map(String::trim).filter(String::isNotBlank).distinct()
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
                updateTrainingRecommendation(runsResult.data.content)
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

    private fun updateTrainingRecommendation(runs: List<RunSummaryResponse>) {
        trainingRunSamples = runs.mapNotNull { run ->
            val startedAt = runCatching { Instant.parse(run.startedAt) }.getOrNull()
                ?: return@mapNotNull null
            TrainingRunSample(
                runId = run.runId,
                status = run.status,
                startedAt = startedAt,
                distanceMeters = (run.distanceMeters ?: 0.0).coerceAtLeast(0.0),
                durationSeconds = (run.durationSeconds ?: 0).coerceAtLeast(0),
            )
        }
        trainingRecommendation = TrainingRecommendationEngine.calculate(trainingRunSamples)
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

    private companion object {
        const val HOME_RUN_FETCH_SIZE = 200
        const val RECENT_RUN_COUNT = 20
        const val WEATHER_REFRESH_INTERVAL_MILLIS = 60 * 60 * 1_000L
    }
}
