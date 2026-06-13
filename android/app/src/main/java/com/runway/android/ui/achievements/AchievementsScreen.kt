package com.runway.android.ui.achievements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.AchievementCard
import com.runway.android.ui.theme.DisplayFontFamily

private val AchievementBackground = Color(0xFF080C0B)
private val AchievementCardColor = Color(0xFF0B100E)
private val AchievementGrid = Color(0xFF173028)
private val AchievementGreen = Color(0xFFB8FF00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AchievementBackground),
    ) {
        AchievementsBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            AchievementHeader(onBack = onBack)

            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    viewModel.isLoading -> LoadingState()
                    viewModel.errorMessage != null -> ErrorState(
                        message = viewModel.errorMessage!!,
                        onRetry = viewModel::retry,
                    )
                    viewModel.items.isEmpty() -> EmptyState()
                    else -> {
                        val unlocked = viewModel.items.filter { it.unlocked }
                        val locked = viewModel.items.filterNot { it.unlocked }

                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 36.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                AchievementSummary(
                                    unlockedCount = unlocked.size,
                                    totalCount = viewModel.items.size,
                                )
                                Spacer(Modifier.height(10.dp))
                            }

                            if (unlocked.isNotEmpty()) {
                                item {
                                    AchievementSectionTitle(
                                        title = "COMPLETED",
                                        count = unlocked.size,
                                        highlighted = true,
                                    )
                                }
                                items(unlocked, key = { it.code }) { item ->
                                    AchievementCard(item = item)
                                }
                            }

                            if (locked.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(6.dp))
                                    AchievementSectionTitle(
                                        title = "IN PROGRESS",
                                        count = locked.size,
                                        highlighted = false,
                                    )
                                }
                                items(locked, key = { it.code }) { item ->
                                    AchievementCard(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                tint = AchievementGreen,
            )
        }
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = "ACHIEVEMENTS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = DisplayFontFamily,
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = "달릴수록 쌓이는 나의 기록",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.42f),
            )
        }
    }
}

@Composable
private fun AchievementSummary(unlockedCount: Int, totalCount: Int) {
    val progress = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        AchievementGreen.copy(alpha = 0.75f),
                        AchievementGreen.copy(alpha = 0.18f),
                        AchievementGreen.copy(alpha = 0.06f),
                    ),
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = AchievementCardColor.copy(alpha = 0.96f),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(AchievementGreen.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, AchievementGreen.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(52.dp),
                    color = AchievementGreen,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    strokeWidth = 4.dp,
                )
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(25.dp),
                    tint = AchievementGreen,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "YOUR PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = AchievementGreen.copy(alpha = 0.72f),
                    letterSpacing = 1.3.sp,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$unlockedCount",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = DisplayFontFamily,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = " / $totalCount 달성",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.48f),
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}% COMPLETE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AchievementGreen,
                    letterSpacing = 0.8.sp,
                )
            }
        }
    }
}

@Composable
private fun AchievementSectionTitle(title: String, count: Int, highlighted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(
                    if (highlighted) AchievementGreen else Color.White.copy(alpha = 0.22f),
                    RoundedCornerShape(2.dp),
                ),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) AchievementGreen else Color.White.copy(alpha = 0.5f),
            letterSpacing = 1.4.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = count.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = DisplayFontFamily),
            color = Color.White.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AchievementGreen)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.62f),
            )
            TextButton(onClick = onRetry) {
                Text("다시 시도", color = AchievementGreen)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = AchievementGreen.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "첫 러닝을 시작해 보세요",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "달린 기록에 따라 새로운 업적이 열립니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun AchievementsBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grid = 34.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(
                AchievementGrid.copy(alpha = 0.34f),
                Offset(x, 0f),
                Offset(x, size.height),
                0.8f,
            )
            x += grid
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                AchievementGrid.copy(alpha = 0.34f),
                Offset(0f, y),
                Offset(size.width, y),
                0.8f,
            )
            y += grid
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AchievementGreen.copy(alpha = 0.09f),
                    AchievementGreen.copy(alpha = 0.025f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.8f, size.height * 0.08f),
                radius = size.width * 0.72f,
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.8f, size.height * 0.08f),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, AchievementBackground),
                startY = size.height * 0.45f,
                endY = size.height,
            ),
        )
    }
}
