package com.runway.android.ui.running

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.RouteMapView
import com.runway.android.ui.components.RunwayPrimaryButton
import com.runway.android.ui.course.CreateCourseDialog
import com.runway.android.ui.theme.OutlineVariantDark
import com.runway.android.ui.theme.SurfaceContainerDark
import kotlinx.coroutines.delay

@Composable
fun RunResultScreen(
    onBackToHome: () -> Unit,
    onShareImage: (runId: String) -> Unit = {},
    onOpenRunDetail: (String) -> Unit = {},
    onNavigateToCourseDetail: (String) -> Unit = {},
    viewModel: RunResultViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.courseCreated.collect { courseId ->
            onNavigateToCourseDetail(courseId)
        }
    }

    var distTriggered by remember { mutableStateOf(false) }
    var cardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        distTriggered = true
        delay(250)
        cardsVisible = true
    }
    val distTarget = viewModel.distanceText.toFloatOrNull() ?: 0f
    val animatedDist by animateFloatAsState(
        targetValue = if (distTriggered) distTarget else 0f,
        animationSpec = tween(900),
        label = "heroDistance",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ─── Scrollable content ───
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 160.dp),
        ) {

            // 1. Ambient top glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                1f to Color.Transparent,
                            ),
                        ),
                )
            }

            // 2. Hero section — centered
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // "RUN COMPLETE" badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 16.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RUN COMPLETE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            letterSpacing = 2.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Big animated distance
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.2f".format(animatedDist),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                // Duration • pace
                Text(
                    text = "${viewModel.timerText} • ${viewModel.paceText}/km",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // 3. Route map card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 20.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable(enabled = viewModel.runId != null && !viewModel.isLoadingRoute) {
                        viewModel.runId?.let(onOpenRunDetail)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (viewModel.isLoadingRoute) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                } else {
                    RouteMapView(
                        points = viewModel.routePoints,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Stats row — TIME and CALORIES
            AnimatedVisibility(
                visible = cardsVisible,
                enter = slideInVertically(tween(350)) { it / 2 } + fadeIn(tween(350)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricCard(
                        label = "TIME",
                        value = viewModel.timerText,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "CALORIES",
                        value = viewModel.caloriesText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ─── Sticky bottom buttons ───
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to MaterialTheme.colorScheme.background,
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (viewModel.courseCreatedSuccess) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "코스가 생성되었습니다!",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                RunwayPrimaryButton(
                    text = "이 러닝으로 코스 만들기",
                    onClick = { viewModel.onShowCreateDialog() },
                    enabled = viewModel.runId != null,
                )
            }
            if (viewModel.runId != null) {
                OutlinedButton(
                    onClick = { onShareImage(viewModel.runId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(
                        text = "공유 이미지 만들기",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            TextButton(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "홈으로",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // ─── 코스 저장 다이얼로그 (항상 draft로 저장) ───
    if (viewModel.showCreateDialog) {
        CreateCourseDialog(
            courseName = viewModel.courseName,
            onCourseNameChange = viewModel::onCourseNameChange,
            isLoop = viewModel.isLoop,
            onIsLoopChange = viewModel::onIsLoopChange,
            isCreating = viewModel.isCreating,
            errorMessage = viewModel.createError,
            onConfirm = viewModel::createCourse,
            onDismiss = viewModel::onDismissCreateDialog,
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = SurfaceContainerDark,
        border = BorderStroke(1.dp, OutlineVariantDark),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
