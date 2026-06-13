package com.runway.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.runway.android.ui.components.CoursePreviewSheet
import com.runway.android.ui.components.DiscoverCourseCard
import com.runway.android.ui.components.RecentRunCard
import com.runway.android.ui.components.RunHeroSection
import com.runway.android.ui.components.SectionHeader
import com.runway.android.ui.components.WeeklyStatsCard
import com.runway.android.ui.components.SavedCoursePickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onSetGoal: () -> Unit = {},
    onSeeAllRuns: () -> Unit = {},
    onNavigateToCourseDetail: (String) -> Unit = {},
    onNavigateToMapDetail: (String) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToRunDetail: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.tryLoadNearbyCourses()
        viewModel.tryLoadWeather()

        while (true) {
            delay(WEATHER_REFRESH_INTERVAL_MILLIS)
            viewModel.tryLoadWeather(forceRefresh = true)
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.tryLoadWeather(forceRefresh = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (viewModel.showCoursePicker) {
        SavedCoursePickerSheet(
            courses = viewModel.savedCourses,
            bestTimesByCourseId = viewModel.savedCourseBestTimes,
            isLoading = viewModel.isLoadingSavedCourses,
            error = viewModel.savedCoursesError,
            onDismiss = viewModel::closeCoursePicker,
            onRetry = viewModel::loadSavedCourses,
            onExploreCourses = {
                viewModel.closeCoursePicker()
                onNavigateToDiscover()
            },
            onCourseSelected = { course ->
                viewModel.closeCoursePicker()
                viewModel.selectCoursePreview(course)
            },
        )
    }

    viewModel.coursePreview?.let { course ->
        CoursePreviewSheet(
            course = course,
            routePoints = viewModel.previewPoints,
            bestTimeSeconds = viewModel.previewBestTimeSeconds,
            avgRating = viewModel.previewAvgRating,
            ratingCount = viewModel.previewRatingCount,
            isLoading = viewModel.isLoadingPreview,
            onDismiss = viewModel::dismissCoursePreview,
            onViewMap = { courseId ->
                viewModel.dismissCoursePreview()
                onNavigateToMapDetail(courseId)
            },
            onStartAttempt = { courseId ->
                viewModel.dismissCoursePreview()
                onNavigateToCourseDetail(courseId)
            },
        )
    }

    if (viewModel.showGoalSheet) {
        GoalSetupSheet(
            onDismiss = viewModel::closeGoalSheet,
            onConfirm = { goal ->
                viewModel.setGoal(goal)
                onStartRun()
            },
        )
    }

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val firstScreenHeight = maxHeight

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {

            item {
                RunHeroSection(
                    onStartRun = onStartRun,
                    onSetGoal = viewModel::openGoalSheet,
                    selectedGoal = viewModel.selectedGoal,
                    weatherInfo = viewModel.weatherInfo,
                    currentLocation = viewModel.currentLocation,
                    hasLocationPermission = viewModel.locationPermissionGranted,
                    onSelectCourse = viewModel::openCoursePicker,
                    isVoiceGuideEnabled = viewModel.isVoiceGuideEnabled,
                    onVoiceGuideEnabledChange = viewModel::updateVoiceGuideEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(firstScreenHeight),
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 20.dp),
                ) {
                    WeeklyStatsCard(
                        stats = viewModel.weeklyStats,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }

            item {
                SectionHeader(
                    title = "주변 코스",
                    cta = "전체 보기",
                    onCtaClick = onNavigateToDiscover,
                )
            }
            item {
            when {
                viewModel.isLoadingNearbyCourses -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                viewModel.nearbyCourses.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (viewModel.locationPermissionGranted)
                                "주변 3km 내에 코스가 없어요. 직접 만들어보세요!"
                            else
                                "위치 권한이 필요해요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(viewModel.nearbyCourses) { course ->
                            DiscoverCourseCard(
                                course = course,
                                modifier = Modifier.width(280.dp),
                                onClick = { onNavigateToCourseDetail(course.courseId) },
                            )
                        }
                    }
                }
            }
            }

            // ─── Recent runs: eased is ~1.0 by this point, no graphicsLayer needed ───
            item {
                SectionHeader(
                    title = "최근 러닝",
                    cta = if (!viewModel.isLoadingRuns && viewModel.recentRuns.isNotEmpty()) "전체 보기" else null,
                    onCtaClick = onSeeAllRuns,
                )
            }

            when {
            viewModel.isLoadingRuns -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                    )
                }
            }
            viewModel.recentRuns.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "아직 러닝 기록이 없어요. 첫 러닝을 시작해보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> itemsIndexed(viewModel.recentRuns, key = { _, run -> run.runId }) { index, run ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(run.runId) {
                    delay(index * 60L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(300)),
                ) {
                    RecentRunCard(
                        run = run,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp),
                        onClick = { onNavigateToRunDetail(run.runId) },
                    )
                }
            }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
    } // PullToRefreshBox
}

private const val WEATHER_REFRESH_INTERVAL_MILLIS = 60 * 60 * 1_000L
