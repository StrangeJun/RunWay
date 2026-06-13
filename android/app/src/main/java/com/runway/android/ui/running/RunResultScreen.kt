package com.runway.android.ui.running

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.RouteMapView
import com.runway.android.ui.components.runwayCardFrame
import com.runway.android.ui.course.CreateCourseDialog
import com.runway.android.ui.theme.DisplayFontFamily
import com.runway.android.ui.theme.PillShape

private val BgDark   = Color(0xFF080C0B)
private val Accent   = Color(0xFFB8FF00)
private val StatCard = Color(0xFF0E1612)

@Composable
fun RunResultScreen(
    onBackToHome: () -> Unit,
    onShareImage: (runId: String) -> Unit = {},
    onOpenRunDetail: (String) -> Unit = {},
    onNavigateToCourseDetail: (String) -> Unit = {},
    viewModel: RunResultViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.courseCreated.collect(onNavigateToCourseDetail) }

    Box(
        modifier = Modifier.fillMaxSize().background(BgDark),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── 상단 지도 영역 ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            ) {
                if (viewModel.isLoadingRoute) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF101510)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                    }
                } else {
                    RouteMapView(
                        points = viewModel.routePoints,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = viewModel.runId != null) {
                                viewModel.runId?.let(onOpenRunDetail)
                            },
                    )
                }

                // 하단 그라데이션 페이드
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, BgDark)),
                        ),
                )

                // 상단 statusBar 패딩용 그라데이션
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(listOf(BgDark.copy(0.7f), Color.Transparent)),
                        ),
                )

                // RUN COMPLETE 배지
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                    shape = PillShape,
                    color = Accent,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsRun,
                            null,
                            Modifier.size(14.dp),
                            tint = BgDark,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "RUN COMPLETE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = BgDark,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }

            // ── 메인 콘텐츠 ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(4.dp))

                // 거리 — 히어로 수치
                Text(
                    text = viewModel.distanceText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = DisplayFontFamily,
                        fontSize = 88.sp,
                        lineHeight = 92.sp,
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "KILOMETERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    letterSpacing = 3.sp,
                )

                Spacer(Modifier.height(28.dp))

                // ── 주요 지표 2×2 그리드 ──────────────────────────────────
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .runwayCardFrame(MaterialTheme.shapes.extraLarge),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = StatCard,
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            StatItem(
                                value = viewModel.timerText,
                                label = "TIME",
                                modifier = Modifier.weight(1f),
                            )
                            VerticalStatDivider()
                            StatItem(
                                value = viewModel.paceText,
                                label = "PACE",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        HorizontalDivider(
                            color = Accent.copy(alpha = 0.10f),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            StatItem(
                                value = viewModel.stepsText,
                                label = "STEPS",
                                modifier = Modifier.weight(1f),
                            )
                            VerticalStatDivider()
                            StatItem(
                                value = viewModel.caloriesText.removeSuffix(" kcal"),
                                label = "KCAL",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── 속도 시각화 바 ─────────────────────────────────────────
                PaceBar(paceText = viewModel.paceText)

                Spacer(Modifier.height(24.dp))

                // ── 액션 버튼 ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = viewModel::onShowCreateDialog,
                        enabled = viewModel.runId != null,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor   = BgDark,
                            disabledContainerColor = Accent.copy(0.4f),
                            disabledContentColor   = BgDark,
                        ),
                    ) {
                        Icon(Icons.Filled.AddCircleOutline, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "코스 만들기",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Button(
                        onClick = { viewModel.runId?.let(onShareImage) },
                        enabled = viewModel.runId != null,
                        modifier = Modifier.height(52.dp).width(72.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(0.08f),
                            contentColor   = Color.White,
                        ),
                    ) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                    }
                }

                if (viewModel.courseCreatedSuccess) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            Modifier.size(15.dp),
                            tint = Accent,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "코스가 생성되었습니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = Accent,
                        )
                    }
                }

                TextButton(
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "홈으로",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(0.35f),
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (viewModel.showCreateDialog) {
        CreateCourseDialog(
            courseName     = viewModel.courseName,
            onCourseNameChange = viewModel::onCourseNameChange,
            isLoop         = viewModel.isLoop,
            onIsLoopChange = viewModel::onIsLoopChange,
            isCreating     = viewModel.isCreating,
            errorMessage   = viewModel.createError,
            onConfirm      = viewModel::createCourse,
            onDismiss      = viewModel::onDismissCreateDialog,
        )
    }
}

// ── 지표 셀 ──────────────────────────────────────────────────────────────────

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 22.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = DisplayFontFamily,
                fontSize = 34.sp,
                lineHeight = 36.sp,
            ),
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Accent.copy(0.70f),
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun VerticalStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(80.dp)
            .background(Accent.copy(0.10f)),
    )
}

// ── 페이스 바 (시각화) ────────────────────────────────────────────────────────

@Composable
private fun PaceBar(paceText: String) {
    val secsPerKm = runCatching {
        val parts = paceText.removeSuffix("/km").split("'", "\"", ":")
        parts[0].toInt() * 60 + parts.getOrNull(1)?.toInt().orDefault(0)
    }.getOrElse { 0 }

    // 4:00~8:00 사이 정규화 (240~480초)
    val ratio = if (secsPerKm > 0) ((secsPerKm - 240f) / 240f).coerceIn(0f, 1f) else 0f
    val fillRatio = 1f - ratio  // 빠를수록 더 채워짐

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "PACE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.35f),
                letterSpacing = 1.5.sp,
            )
            Text(
                paceText,
                style = MaterialTheme.typography.labelSmall,
                color = Accent.copy(0.80f),
                letterSpacing = 0.5.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        ) {
            drawLine(
                color = Color.White.copy(0.08f),
                start = Offset(0f, size.height / 2),
                end   = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Accent.copy(0.50f), Accent),
                ),
                start = Offset(0f, size.height / 2),
                end   = Offset(size.width * fillRatio, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("4:00", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.22f), fontSize = 9.sp)
            Text("6:00", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.22f), fontSize = 9.sp)
            Text("8:00+", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.22f), fontSize = 9.sp)
        }
    }
}

private fun Int?.orDefault(default: Int) = this ?: default
