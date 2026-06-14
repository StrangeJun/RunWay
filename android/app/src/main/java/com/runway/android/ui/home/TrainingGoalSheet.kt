package com.runway.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runway.android.domain.training.TrainingGoal
import com.runway.android.domain.training.TrainingGoalType
import com.runway.android.ui.components.RunwayPrimaryButton
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingGoalSheet(
    currentGoal: TrainingGoal?,
    onDismiss: () -> Unit,
    onConfirm: (TrainingGoal) -> Unit,
) {
    val initial = currentGoal ?: TrainingGoal()
    var goalType by remember { mutableStateOf(initial.goalType) }
    val initialDistanceHundredths = ((initial.goalDistanceKm ?: 10.0) * 100).toInt()
    var distanceKm by remember { mutableIntStateOf(initialDistanceHundredths / 100) }
    var distanceDecimal by remember { mutableIntStateOf(initialDistanceHundredths % 100) }
    val initialGoalSeconds = initial.goalTimeSeconds ?: 60 * 60
    var goalHours by remember { mutableIntStateOf(initialGoalSeconds / 3_600) }
    var goalMinutes by remember { mutableIntStateOf(initialGoalSeconds % 3_600 / 60) }
    var goalDate by remember {
        mutableStateOf(
            initial.goalDate
                ?.takeUnless { it.isBefore(LocalDate.now()) }
                ?: LocalDate.now().plusWeeks(8),
        )
    }
    var weeklyRuns by remember { mutableIntStateOf(initial.preferredRunsPerWeek) }
    var availableDays by remember { mutableStateOf(initial.availableDays) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .fillMaxWidth(0.1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding(),
        ) {
            TrainingGoalHeader(onDismiss)

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                GoalSection(
                    title = "어떤 목표로 달릴까요?",
                    description = "선택한 목표와 최근 기록을 함께 분석해요.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrainingGoalType.entries.forEach { type ->
                            GoalTypeCard(
                                type = type,
                                selected = goalType == type,
                                onClick = {
                                    goalType = type
                                    validationMessage = null
                                },
                            )
                        }
                    }
                }

                GoalSection(
                    title = "목표 상세",
                    description = goalType.detailDescription(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        if (goalType != TrainingGoalType.FITNESS) {
                            GoalPickerBlock(
                                label = "목표 거리",
                                value = distanceLabel(distanceKm, distanceDecimal),
                            ) {
                                DistanceWheelPicker(
                                    km = distanceKm,
                                    decimal = distanceDecimal,
                                    onKmChange = { distanceKm = it },
                                    onDecimalChange = { distanceDecimal = it },
                                    maxKm = 100,
                                )
                            }
                        }

                        if (goalType == TrainingGoalType.RACE_TIME) {
                            GoalPickerBlock(
                                label = "목표 기록",
                                value = timeLabel(goalHours, goalMinutes),
                            ) {
                                TimeWheelPicker(
                                    hours = goalHours,
                                    minutes = goalMinutes,
                                    onHoursChange = {
                                        goalHours = it
                                        if (it == 0 && goalMinutes == 0) goalMinutes = 1
                                    },
                                    onMinutesChange = {
                                        goalMinutes = it.coerceAtLeast(
                                            if (goalHours == 0) 1 else 0,
                                        )
                                    },
                                )
                            }
                        }

                        GoalPickerBlock(
                            label = if (goalType == TrainingGoalType.FITNESS) {
                                "습관 형성 목표일"
                            } else {
                                "목표 날짜"
                            },
                            value = goalDate.toString(),
                        ) {
                            DateWheelPicker(
                                date = goalDate,
                                onDateChange = { goalDate = it },
                            )
                        }
                    }
                }

                GoalSection(
                    title = "주당 훈련 횟수",
                    description = "현재 생활 패턴에서 꾸준히 지킬 수 있는 횟수를 선택하세요.",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (1..7).forEach { count ->
                            CompactChoice(
                                text = count.toString(),
                                selected = weeklyRuns == count,
                                onClick = { weeklyRuns = count },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                GoalSection(
                    title = "훈련 가능한 요일",
                    description = "${availableDays.size}일 선택 · 훈련 사이 회복일도 함께 고려해요.",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val day = index + 1
                            DayChoice(
                                text = label,
                                selected = day in availableDays,
                                onClick = {
                                    availableDays = if (day in availableDays) {
                                        availableDays - day
                                    } else {
                                        availableDays + day
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                validationMessage?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                }

                RunwayPrimaryButton(
                    text = "AI 훈련 스케줄 만들기",
                    onClick = {
                        val parsedDistance = distanceKm + distanceDecimal / 100.0
                        val parsedTime = if (goalType == TrainingGoalType.RACE_TIME) {
                            goalHours * 3_600 + goalMinutes * 60
                        } else {
                            null
                        }
                        validationMessage = when {
                            goalType != TrainingGoalType.FITNESS &&
                                parsedDistance !in 1.0..100.0 ->
                                "목표 거리를 1~100km로 입력해 주세요."
                            goalDate.isBefore(LocalDate.now()) ->
                                "오늘 이후의 목표 날짜를 입력해 주세요."
                            availableDays.size < weeklyRuns ->
                                "주당 훈련 횟수만큼 가능한 요일을 선택해 주세요."
                            else -> null
                        }
                        if (validationMessage == null) {
                            onConfirm(
                                TrainingGoal(
                                    goalType = goalType,
                                    goalDistanceKm = if (goalType == TrainingGoalType.FITNESS) {
                                        null
                                    } else {
                                        parsedDistance
                                    },
                                    goalDate = goalDate,
                                    goalTimeSeconds = parsedTime,
                                    preferredRunsPerWeek = weeklyRuns,
                                    availableDays = availableDays,
                                ),
                            )
                        }
                    },
                )

                Text(
                    text = "최근 러닝 기록이 3회 이상이면 페이스와 훈련량을 더 정교하게 반영합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TrainingGoalHeader(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(11.dp),
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("닫기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = "나만의 러닝 목표",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "목표와 가능한 일정을 알려주면 Gemini가 최근 기록을 분석해 이번 주 스케줄을 설계합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun GoalTypeCard(
    type: TrainingGoalType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        MaterialTheme.shapes.medium,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Icon(
                    imageVector = type.icon(),
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(10.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = type.label(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = type.shortDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun GoalPickerBlock(
    label: String,
    value: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        content()
    }
}

@Composable
private fun CompactChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val dayLabels = listOf("월", "화", "수", "목", "금", "토", "일")

private fun TrainingGoalType.label(): String = when (this) {
    TrainingGoalType.DISTANCE -> "목표 거리 완주"
    TrainingGoalType.RACE_TIME -> "목표 기록 달성"
    TrainingGoalType.FITNESS -> "기초 체력 만들기"
}

private fun TrainingGoalType.shortDescription(): String = when (this) {
    TrainingGoalType.DISTANCE -> "5km, 10km, 하프 등 원하는 거리를 완주해요."
    TrainingGoalType.RACE_TIME -> "목표 거리와 기록에 맞춰 페이스를 훈련해요."
    TrainingGoalType.FITNESS -> "기록보다 꾸준한 러닝 습관과 지구력을 만들어요."
}

private fun TrainingGoalType.detailDescription(): String = when (this) {
    TrainingGoalType.DISTANCE -> "완주하고 싶은 거리와 목표 날짜를 입력하세요."
    TrainingGoalType.RACE_TIME -> "도전할 거리와 목표 기록, 날짜를 함께 입력하세요."
    TrainingGoalType.FITNESS -> "특정 거리나 기록 없이 목표일까지 규칙적으로 달리는 계획을 만들어요."
}

private fun TrainingGoalType.icon(): ImageVector = when (this) {
    TrainingGoalType.DISTANCE -> Icons.Filled.Flag
    TrainingGoalType.RACE_TIME -> Icons.Filled.EmojiEvents
    TrainingGoalType.FITNESS -> Icons.Filled.Bolt
}

private fun distanceLabel(km: Int, decimal: Int): String =
    if (decimal == 0) "${km}km" else "$km.${decimal.toString().padStart(2, '0')}km"

private fun timeLabel(hours: Int, minutes: Int): String =
    if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
