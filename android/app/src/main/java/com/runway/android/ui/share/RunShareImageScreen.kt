package com.runway.android.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.share.MetricPreset
import com.runway.android.core.share.ShareTemplate
import com.runway.android.core.util.formatDuration
import com.runway.android.data.running.model.RunDetailResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RunShareImageScreen(
    onBack: () -> Unit,
    viewModel: RunShareImageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { uri ->
            context.startActivity(com.runway.android.core.share.ImageShareUtil.buildShareIntent(uri))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ─── TopBar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로 가기",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "공유 이미지",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        when {
            viewModel.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            viewModel.hasError -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "러닝 데이터를 불러오지 못했습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            viewModel.detail != null -> {
                val detail = viewModel.detail!!

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── 카드 미리보기 (crossfade on template change) ───
                    Crossfade(
                        targetState = viewModel.selectedTemplate,
                        animationSpec = tween(300),
                        label = "templateCrossfade",
                    ) { template ->
                        ShareCardPreview(
                            detail = detail,
                            template = template,
                            preset = viewModel.selectedPreset,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.extraLarge),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ─── 템플릿 선택 ───
                    Text(
                        text = "테마 선택",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                    ) {
                        items(ShareTemplate.entries) { template ->
                            TemplateChip(
                                template = template,
                                selected = viewModel.selectedTemplate == template,
                                onClick = { viewModel.onTemplateChange(template) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ─── 지표 프리셋 선택 ───
                    Text(
                        text = "지표 선택",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = viewModel.selectedPreset == preset,
                                onClick = { viewModel.onPresetChange(preset) },
                                label = { Text(preset.displayName, style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ─── 에러 메시지 ───
                    if (viewModel.shareError != null) {
                        Text(
                            text = viewModel.shareError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                // ─── 공유 버튼 ───
                Button(
                    onClick = { viewModel.captureAndShare(context) },
                    enabled = !viewModel.isSharing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    if (viewModel.isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("이미지 생성 중…")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("공유하기")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareCardPreview(
    detail: RunDetailResponse,
    template: ShareTemplate,
    preset: MetricPreset,
    modifier: Modifier = Modifier,
) {
    val bgColor = Color(template.bgColor)
    val accentColor = Color(template.accentColor)
    val textPrimary = Color(template.textPrimary)
    val textSecondary = Color(template.textSecondary)

    val distKm = (detail.distanceMeters ?: 0.0) / 1000.0

    Box(
        modifier = modifier.background(bgColor),
    ) {
        // Top accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accentColor),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Brand
            Text(
                text = "RUN WAY",
                color = textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )
            Text(
                text = formatPreviewDate(detail.startedAt),
                color = textSecondary,
                fontSize = 12.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Distance
            Text(
                text = "%.2f".format(distKm),
                color = textPrimary,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 64.sp,
            )
            Text(text = "km", color = textSecondary, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics — preset-dependent
            when (preset) {
                MetricPreset.FULL_STATS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text(text = "시간", color = textSecondary, fontSize = 11.sp)
                            Text(
                                text = formatPreviewDuration(detail.durationSeconds),
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column {
                            Text(text = "페이스", color = textSecondary, fontSize = 11.sp)
                            Text(
                                text = formatPreviewPace(detail.avgPaceSecondsPerKm),
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (detail.caloriesBurned != null) {
                            Column {
                                Text(text = "칼로리", color = textSecondary, fontSize = 11.sp)
                                Text(
                                    text = "${detail.caloriesBurned} kcal",
                                    color = textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                MetricPreset.DISTANCE_FOCUS -> {
                    Column {
                        Text(text = "소요 시간", color = textSecondary, fontSize = 11.sp)
                        Text(
                            text = formatPreviewDuration(detail.durationSeconds),
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                MetricPreset.PACE_FOCUS -> {
                    Column {
                        Text(text = "평균 페이스", color = textSecondary, fontSize = 11.sp)
                        Text(
                            text = formatPreviewPace(detail.avgPaceSecondsPerKm),
                            color = accentColor,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "소요 시간", color = textSecondary, fontSize = 11.sp)
                        Text(
                            text = formatPreviewDuration(detail.durationSeconds),
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Run your way.",
                color = accentColor,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun TemplateChip(
    template: ShareTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = Color(template.bgColor)
    val accentColor = Color(template.accentColor)
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "chipBorder",
    )
    val chipScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale",
    )

    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 48.dp)
            .scale(chipScale)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(borderWidth, accentColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accentColor),
        )
        Text(
            text = template.displayName,
            color = Color(template.textPrimary),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
    }
}

private fun formatPreviewDate(isoDate: String?): String {
    if (isoDate == null) return ""
    return runCatching {
        val instant = Instant.parse(isoDate)
        DateTimeFormatter.ofPattern("yyyy.MM.dd")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }.getOrDefault("")
}

private fun formatPreviewDuration(seconds: Int?): String {
    return formatDuration(seconds)
}

private fun formatPreviewPace(secsPerKm: Int?): String {
    if (secsPerKm == null || secsPerKm <= 0) return "--'--\""
    return "%d'%02d\"".format(secsPerKm / 60, secsPerKm % 60)
}
