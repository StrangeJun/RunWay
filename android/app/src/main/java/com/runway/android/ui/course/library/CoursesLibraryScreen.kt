package com.runway.android.ui.course.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.data.course.model.ParticipatedCourseItem
import com.runway.android.ui.theme.OutlineVariantDark
import com.runway.android.ui.theme.SurfaceContainerDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesLibraryScreen(
    onNavigateToCourseDetail: (String) -> Unit = {},
    viewModel: CoursesLibraryViewModel = hiltViewModel(),
) {
    val tabs = listOf("만든 코스", "즐겨찾기", "참여한 코스")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // ON_RESUME 마다 항상 재로드:
    // - 탭 전환 시 컴포저블이 새로 생성되면 라이프사이클이 이미 RESUMED이므로
    //   observer 등록 직후 ON_RESUME이 즉시 발생 → reload ✓
    // - 코스 상세에서 돌아올 때도 ON_RESUME 발생 → reload ✓
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
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
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold
                                         else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
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
                }
            }
        }
    }
}

// ─── 만든 코스 탭 ─────────────────────────────────────────────────────────────

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
            EmptyState(
                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null,
                    modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                message = "아직 만든 코스가 없습니다.",
                sub = "러닝을 완료한 후 코스를 등록해 보세요.",
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = { it.courseId }) { course ->
                    CreatedCourseCard(course = course, onClick = { onCourseClick(course.courseId) })
                }
            }
        }
    }
}

@Composable
private fun CreatedCourseCard(course: CourseResponse, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, OutlineVariantDark, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = SurfaceContainerDark,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 거리 원형 뱃지
            DistanceBadge(meters = course.distanceMeters)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    StatusBadge(status = course.status)
                }
                if (!course.description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (course.isLoop) {
                        StatChip(
                            icon = { Icon(Icons.Filled.Loop, null, Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary) },
                            text = "루프",
                        )
                    }
                    StatChip(
                        icon = { Icon(Icons.Filled.TrendingUp, null, Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = "도전 ${course.attemptCount}회",
                    )
                    StatChip(
                        icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = "완주 ${course.completionCount}회",
                    )
                }
            }
        }
    }
}

// ─── 즐겨찾기 탭 ──────────────────────────────────────────────────────────────

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
            EmptyState(
                icon = { Icon(Icons.Filled.BookmarkBorder, null,
                    modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                message = "즐겨찾기한 코스가 없습니다.",
                sub = "코스 상세 화면에서 즐겨찾기를 추가해 보세요.",
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = { it.courseId }) { course ->
                    FavoriteCourseCard(course = course, onClick = { onCourseClick(course.courseId) })
                }
            }
        }
    }
}

@Composable
private fun FavoriteCourseCard(course: CourseResponse, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, OutlineVariantDark, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = SurfaceContainerDark,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DistanceBadge(meters = course.distanceMeters)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!course.description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (course.isLoop) {
                        StatChip(
                            icon = { Icon(Icons.Filled.Loop, null, Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary) },
                            text = "루프",
                        )
                    }
                    StatChip(
                        icon = { Icon(Icons.Filled.TrendingUp, null, Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = "완주 ${course.completionCount}회",
                    )
                }
            }
        }
    }
}

// ─── 참여한 코스 탭 ───────────────────────────────────────────────────────────

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
            EmptyState(
                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null,
                    modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                message = "아직 참여한 코스가 없습니다.",
                sub = "코스 탐색에서 도전할 코스를 찾아보세요.",
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = { it.courseId }) { course ->
                    ParticipatedCourseCard(course = course, onClick = { onCourseClick(course.courseId) })
                }
            }
        }
    }
}

@Composable
private fun ParticipatedCourseCard(course: ParticipatedCourseItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, OutlineVariantDark, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = SurfaceContainerDark,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DistanceBadge(meters = course.distanceMeters)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!course.description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (course.isLoop) {
                        StatChip(
                            icon = { Icon(Icons.Filled.Loop, null, Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary) },
                            text = "루프",
                        )
                    }
                    StatChip(
                        icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        text = "완주 ${course.completionCountByMe}/${course.attemptCountByMe}회",
                    )
                    course.bestTimeSecondsByMe?.let { secs ->
                        StatChip(
                            icon = { Icon(Icons.Filled.Timer, null, Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary) },
                            text = formatDuration(secs),
                        )
                    }
                }
            }
        }
    }
}

// ─── 공통 컴포넌트 ─────────────────────────────────────────────────────────────

@Composable
private fun DistanceBadge(meters: Double) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val (value, unit) = if (meters >= 1000)
                "%.1f".format(meters / 1000.0) to "km"
            else
                "${meters.toInt()}" to "m"
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun StatChip(icon: @Composable () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            .background(color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

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
private fun EmptyState(
    icon: @Composable () -> Unit,
    message: String,
    sub: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(text = sub, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Surface(onClick = onRetry, shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary) {
            Text(text = "다시 시도", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        }
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
