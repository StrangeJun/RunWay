package com.runway.android.ui.leaderboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.data.attempt.model.LeaderboardItem
import com.runway.android.data.attempt.model.LeaderboardResponse
import com.runway.android.ui.course.detail.RateCourseDialog
import com.runway.android.ui.theme.OutlineVariantDark
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseLeaderboardScreen(
    onBack: () -> Unit,
    viewModel: CourseLeaderboardViewModel = hiltViewModel(),
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column {
            // ─── Fixed header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로 가기",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = "리더보드",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "새로고침",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = OutlineVariantDark)

            // ─── Body ────────────────────────────────────────────────────────
            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    viewModel.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    viewModel.errorMessage != null -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "리더보드를 불러오지 못했습니다",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            onClick = viewModel::refresh,
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

                    viewModel.leaderboard != null -> LeaderboardContent(
                        leaderboard = viewModel.leaderboard!!,
                        sortBy = viewModel.sortBy,
                        onSortChange = viewModel::updateSortBy,
                        isPR = viewModel.isPR,
                        previousBestSeconds = viewModel.previousBestSeconds,
                        improvementSeconds = viewModel.improvementSeconds,
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardContent(
    leaderboard: LeaderboardResponse,
    sortBy: String,
    onSortChange: (String) -> Unit,
    isPR: Boolean = false,
    previousBestSeconds: Int? = null,
    improvementSeconds: Int? = null,
) {
    val items = leaderboard.items

    // PR/첫 완주 배너 애니메이션
    val isFirstCompletion = isPR && previousBestSeconds == null
    val showBanner = isFirstCompletion ||
        (isPR && previousBestSeconds != null) ||
        (!isPR && improvementSeconds != null && previousBestSeconds != null && improvementSeconds <= 0)
    var bannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { bannerVisible = true }

    var shimmerTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(isPR) {
        if (isPR) {
            kotlinx.coroutines.delay(400)
            shimmerTriggered = true
        }
    }
    val shimmerProgress by animateFloatAsState(
        targetValue = if (shimmerTriggered) 1f else 0f,
        animationSpec = tween(800),
        label = "prShimmer",
    )

    val tabs = listOf("fastest_time" to "빠른 시간 순", "most_completions" to "완주 횟수 순")
    val selectedIndex = tabs.indexOfFirst { it.first == sortBy }.coerceAtLeast(0)

    Column {
        // ─── 완주 결과 배너 ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = bannerVisible && showBanner,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(350),
            ) + fadeIn(tween(350)),
        ) {
            when {
                isFirstCompletion -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                drawContent()
                                val center = size.width * (shimmerProgress * 2.5f - 0.5f)
                                val halfWidth = size.width * 0.35f
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.28f),
                                            Color.Transparent,
                                        ),
                                        startX = center - halfWidth,
                                        endX = center + halfWidth,
                                    ),
                                    topLeft = Offset.Zero,
                                    size = size,
                                )
                            },
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column {
                                    Text(
                                        text = "첫 완주!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Text(
                                        text = "이 코스를 처음 완주했습니다. 대단해요!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                    )
                                }
                            }
                        }
                    }
                }
                isPR && previousBestSeconds != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                drawContent()
                                val center = size.width * (shimmerProgress * 2.5f - 0.5f)
                                val halfWidth = size.width * 0.35f
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.28f),
                                            Color.Transparent,
                                        ),
                                        startX = center - halfWidth,
                                        endX = center + halfWidth,
                                    ),
                                    topLeft = Offset.Zero,
                                    size = size,
                                )
                            },
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column {
                                    Text(
                                        text = "코스 신기록!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    if (improvementSeconds != null && improvementSeconds > 0) {
                                        Text(
                                            text = "이전 기록보다 ${formatTime(improvementSeconds)} 빠름 (이전: ${formatTime(previousBestSeconds)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                !isPR && improvementSeconds != null && previousBestSeconds != null && improvementSeconds <= 0 -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                if (improvementSeconds == 0) {
                                    Text(
                                        text = "지난번과 동일한 기록이에요!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "최고 기록: ${formatTime(previousBestSeconds)} · 다음엔 더 빠르게 달려보세요!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    )
                                } else {
                                    val regressionSeconds = -improvementSeconds
                                    Text(
                                        text = "지난번보다 ${formatTime(regressionSeconds)} 느림",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "최고 기록: ${formatTime(previousBestSeconds)} · 다음엔 더 잘 달릴 수 있어요!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── 탭 ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp),
        ) {
            tabs.forEachIndexed { index, (key, label) ->
                val isSelected = selectedIndex == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSortChange(key) }
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(1.dp),
                                ),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = OutlineVariantDark.copy(alpha = 0.4f))

        // ─── 내 순위 배너 ────────────────────────────────────────────────────
        leaderboard.myRank?.let { rank ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "내 순위",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "${rank}위",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            HorizontalDivider(color = OutlineVariantDark.copy(alpha = 0.3f))
        }

        // ─── 리스트 ──────────────────────────────────────────────────────────
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "아직 완주 기록이 없습니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "이 코스의 첫 번째 완주자가 되어보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                // ─── 시상대 (top 3) ──────────────────────────────────────────
                if (items.size >= 2) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        PodiumSection(
                            items = items.take(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = OutlineVariantDark.copy(alpha = 0.3f))
                    }
                }

                // ─── 나머지 순위 (4위~) ─────────────────────────────────────
                itemsIndexed(if (items.size >= 2) items.drop(3) else items) { index, item ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(item.userId) {
                        delay(index.coerceAtMost(6) * 60L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
                    ) {
                        LeaderboardListRow(
                            item = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── 시상대 섹션 ──────────────────────────────────────────────────────────────

@Composable
private fun PodiumSection(items: List<LeaderboardItem>, modifier: Modifier = Modifier) {
    // Display order: 2위(left) | 1위(center) | 3위(right)
    val rank2 = items.getOrNull(1)
    val rank1 = items.getOrNull(0)
    val rank3 = items.getOrNull(2)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        // 2위 (왼쪽, 중간 높이)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            if (rank2 != null) {
                PodiumSlot(
                    item = rank2,
                    podiumHeight = 120.dp,
                    avatarSize = 48.dp,
                    isFirst = false,
                    showCrown = false,
                )
            }
        }

        // 1위 (가운데, 가장 높음)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            if (rank1 != null) {
                PodiumSlot(
                    item = rank1,
                    podiumHeight = 160.dp,
                    avatarSize = 56.dp,
                    isFirst = true,
                    showCrown = true,
                )
            }
        }

        // 3위 (오른쪽, 가장 낮음)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            if (rank3 != null) {
                PodiumSlot(
                    item = rank3,
                    podiumHeight = 96.dp,
                    avatarSize = 44.dp,
                    isFirst = false,
                    showCrown = false,
                )
            }
        }
    }
}

@Composable
private fun PodiumSlot(
    item: LeaderboardItem,
    podiumHeight: Dp,
    avatarSize: Dp,
    isFirst: Boolean,
    showCrown: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surfaceContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // 왕관 (1위만)
        if (showCrown) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 아바타 원
        val avatarModifier = if (isFirst) {
            Modifier
                .size(avatarSize)
                .border(BorderStroke(2.dp, primary), CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(primary)
        } else {
            Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(surfaceContainer)
        }
        Box(
            modifier = avatarModifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFirst) onPrimary else onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 닉네임
        Text(
            text = item.nickname,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 기록
        Text(
            text = formatTime(item.bestTimeSeconds),
            style = MaterialTheme.typography.labelMedium,
            color = primary,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 시상대 블록
        val podiumModifier = if (isFirst) {
            Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(surfaceContainer)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.6f), primary.copy(alpha = 0.1f)),
                    ),
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                )
        } else {
            Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(surfaceContainer)
        }

        Box(
            modifier = podiumModifier,
            contentAlignment = Alignment.TopCenter,
        ) {
            // 순위 배지
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .background(
                        color = if (isFirst) primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${item.rank}위",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isFirst) primary else onSurfaceVariant,
                )
            }
        }
    }
}

// ─── 4위 이후 행 ─────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardListRow(item: LeaderboardItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = OutlineVariantDark.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 순위
        Text(
            text = "${item.rank}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))

        // 아바타
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        // 닉네임 + 완주 횟수
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nickname,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = "완주 ${item.completionCount}회",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 기록
        Text(
            text = formatTime(item.bestTimeSeconds),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ─── 시간 포맷 유틸 ──────────────────────────────────────────────────────────

private fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}
