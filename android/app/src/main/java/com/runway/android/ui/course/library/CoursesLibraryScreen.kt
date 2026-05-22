package com.runway.android.ui.course.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.ParticipatedCourseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesLibraryScreen(
    onNavigateToCourseDetail: (String) -> Unit = {},
    viewModel: CoursesLibraryViewModel = hiltViewModel(),
) {
    val tabs = listOf("만든 코스", "즐겨찾기", "참여한 코스")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadFavoriteCourses()
                viewModel.loadMyCourses()
                viewModel.loadParticipatedCourses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Text(
            text = "내 코스",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (selectedTabIndex) {
                0 -> CreatedCoursesTab(
                    courses = viewModel.myCourses,
                    isLoading = viewModel.isLoadingMyCourses,
                    error = viewModel.myCoursesError,
                    onRetry = viewModel::loadMyCourses,
                    onCourseClick = onNavigateToCourseDetail,
                )
                1 -> FavoriteCoursesTab(
                    courses = viewModel.favoriteCourses,
                    isLoading = viewModel.isLoadingFavorites,
                    error = viewModel.favoritesError,
                    onRetry = viewModel::loadFavoriteCourses,
                    onCourseClick = onNavigateToCourseDetail,
                )
                2 -> ParticipatedCoursesTab(
                    courses = viewModel.participatedCourses,
                    isLoading = viewModel.isLoadingParticipated,
                    error = viewModel.participatedError,
                    onRetry = viewModel::loadParticipatedCourses,
                    onCourseClick = onNavigateToCourseDetail,
                )
                else -> {}
            }
        }
    }
}

// ─── 만든 코스 탭 ───

@Composable
private fun CreatedCoursesTab(
    courses: List<CourseResponse>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onCourseClick: (String) -> Unit,
) {
    TabContent(isLoading = isLoading, error = error, onRetry = onRetry) {
        if (courses.isEmpty()) {
            EmptyState("아직 만든 코스가 없습니다.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = { it.courseId }) { course ->
                    CreatedCourseCard(course = course, onClick = { onCourseClick(course.courseId) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun CreatedCourseCard(course: CourseResponse, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(status = course.status)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CourseStatItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = formatDistance(course.distanceMeters),
                )
                if (course.isLoop) {
                    CourseStatItem(
                        icon = { Icon(Icons.Filled.Loop, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        text = "루프",
                    )
                }
                CourseStatItem(
                    icon = { Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = "완주 ${course.completionCount}회",
                )
            }
        }
    }
}

// ─── 즐겨찾기 탭 ───

@Composable
private fun FavoriteCoursesTab(
    courses: List<CourseResponse>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onCourseClick: (String) -> Unit,
) {
    TabContent(isLoading = isLoading, error = error, onRetry = onRetry) {
        if (courses.isEmpty()) {
            EmptyState("즐겨찾기한 코스가 없습니다.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = { it.courseId }) { course ->
                    FavoriteCourseCard(course = course, onClick = { onCourseClick(course.courseId) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun FavoriteCourseCard(course: CourseResponse, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CourseStatItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = formatDistance(course.distanceMeters),
                )
                if (course.isLoop) {
                    CourseStatItem(
                        icon = { Icon(Icons.Filled.Loop, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        text = "루프",
                    )
                }
                CourseStatItem(
                    icon = { Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = "완주 ${course.completionCount}회",
                )
            }
        }
    }
}

// ─── 참여한 코스 탭 ───

@Composable
private fun ParticipatedCoursesTab(
    courses: List<ParticipatedCourseItem>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onCourseClick: (String) -> Unit,
) {
    TabContent(isLoading = isLoading, error = error, onRetry = onRetry) {
        if (courses.isEmpty()) {
            EmptyState("아직 참여한 코스가 없습니다.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = { it.courseId }) { course ->
                    ParticipatedCourseCard(course = course, onClick = { onCourseClick(course.courseId) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun ParticipatedCourseCard(course: ParticipatedCourseItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CourseStatItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = formatDistance(course.distanceMeters),
                )
                if (course.isLoop) {
                    CourseStatItem(
                        icon = { Icon(Icons.Filled.Loop, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                        text = "루프",
                    )
                }
                CourseStatItem(
                    icon = { Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                    text = "완주 ${course.completionCountByMe}/${course.attemptCountByMe}회",
                )
                course.bestTimeSecondsByMe?.let { secs ->
                    CourseStatItem(
                        icon = { Icon(Icons.Filled.Timer, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = formatDuration(secs),
                    )
                }
            }
        }
    }
}

// ─── 공통 컴포넌트 ───

@Composable
private fun TabContent(
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            error != null -> ErrorState(message = error, onRetry = onRetry)
            else -> content()
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = onRetry,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = "다시 시도",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "published" -> "공개" to MaterialTheme.colorScheme.primary
        "draft" -> "초안" to MaterialTheme.colorScheme.onSurfaceVariant
        "archived" -> "보관" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun CourseStatItem(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000.0) else "${meters.toInt()} m"

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
