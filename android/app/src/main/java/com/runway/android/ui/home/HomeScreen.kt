package com.runway.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.DiscoverCourseCard
import com.runway.android.ui.components.HomeWeatherCard
import com.runway.android.ui.components.RecentRunCard
import com.runway.android.ui.components.RunHeroSection
import com.runway.android.ui.components.SectionHeader
import com.runway.android.ui.components.WeeklyStatsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onSetGoal: () -> Unit = {},
    onSeeAllRuns: () -> Unit = {},
    onNavigateToCourseDetail: (String) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToRunDetail: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.tryLoadNearbyCourses()
        viewModel.tryLoadWeather()
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

    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // smoothstep easing: 0f = hero fully visible, 1f = hero scrolled / content revealed
    // fade distance = 380dp for a gradual, cinematic transition
    val eased by remember {
        derivedStateOf {
            val raw = if (listState.firstVisibleItemIndex > 0) 1f
            else {
                val fadePx = with(density) { 380.dp.toPx() }
                (listState.firstVisibleItemScrollOffset / fadePx).coerceIn(0f, 1f)
            }
            // smoothstep: 3t² - 2t³  — slow start, smooth middle, slow end
            raw * raw * (3f - 2f * raw)
        }
    }

    // Content slide-up distance (pixels) — items translate from below as they fade in
    val slideDistPx = with(density) { 28.dp.toPx() }

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {

        // ─── Run Hero: fades out as user scrolls ───
        item {
            RunHeroSection(
                onStartRun = onStartRun,
                onSetGoal = viewModel::openGoalSheet,
                selectedGoal = viewModel.selectedGoal,
                weatherInfo = viewModel.weatherInfo,
                currentLocation = viewModel.currentLocation,
                hasLocationPermission = viewModel.locationPermissionGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(1.08f)
                    .graphicsLayer { alpha = 1f - eased },
            )
        }

        if (viewModel.weatherInfo != null) {
            item {
                HomeWeatherCard(
                    weather = viewModel.weatherInfo!!,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp),
                )
            }
        }

        // ─── Weekly stats: fades in + slides up ───
        item {
            SectionHeader(
                title = "이번 주",
                modifier = Modifier.graphicsLayer {
                    alpha = eased
                    translationY = (1f - eased) * slideDistPx
                },
            )
        }
        item {
            WeeklyStatsCard(
                stats = viewModel.weeklyStats,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = eased
                        translationY = (1f - eased) * slideDistPx
                    },
            )
        }

        // ─── Nearby courses: fades in + slides up ───
        item {
            SectionHeader(
                title = "주변 코스",
                cta = "전체 보기",
                onCtaClick = onNavigateToDiscover,
                modifier = Modifier.graphicsLayer {
                    alpha = eased
                    translationY = (1f - eased) * slideDistPx
                },
            )
        }
        item {
            when {
                viewModel.isLoadingNearbyCourses -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .graphicsLayer {
                                alpha = eased
                                translationY = (1f - eased) * slideDistPx
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                viewModel.nearbyCourses.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .graphicsLayer {
                                alpha = eased
                                translationY = (1f - eased) * slideDistPx
                            },
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
                        modifier = Modifier.graphicsLayer {
                            alpha = eased
                            translationY = (1f - eased) * slideDistPx
                        },
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
    } // PullToRefreshBox
}
