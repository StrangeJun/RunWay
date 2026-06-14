package com.runway.android.ui.reminder

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.data.reminder.DayRunningPlan
import com.runway.android.data.reminder.ReminderPlanFormatter
import com.runway.android.data.reminder.ReminderWorkoutType
import com.runway.android.ui.components.runwayCardFrame
import com.runway.android.ui.theme.SurfaceContainerDark
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderScreen(
    onBack: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // 상단 바
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
                text = "러닝 리마인더",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 알림 활성화 토글
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .runwayCardFrame(MaterialTheme.shapes.extraLarge),
            shape = MaterialTheme.shapes.extraLarge,
            color = SurfaceContainerDark,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (viewModel.enabled) Icons.Filled.NotificationsActive
                        else Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = if (viewModel.enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = "리마인더 알림",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (viewModel.enabled) "활성화됨" else "비활성화됨",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = viewModel.enabled,
                    onCheckedChange = viewModel::toggleEnabled,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 시간 설정
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .runwayCardFrame(MaterialTheme.shapes.extraLarge),
            shape = MaterialTheme.shapes.extraLarge,
            color = SurfaceContainerDark,
            onClick = {
                TimePickerDialog(
                    context,
                    { _, h, m -> viewModel.setTime(h, m) },
                    viewModel.hour,
                    viewModel.minute,
                    true,
                ).show()
            },
            enabled = viewModel.enabled,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = if (viewModel.enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "알림 시간",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (viewModel.enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
                Text(
                    text = "%02d:%02d".format(viewModel.hour, viewModel.minute),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 요일 선택
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "반복 요일".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            val days = listOf(
                Calendar.MONDAY to "월",
                Calendar.TUESDAY to "화",
                Calendar.WEDNESDAY to "수",
                Calendar.THURSDAY to "목",
                Calendar.FRIDAY to "금",
                Calendar.SATURDAY to "토",
                Calendar.SUNDAY to "일",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                days.forEach { (dayConst, label) ->
                    FilterChip(
                        selected = dayConst in viewModel.enabledDays,
                        onClick = { if (viewModel.enabled) viewModel.toggleDay(dayConst) },
                        label = { Text(label) },
                        enabled = viewModel.enabled,
                    )
                }
            }

            if (viewModel.enabledDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "요일별 러닝 계획".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                reminderDays()
                    .filter { (day, _) -> day in viewModel.enabledDays }
                    .forEach { (day, label) ->
                        DayRunningPlanCard(
                            dayLabel = label,
                            plan = viewModel.planFor(day),
                            enabled = viewModel.enabled,
                            onWorkoutTypeChange = { viewModel.setWorkoutType(day, it) },
                            onDistanceChange = { viewModel.setDistance(day, it) },
                            onPaceChange = { viewModel.setPace(day, it) },
                            onDurationChange = { viewModel.setDuration(day, it) },
                            onNoteChange = { viewModel.setNote(day, it) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 안내 문구
        Text(
            text = "거리, 페이스, 시간은 필요한 항목만 입력할 수 있으며 함께 설정할 수도 있습니다.\n설정한 계획은 알림 내용에 표시되고 재부팅 후에도 자동 복원됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayRunningPlanCard(
    dayLabel: String,
    plan: DayRunningPlan,
    enabled: Boolean,
    onWorkoutTypeChange: (ReminderWorkoutType) -> Unit,
    onDistanceChange: (String) -> Unit,
    onPaceChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${dayLabel}요일",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ReminderPlanFormatter.summary(plan),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReminderWorkoutType.entries.forEach { type ->
                    FilterChip(
                        selected = plan.workoutType == type,
                        onClick = { onWorkoutTypeChange(type) },
                        label = { Text(type.label) },
                        enabled = enabled,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ReminderNumberField(
                    value = plan.distanceKm,
                    onValueChange = onDistanceChange,
                    label = "거리",
                    placeholder = "5",
                    suffix = "km",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                ReminderNumberField(
                    value = plan.durationMinutes,
                    onValueChange = onDurationChange,
                    label = "러닝 시간",
                    placeholder = "30",
                    suffix = "분",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = plan.targetPace,
                onValueChange = onPaceChange,
                label = { Text("목표 페이스") },
                placeholder = { Text("예: 5:30") },
                suffix = { Text("/km") },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = plan.note,
                onValueChange = onNoteChange,
                label = { Text("추가 계획 또는 메모") },
                placeholder = { Text("예: 1km 워밍업 후 400m × 6회") },
                enabled = enabled,
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReminderNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    suffix: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        suffix = { Text(suffix) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

private fun reminderDays() = listOf(
    Calendar.MONDAY to "월",
    Calendar.TUESDAY to "화",
    Calendar.WEDNESDAY to "수",
    Calendar.THURSDAY to "목",
    Calendar.FRIDAY to "금",
    Calendar.SATURDAY to "토",
    Calendar.SUNDAY to "일",
)
