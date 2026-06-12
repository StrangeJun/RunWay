package com.runway.android.ui.course.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.map.MapPoint
import com.runway.android.core.share.ShareUtils
import com.runway.android.data.attempt.model.LeaderboardItem
import com.runway.android.data.attempt.model.MyBestAttemptResponse
import com.runway.android.ui.components.RouteMapView
import com.runway.android.ui.theme.OutlineVariantDark
import com.runway.android.ui.theme.SurfaceContainerDark
import com.runway.android.ui.theme.SurfaceContainerHighDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    onBack: () -> Unit,
    onNavigateToAttempt: (courseId: String, courseAttemptId: String, runningRecordId: String) -> Unit,
    onNavigateToLeaderboard: (courseId: String) -> Unit,
    onNavigateToMap: (courseId: String) -> Unit = {},
    viewModel: CourseDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navigateToAttempt.collect { event ->
            onNavigateToAttempt(event.courseId, event.courseAttemptId, event.runningRecordId)
        }
    }

    // 화면으로 돌아올 때 데이터 갱신 (완주 후 내 기록 업데이트 등)
    // 최초 진입 시 ON_RESUME은 init { load() }와 중복되므로 건너뛴다.
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFirstResume by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                } else {
                    viewModel.refresh()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 즐겨찾기 에러 다이얼로그
    if (viewModel.favoriteError != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearFavoriteError,
            title = { Text("즐겨찾기 오류") },
            text = { Text(viewModel.favoriteError!!) },
            confirmButton = {
                TextButton(onClick = viewModel::clearFavoriteError) { Text("확인") }
            },
        )
    }

    // 도전 시작 실패 다이얼로그
    if (viewModel.startAttemptError != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearStartAttemptError,
            title = { Text("도전 시작 실패") },
            text = { Text(viewModel.startAttemptError!!) },
            confirmButton = {
                TextButton(onClick = viewModel::clearStartAttemptError) { Text("확인") }
            },
        )
    }

    // 신고 성공 다이얼로그
    if (viewModel.reportSuccess) {
        AlertDialog(
            onDismissRequest = viewModel::clearReportSuccess,
            title = { Text("신고 접수 완료") },
            text = { Text("신고가 접수되었습니다. 검토 후 조치하겠습니다.") },
            confirmButton = {
                TextButton(onClick = viewModel::clearReportSuccess) { Text("확인") }
            },
        )
    }

    // 평가 성공 다이얼로그
    if (viewModel.ratingSuccess) {
        AlertDialog(
            onDismissRequest = viewModel::clearRatingSuccess,
            title = { Text("평가 완료") },
            text = { Text("평가해 주셔서 감사합니다.") },
            confirmButton = {
                TextButton(onClick = viewModel::clearRatingSuccess) { Text("확인") }
            },
        )
    }

    // 평가 다이얼로그
    if (viewModel.showRateDialog) {
        RateCourseDialog(
            selectedRating = viewModel.ratingValue,
            onRatingChange = viewModel::onRatingValueChange,
            comment = viewModel.ratingComment,
            onCommentChange = viewModel::onRatingCommentChange,
            isSubmitting = viewModel.isSubmittingRating,
            errorMessage = viewModel.ratingError,
            onConfirm = viewModel::submitRating,
            onDismiss = viewModel::dismissRateDialog,
        )
    }

    // 공개 잠금 팝업 — 10회 완주 미달 시
    if (viewModel.showPublishLockedDialog) {
        val completions = viewModel.courseDetail?.completionCount ?: 0
        val remaining = (CourseDetailViewModel.PUBLISH_MIN_COMPLETIONS - completions).coerceAtLeast(0)
        AlertDialog(
            onDismissRequest = viewModel::dismissPublishLockedDialog,
            title = { Text("공개 코스 등록 조건 미달") },
            text = {
                Text(
                    "내가 만든 코스를 ${CourseDetailViewModel.PUBLISH_MIN_COMPLETIONS}회 완주해야 공개 코스로 등록할 수 있습니다.\n\n" +
                    "현재 완주: ${completions}회 / ${CourseDetailViewModel.PUBLISH_MIN_COMPLETIONS}회\n" +
                    "앞으로 ${remaining}회 더 완주하면 공개할 수 있어요.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPublishLockedDialog) { Text("확인") }
            },
        )
    }

    // 발행 성공 다이얼로그
    if (viewModel.publishSuccess) {
        AlertDialog(
            onDismissRequest = viewModel::clearPublishSuccess,
            title = { Text("코스 공개 완료") },
            text = { Text("코스가 공개되었습니다. 다른 러너들이 이 코스에 도전할 수 있습니다.") },
            confirmButton = {
                TextButton(onClick = viewModel::clearPublishSuccess) { Text("확인") }
            },
        )
    }

    // 삭제 확인 다이얼로그
    if (viewModel.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("코스를 삭제할까요?") },
            text = { Text("삭제된 코스는 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteDraftCourse,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) { Text("취소") }
            },
        )
    }

    // 삭제 성공 → 뒤로가기
    LaunchedEffect(viewModel.deleteSuccess) {
        if (viewModel.deleteSuccess) onBack()
    }

    if (viewModel.archiveSuccess) {
        AlertDialog(
            onDismissRequest = viewModel::clearArchiveSuccess,
            title = { Text("코스 보관 완료") },
            text = { Text("코스가 보관되었습니다. 탐색 결과에서 더 이상 표시되지 않습니다.") },
            confirmButton = {
                TextButton(onClick = viewModel::clearArchiveSuccess) { Text("확인") }
            },
        )
    }

    // 보관 확인 다이얼로그
    if (viewModel.showArchiveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissArchiveDialog,
            title = { Text("코스를 보관할까요?") },
            text = {
                Column {
                    Text("보관된 코스는 탐색 결과에 표시되지 않으며, 더 이상 도전을 시작할 수 없습니다.")
                    if (viewModel.archiveError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.archiveError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::submitArchive,
                    enabled = !viewModel.isArchiving,
                ) {
                    if (viewModel.isArchiving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text("보관하기", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissArchiveDialog,
                    enabled = !viewModel.isArchiving,
                ) { Text("취소") }
            },
        )
    }

    // 발행 다이얼로그
    if (viewModel.showPublishDialog) {
        PublishCourseDialog(
            isPublishing = viewModel.isPublishing,
            error = viewModel.publishError,
            onDismiss = viewModel::dismissPublishDialog,
            onPublish = viewModel::submitPublish,
        )
    }

    // 신고 다이얼로그
    if (viewModel.showReportDialog) {
        ReportCourseDialog(
            selectedReason = viewModel.reportReason,
            onReasonChange = viewModel::onReportReasonChange,
            description = viewModel.reportDescription,
            onDescriptionChange = viewModel::onReportDescriptionChange,
            isSubmitting = viewModel.isSubmittingReport,
            errorMessage = viewModel.reportError,
            onConfirm = viewModel::submitReport,
            onDismiss = viewModel::dismissReportDialog,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.courseDetail?.name ?: "코스 상세",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        enabled = !viewModel.isFavoriteToggling,
                    ) {
                        Icon(
                            imageVector = if (viewModel.isFavorited) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (viewModel.isFavorited) "즐겨찾기 해제" else "즐겨찾기 추가",
                            tint = if (viewModel.isFavorited) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            val detail = viewModel.courseDetail
                            if (detail != null) {
                                val distKm = if (detail.distanceMeters >= 1000)
                                    "%.1f km".format(detail.distanceMeters / 1000)
                                else "${detail.distanceMeters.toInt()} m"
                                ShareUtils.shareCourse(
                                    context = context,
                                    name = detail.name,
                                    distanceKm = distKm,
                                    completionCount = detail.completionCount,
                                    isLoop = detail.isLoop,
                                )
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "공유",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::openReportDialog) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = "신고",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = viewModel::pullRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.errorMessage != null && viewModel.courseDetail == null ->
                    ErrorState(
                        message = viewModel.errorMessage!!,
                        onRetry = viewModel::refresh,
                    )
                viewModel.courseDetail != null ->
                    CourseDetailContent(
                        viewModel = viewModel,
                        onNavigateToLeaderboard = onNavigateToLeaderboard,
                        onNavigateToMap = onNavigateToMap,
                    )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
            text = "코스를 불러오지 못했습니다",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
            onClick = onRetry,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = "다시 시도",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun CourseDetailContent(
    viewModel: CourseDetailViewModel,
    onNavigateToLeaderboard: (courseId: String) -> Unit,
    onNavigateToMap: (courseId: String) -> Unit,
) {
    val course = viewModel.courseDetail ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ─── Map header (300dp) with gradient overlay ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clickable { onNavigateToMap(viewModel.courseId) },
        ) {
            RouteMapView(
                points = viewModel.coursePoints.map { MapPoint(it.latitude, it.longitude) },
                modifier = Modifier.fillMaxSize(),
            )
            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
            // "자세히 보기" badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                shape = MaterialTheme.shapes.large,
                color = SurfaceContainerDark.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, OutlineVariantDark),
            ) {
                Text(
                    text = "지도 자세히 보기",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(20.dp))

            // ─── 제목 + 루프 배지 ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (course.isLoop) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Loop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "루프",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ─── 크리에이터 ───
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = course.creator.nickname,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = OutlineVariantDark)
            Spacer(modifier = Modifier.height(20.dp))

            // ─── 통계 카드 (count-up) ───
            var statsTriggered by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { statsTriggered = true }
            val animatedDistance by animateFloatAsState(
                targetValue = if (statsTriggered) course.distanceMeters.toFloat() else 0f,
                animationSpec = tween(1200),
                label = "dist",
            )
            val animatedAttempts by animateIntAsState(
                targetValue = if (statsTriggered) course.attemptCount else 0,
                animationSpec = tween(1200),
                label = "attempts",
            )
            val animatedCompletions by animateIntAsState(
                targetValue = if (statsTriggered) course.completionCount else 0,
                animationSpec = tween(1200),
                label = "completions",
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = SurfaceContainerDark,
                border = BorderStroke(1.dp, OutlineVariantDark),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(label = "거리", value = formatDistance(animatedDistance.toDouble()))
                    StatItem(label = "도전", value = "${animatedAttempts}회")
                    StatItem(label = "완주", value = "${animatedCompletions}회")
                }
            }

            // ─── 평점 행 ───
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    if (course.avgRating != null && (course.ratingCount ?: 0L) > 0) {
                        Text(
                            text = "%.1f".format(course.avgRating),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = " (${course.ratingCount}개)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "아직 평가 없음",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!viewModel.hasRated) {
                    Surface(
                        onClick = viewModel::openRateDialog,
                        shape = MaterialTheme.shapes.extraLarge,
                        color = SurfaceContainerDark,
                        border = BorderStroke(1.dp, OutlineVariantDark),
                    ) {
                        Text(
                            text = "평가하기",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                } else {
                    Text(
                        text = "평가 완료",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            // ─── 설명 ───
            if (!course.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = OutlineVariantDark)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "코스 설명".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = course.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // ─── 코스 메타데이터 ───
            val hasMetadata = course.difficulty != null || course.riskLevel != null ||
                    course.slopeLevel != null || course.surfaceType != null ||
                    course.recommendedTime != null || !course.warnings.isNullOrBlank()
            if (hasMetadata) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = OutlineVariantDark)
                Spacer(modifier = Modifier.height(20.dp))
                CourseMetadataSection(course = course)
            }

            // ─── 초안 상태 + 소유자 → 공개하기 + 삭제 버튼 ───
            if (course.status == "draft" && course.isOwner) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = OutlineVariantDark)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = viewModel::openPublishDialog,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "공개하기",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.errorContainer,
                        onClick = viewModel::openDeleteDialog,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "삭제",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = OutlineVariantDark)
            Spacer(modifier = Modifier.height(20.dp))

            // ─── 리더보드 섹션 ───
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = SurfaceContainerDark,
                border = BorderStroke(1.dp, OutlineVariantDark),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "리더보드".uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            onClick = { onNavigateToLeaderboard(viewModel.courseId) },
                            shape = MaterialTheme.shapes.extraLarge,
                            color = SurfaceContainerHighDark,
                            border = BorderStroke(1.dp, OutlineVariantDark),
                        ) {
                            Text(
                                text = "순위 보기",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── 인라인 Top 5 미리보기 ───
                    when {
                        viewModel.isLoadingLeaderboard -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        viewModel.previewLeaderboard.isEmpty() -> {
                            Text(
                                text = "아직 완주 기록이 없습니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                viewModel.previewLeaderboard.forEach { item ->
                                    LeaderboardPreviewRow(item = item)
                                }
                            }
                        }
                    }
                }
            }

            // ─── 내 기록 섹션 ───
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = OutlineVariantDark)
            Spacer(modifier = Modifier.height(20.dp))
            if (viewModel.myBestAttempt != null) {
                MyBestAttemptSection(best = viewModel.myBestAttempt!!)
            } else if (!viewModel.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "내 기록".uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "아직 이 코스를 완주한 기록이 없어요. 도전해 보세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── 도전 CTA — lime green pill, fixed bottom ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            ),
                        ),
                    )
                    .padding(bottom = 16.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (viewModel.isStartingAttempt)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.primary,
                    onClick = { if (!viewModel.isStartingAttempt) viewModel.startAttempt() },
                ) {
                    val hasCompletions = (viewModel.myBestAttempt?.completionCount ?: 0) > 0
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (viewModel.isStartingAttempt) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "도전 시작 중…",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasCompletions) "다시 도전하기" else "코스 도전하기",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LeaderboardPreviewRow(item: LeaderboardItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "#${item.rank}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.rank <= 3)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(30.dp),
            )
            Text(
                text = item.nickname,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = formatSeconds(item.bestTimeSeconds),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatSeconds(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}

private fun formatDistance(meters: Double): String = when {
    meters >= 1000 -> "%.1f km".format(meters / 1000)
    else -> "${meters.toInt()} m"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseMetadataSection(course: com.runway.android.data.course.model.CourseDetailResponse) {
    Text(
        text = "코스 정보".uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        course.difficulty?.let { MetadataChip(label = difficultyLabel(it)) }
        course.slopeLevel?.let { MetadataChip(label = slopeLabel(it)) }
        course.riskLevel?.let { MetadataChip(label = riskLabel(it)) }
        course.surfaceType?.let { MetadataChip(label = surfaceLabel(it)) }
        course.recommendedTime?.let { MetadataChip(label = timeLabel(it)) }
    }
    if (!course.warnings.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = course.warnings,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun MetadataChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = SurfaceContainerHighDark,
        border = BorderStroke(1.dp, OutlineVariantDark),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun MyBestAttemptSection(best: MyBestAttemptResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "내 기록".uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = SurfaceContainerDark,
        border = BorderStroke(1.dp, OutlineVariantDark),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (best.bestTimeSeconds != null) formatSeconds(best.bestTimeSeconds) else "--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "베스트 기록",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${best.completionCount}회",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "완주 횟수",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun difficultyLabel(v: String) = when (v) { "easy" -> "난이도: 쉬움"; "hard" -> "난이도: 어려움"; else -> "난이도: 보통" }
private fun slopeLabel(v: String) = when (v) { "flat" -> "경사: 평탄"; "steep" -> "경사: 가파름"; else -> "경사: 완만" }
private fun riskLabel(v: String) = when (v) { "low" -> "위험도: 낮음"; "high" -> "위험도: 높음"; else -> "위험도: 보통" }
private fun surfaceLabel(v: String) = when (v) { "road" -> "도로"; "park" -> "공원"; "trail" -> "트레일"; else -> "혼합" }
private fun timeLabel(v: String) = when (v) { "morning" -> "추천: 아침"; "day" -> "추천: 낮"; "night" -> "추천: 저녁"; else -> "추천: 무관" }
