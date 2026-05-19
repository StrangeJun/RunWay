package com.runway.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.DiscoverCourseCard
import com.runway.android.ui.components.RecentRunCard
import com.runway.android.ui.components.SectionHeader
import com.runway.android.ui.components.StartRunCard
import com.runway.android.ui.components.WeeklyStatsCard

@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onSeeAllRuns: () -> Unit = {},
    onNavigateToCourseDetail: (String) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.tryLoadNearbyCourses()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // ─── Greeting header ───
        item {
            GreetingHeader(
                greeting = viewModel.greeting,
                nickname = viewModel.nickname,
            )
        }

        // ─── Weekly stats ───
        item {
            WeeklyStatsCard(
                stats = viewModel.weeklyStats,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        // ─── Start Running ───
        item {
            Spacer(modifier = Modifier.height(12.dp))
            StartRunCard(
                onClick = onStartRun,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        // ─── Nearby courses ───
        item {
            SectionHeader(title = "Nearby courses", cta = "See all", onCtaClick = onNavigateToDiscover)
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
                            text = "주변에 코스가 없습니다",
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

        // ─── Recent runs ───
        item {
            SectionHeader(
                title = "Recent runs",
                cta = if (!viewModel.isLoadingRuns && viewModel.recentRuns.isNotEmpty()) "See all" else null,
                onCtaClick = onSeeAllRuns,
            )
        }

        items(viewModel.recentRuns) { run ->
            RecentRunCard(
                run = run,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun GreetingHeader(greeting: String, nickname: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${nickname.ifEmpty { "Runner" }} 👋",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
