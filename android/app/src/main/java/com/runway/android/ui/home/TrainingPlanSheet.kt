package com.runway.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.runway.android.data.reminder.ReminderWorkoutType
import com.runway.android.data.training.model.TrainingDay
import com.runway.android.data.training.model.TrainingPlanRequest
import com.runway.android.data.training.model.TrainingPlanResponse
import com.runway.android.ui.components.RunwayPrimaryButton
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanSheet(
    plan: TrainingPlanResponse?,
    isGenerating: Boolean,
    error: String?,
    applied: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (TrainingPlanRequest) -> Unit,
    onUpdateDay: (TrainingDay) -> Unit,
    onApplyToReminder: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        text = "AI 훈련 스케줄",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "목표에 맞는 1주 계획을 만들고 직접 수정하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            TrainingPlanGoalForm(
                isGenerating = isGenerating,
                onGenerate = onGenerate,
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            plan?.let { generated ->
                Spacer(Modifier.height(24.dp))
                Text(
                    text = generated.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = generated.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                Text(
                    text = if (generated.source == "GEMINI") "Gemini 추천" else "기본 추천",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                generated.days
                    .sortedBy(::calendarDayOrder)
                    .forEach { day ->
                        Spacer(Modifier.height(12.dp))
                        EditableTrainingDayCard(day = day, onUpdate = onUpdateDay)
                    }

                Spacer(Modifier.height(20.dp))
                RunwayPrimaryButton(
                    text = if (applied) "리마인더 적용 완료" else "리마인더에 적용",
                    onClick = onApplyToReminder,
                    enabled = !applied,
                )
                Text(
                    text = "러닝이 있는 요일만 현재 리마인더 시간으로 예약됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingPlanGoalForm(
    isGenerating: Boolean,
    onGenerate: (TrainingPlanRequest) -> Unit,
) {
    var goalDistance by remember { mutableStateOf("42.195") }
    var goalTime by remember { mutableStateOf("240") }
    var targetPace by remember { mutableStateOf("5:41") }
    var weeklyKm by remember { mutableStateOf("20") }
    var trainingDays by remember { mutableStateOf(4) }
    var experience by remember { mutableStateOf("BEGINNER") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("5", "10", "21.1", "42.195").forEach { distance ->
                FilterChip(
                    selected = goalDistance == distance,
                    onClick = { goalDistance = distance },
                    label = { Text("${distance}km") },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanInput(
                value = goalDistance,
                onValueChange = { goalDistance = decimalInput(it, 6) },
                label = "목표 거리",
                suffix = "km",
                modifier = Modifier.weight(1f),
            )
            PlanInput(
                value = goalTime,
                onValueChange = { goalTime = it.filter(Char::isDigit).take(4) },
                label = "목표 기록",
                suffix = "분",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanInput(
                value = targetPace,
                onValueChange = { targetPace = it.filter { char -> char.isDigit() || char == ':' }.take(5) },
                label = "목표 페이스",
                suffix = "/km",
                modifier = Modifier.weight(1f),
            )
            PlanInput(
                value = weeklyKm,
                onValueChange = { weeklyKm = decimalInput(it, 5) },
                label = "현재 주간 거리",
                suffix = "km",
                modifier = Modifier.weight(1f),
            )
        }

        Text("주당 훈련 가능 일수", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (2..7).forEach { count ->
                FilterChip(
                    selected = trainingDays == count,
                    onClick = { trainingDays = count },
                    label = { Text("${count}일") },
                )
            }
        }

        Text("러닝 경험", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "BEGINNER" to "입문",
                "INTERMEDIATE" to "중급",
                "ADVANCED" to "상급",
            ).forEach { (value, label) ->
                FilterChip(
                    selected = experience == value,
                    onClick = { experience = value },
                    label = { Text(label) },
                )
            }
        }

        RunwayPrimaryButton(
            text = if (isGenerating) "계획 생성 중..." else "1주 훈련계획 추천받기",
            enabled = !isGenerating && goalDistance.toDoubleOrNull() != null,
            onClick = {
                onGenerate(
                    TrainingPlanRequest(
                        goalDistanceKm = goalDistance.toDoubleOrNull() ?: return@RunwayPrimaryButton,
                        goalTimeMinutes = goalTime.toIntOrNull(),
                        targetPace = targetPace,
                        currentWeeklyKm = weeklyKm.toDoubleOrNull(),
                        trainingDays = trainingDays,
                        experienceLevel = experience,
                    ),
                )
            },
        )
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableTrainingDayCard(
    day: TrainingDay,
    onUpdate: (TrainingDay) -> Unit,
) {
    var distanceText by remember(day.dayOfWeek) {
        mutableStateOf(day.distanceKm?.toString().orEmpty())
    }
    var durationText by remember(day.dayOfWeek) {
        mutableStateOf(day.durationMinutes?.toString().orEmpty())
    }
    LaunchedEffect(day.distanceKm) {
        val external = day.distanceKm?.toString().orEmpty()
        if (distanceText.toDoubleOrNull() != day.distanceKm) distanceText = external
    }
    LaunchedEffect(day.durationMinutes) {
        if (durationText.toIntOrNull() != day.durationMinutes) {
            durationText = day.durationMinutes?.toString().orEmpty()
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = calendarDayLabel(day.dayOfWeek),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("휴식", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = day.restDay,
                        onCheckedChange = { rest ->
                            onUpdate(
                                day.copy(
                                    restDay = rest,
                                    workoutType = if (rest) "REST" else "EASY",
                                    title = if (rest) "휴식" else "이지런",
                                ),
                            )
                        },
                    )
                }
            }

            if (!day.restDay) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ReminderWorkoutType.entries.forEach { type ->
                        FilterChip(
                            selected = day.workoutType == type.name,
                            onClick = {
                                onUpdate(day.copy(workoutType = type.name, title = type.label))
                            },
                            label = { Text(type.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanInput(
                        value = distanceText,
                        onValueChange = {
                            distanceText = decimalInput(it, 6)
                            onUpdate(day.copy(distanceKm = distanceText.toDoubleOrNull()))
                        },
                        label = "거리",
                        suffix = "km",
                        modifier = Modifier.weight(1f),
                    )
                    PlanInput(
                        value = durationText,
                        onValueChange = {
                            durationText = it.filter(Char::isDigit).take(4)
                            onUpdate(day.copy(durationMinutes = durationText.toIntOrNull()))
                        },
                        label = "시간",
                        suffix = "분",
                        modifier = Modifier.weight(1f),
                    )
                }
                PlanInput(
                    value = day.targetPace,
                    onValueChange = {
                        onUpdate(day.copy(targetPace = it.filter { char -> char.isDigit() || char == ':' }.take(5)))
                    },
                    label = "목표 페이스",
                    suffix = "/km",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = day.description,
                onValueChange = { onUpdate(day.copy(description = it.take(120))) },
                label = { Text("세부 훈련 내용") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlanInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
fun TrainingPlanPromoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI 훈련 스케줄 추천",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "목표 기록과 페이스에 맞춘 1주 계획을 만들어보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClick) {
                Text("추천받기")
            }
        }
    }
}

private fun decimalInput(value: String, maxLength: Int): String {
    val filtered = value.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot < 0) {
        filtered.take(maxLength)
    } else {
        (filtered.substring(0, firstDot + 1) +
            filtered.substring(firstDot + 1).replace(".", "")).take(maxLength)
    }
}

private fun calendarDayOrder(day: TrainingDay): Int = when (day.dayOfWeek) {
    Calendar.MONDAY -> 1
    Calendar.TUESDAY -> 2
    Calendar.WEDNESDAY -> 3
    Calendar.THURSDAY -> 4
    Calendar.FRIDAY -> 5
    Calendar.SATURDAY -> 6
    Calendar.SUNDAY -> 7
    else -> 8
}

private fun calendarDayLabel(day: Int): String = when (day) {
    Calendar.MONDAY -> "월요일"
    Calendar.TUESDAY -> "화요일"
    Calendar.WEDNESDAY -> "수요일"
    Calendar.THURSDAY -> "목요일"
    Calendar.FRIDAY -> "금요일"
    Calendar.SATURDAY -> "토요일"
    Calendar.SUNDAY -> "일요일"
    else -> "요일"
}
