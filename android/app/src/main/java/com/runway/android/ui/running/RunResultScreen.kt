package com.runway.android.ui.running

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.RouteMapView
import com.runway.android.ui.course.CreateCourseDialog
import com.runway.android.ui.theme.PillShape
import com.runway.android.ui.theme.RunwayElectricBlue
import com.runway.android.ui.theme.RunwayPurple

private val ResultPurple = Color(0xFF7138E8)
private val ResultGold = Color(0xFFFFC84A)

@Composable
fun RunResultScreen(
    onBackToHome: () -> Unit,
    onShareImage: (runId: String) -> Unit = {},
    onOpenRunDetail: (String) -> Unit = {},
    onNavigateToCourseDetail: (String) -> Unit = {},
    viewModel: RunResultViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.courseCreated.collect(onNavigateToCourseDetail)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ResultPurple.copy(alpha = 0.34f),
                            Color.Transparent,
                        ),
                        radius = 760f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 14.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "♛", fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "RUN COMPLETE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${viewModel.distanceText} km",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 42.sp,
                    lineHeight = 46.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${viewModel.timerText} • ${viewModel.paceText}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(18.dp, MaterialTheme.shapes.large)
                    .clip(MaterialTheme.shapes.large)
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    RouteMapView(
                        points = viewModel.routePoints,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "🏆  Earned Achievements",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ResultAchievement(
                    value = "★",
                    label = "Run Complete",
                    color = ResultGold,
                )
                ResultAchievement(
                    value = "↗",
                    label = viewModel.paceText.removeSuffix("/km"),
                    color = Color(0xFF43E6BA),
                )
                ResultAchievement(
                    value = viewModel.distanceText,
                    label = "km Total",
                    color = MaterialTheme.colorScheme.primary,
                )
                ResultAchievement(
                    value = viewModel.stepsText,
                    label = "Steps",
                    color = RunwayElectricBlue,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = viewModel::onShowCreateDialog,
                    enabled = viewModel.runId != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("코스 만들기", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { viewModel.runId?.let(onShareImage) },
                    enabled = viewModel.runId != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResultPurple,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("공유", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (viewModel.courseCreatedSuccess) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "코스가 생성되었습니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            TextButton(onClick = onBackToHome) {
                Text(
                    text = "홈으로",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

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
private fun ResultAchievement(
    value: String,
    label: String,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(66.dp),
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(12.dp, CircleShape, ambientColor = color, spotColor = color)
                .background(color.copy(alpha = 0.15f), CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = 0.32f), Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(2.dp, color),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = color,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
