package com.runway.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runway.android.ui.components.RunwayPrimaryButton
import com.runway.android.ui.theme.SurfaceContainerDark
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSetupSheet(
    onDismiss: () -> Unit,
    onConfirm: (RunGoal) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainerDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f), RoundedCornerShape(2.dp)),
            )
        },
    ) {
        GoalSetupContent(
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

private enum class GoalTab(val label: String) { TIME("시간"), DISTANCE("거리"), INTERVAL("인터벌") }
private enum class SegmentMode(val label: String) { TIME("시간"), DISTANCE("거리") }

@Composable
private fun GoalSetupContent(
    onDismiss: () -> Unit,
    onConfirm: (RunGoal) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(GoalTab.DISTANCE) }

    var targetHours by remember { mutableIntStateOf(0) }
    var targetMinutes by remember { mutableIntStateOf(30) }

    var targetKm by remember { mutableIntStateOf(5) }
    var targetDecimal by remember { mutableIntStateOf(0) }

    var warmupMode by remember { mutableStateOf(SegmentMode.TIME) }
    var warmupMinutes by remember { mutableIntStateOf(10) }
    var warmupKm by remember { mutableIntStateOf(1) }
    var warmupDecimal by remember { mutableIntStateOf(0) }

    var workMode by remember { mutableStateOf(SegmentMode.DISTANCE) }
    var workMinutes by remember { mutableIntStateOf(5) }
    var workKm by remember { mutableIntStateOf(1) }
    var workDecimal by remember { mutableIntStateOf(0) }
    var workPace by remember { mutableIntStateOf(300) }
    var workHasPace by remember { mutableStateOf(true) }

    var recoveryMode by remember { mutableStateOf(SegmentMode.TIME) }
    var recoveryMinutes by remember { mutableIntStateOf(2) }
    var recoveryKm by remember { mutableIntStateOf(0) }
    var recoveryDecimal by remember { mutableIntStateOf(40) }
    var recoveryPace by remember { mutableIntStateOf(420) }
    var recoveryHasPace by remember { mutableStateOf(false) }

    var cooldownMode by remember { mutableStateOf(SegmentMode.TIME) }
    var cooldownMinutes by remember { mutableIntStateOf(5) }
    var cooldownKm by remember { mutableIntStateOf(1) }
    var cooldownDecimal by remember { mutableIntStateOf(0) }

    var sets by remember { mutableIntStateOf(3) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "목표 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onDismiss) {
                Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))

        // 탭 선택
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .goalCardBorder(MaterialTheme.shapes.medium),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                GoalTab.entries.forEach { tab ->
                    val active = tab == selectedTab
                    androidx.compose.material3.Surface(
                        onClick = { selectedTab = tab },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))

        when (selectedTab) {
            GoalTab.TIME -> TimeGoalContent(
                hours = targetHours,
                minutes = targetMinutes,
                onHoursChange = {
                    targetHours = it
                    if (it == 0 && targetMinutes == 0) targetMinutes = 1
                },
                onMinutesChange = {
                    targetMinutes = it.coerceAtLeast(if (targetHours == 0) 1 else 0)
                },
            )
            GoalTab.DISTANCE -> DistanceGoalContent(
                km = targetKm,
                decimal = targetDecimal,
                onKmChange = { targetKm = it },
                onDecimalChange = { targetDecimal = it },
            )
            GoalTab.INTERVAL -> IntervalGoalContent(
                warmupMode = warmupMode,
                onWarmupModeChange = { warmupMode = it },
                warmupMinutes = warmupMinutes,
                onWarmupMinutesChange = { warmupMinutes = it },
                warmupKm = warmupKm,
                onWarmupKmChange = { warmupKm = it },
                warmupDecimal = warmupDecimal,
                onWarmupDecimalChange = { warmupDecimal = it },
                workMode = workMode,
                onWorkModeChange = { workMode = it },
                workMinutes = workMinutes,
                onWorkMinutesChange = { workMinutes = it },
                workKm = workKm,
                onWorkKmChange = { workKm = it },
                workDecimal = workDecimal,
                onWorkDecimalChange = { workDecimal = it },
                workPace = workPace,
                onWorkPaceChange = { workPace = it },
                workHasPace = workHasPace,
                onWorkHasPaceChange = { workHasPace = it },
                recoveryMode = recoveryMode,
                onRecoveryModeChange = { recoveryMode = it },
                recoveryMinutes = recoveryMinutes,
                onRecoveryMinutesChange = { recoveryMinutes = it },
                recoveryKm = recoveryKm,
                onRecoveryKmChange = { recoveryKm = it },
                recoveryDecimal = recoveryDecimal,
                onRecoveryDecimalChange = { recoveryDecimal = it },
                recoveryPace = recoveryPace,
                onRecoveryPaceChange = { recoveryPace = it },
                recoveryHasPace = recoveryHasPace,
                onRecoveryHasPaceChange = { recoveryHasPace = it },
                cooldownMode = cooldownMode,
                onCooldownModeChange = { cooldownMode = it },
                cooldownMinutes = cooldownMinutes,
                onCooldownMinutesChange = { cooldownMinutes = it },
                cooldownKm = cooldownKm,
                onCooldownKmChange = { cooldownKm = it },
                cooldownDecimal = cooldownDecimal,
                onCooldownDecimalChange = { cooldownDecimal = it },
                sets = sets,
                onSetsChange = { sets = it },
            )
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        Spacer(Modifier.height(12.dp))

        RunwayPrimaryButton(
            text = "시작하기",
            onClick = {
                val goal = when (selectedTab) {
                    GoalTab.TIME -> RunGoal.TimeGoal((targetHours * 60 + targetMinutes).coerceAtLeast(1))
                    GoalTab.DISTANCE -> RunGoal.DistanceGoal(distanceKm(targetKm, targetDecimal))
                    GoalTab.INTERVAL -> RunGoal.IntervalGoal(
                        warmup = IntervalSegment(
                            duration = segmentDuration(warmupMode, warmupMinutes, warmupKm, warmupDecimal),
                        ),
                        work = IntervalSegment(
                            duration = segmentDuration(workMode, workMinutes, workKm, workDecimal),
                            paceTargetSecPerKm = if (workHasPace) workPace else null,
                        ),
                        recovery = IntervalSegment(
                            duration = segmentDuration(recoveryMode, recoveryMinutes, recoveryKm, recoveryDecimal),
                            paceTargetSecPerKm = if (recoveryHasPace) recoveryPace else null,
                        ),
                        cooldown = IntervalSegment(
                            duration = segmentDuration(cooldownMode, cooldownMinutes, cooldownKm, cooldownDecimal),
                        ),
                        sets = sets,
                    )
                }
                onConfirm(goal)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TimeGoalContent(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    val totalMinutes = (hours * 60 + minutes).coerceAtLeast(1)

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        GoalSectionLabel("목표 시간")
        Spacer(Modifier.height(14.dp))
        TimeWheelPicker(
            hours = hours,
            minutes = minutes,
            onHoursChange = onHoursChange,
            onMinutesChange = onMinutesChange,
        )
        Spacer(Modifier.height(18.dp))
        SimpleGoalSummary(
            title = "오늘의 러닝 목표",
            value = timeLabel(totalMinutes),
            description = "동안 달리기",
            icon = Icons.Filled.Timer,
        )
    }
}

@Composable
private fun DistanceGoalContent(
    km: Int,
    decimal: Int,
    onKmChange: (Int) -> Unit,
    onDecimalChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        GoalSectionLabel("목표 거리")
        Spacer(Modifier.height(14.dp))
        DistanceWheelPicker(
            km = km,
            decimal = decimal,
            onKmChange = onKmChange,
            onDecimalChange = onDecimalChange,
        )
        Spacer(Modifier.height(18.dp))
        SimpleGoalSummary(
            title = "오늘의 러닝 목표",
            value = distanceLabel(distanceMeters(km, decimal)),
            description = "목표 거리 달리기",
            icon = Icons.Filled.Route,
        )
    }
}

@Composable
private fun IntervalGoalContent(
    warmupMode: SegmentMode, onWarmupModeChange: (SegmentMode) -> Unit,
    warmupMinutes: Int, onWarmupMinutesChange: (Int) -> Unit,
    warmupKm: Int, onWarmupKmChange: (Int) -> Unit,
    warmupDecimal: Int, onWarmupDecimalChange: (Int) -> Unit,
    workMode: SegmentMode, onWorkModeChange: (SegmentMode) -> Unit,
    workMinutes: Int, onWorkMinutesChange: (Int) -> Unit,
    workKm: Int, onWorkKmChange: (Int) -> Unit,
    workDecimal: Int, onWorkDecimalChange: (Int) -> Unit,
    workPace: Int, onWorkPaceChange: (Int) -> Unit,
    workHasPace: Boolean, onWorkHasPaceChange: (Boolean) -> Unit,
    recoveryMode: SegmentMode, onRecoveryModeChange: (SegmentMode) -> Unit,
    recoveryMinutes: Int, onRecoveryMinutesChange: (Int) -> Unit,
    recoveryKm: Int, onRecoveryKmChange: (Int) -> Unit,
    recoveryDecimal: Int, onRecoveryDecimalChange: (Int) -> Unit,
    recoveryPace: Int, onRecoveryPaceChange: (Int) -> Unit,
    recoveryHasPace: Boolean, onRecoveryHasPaceChange: (Boolean) -> Unit,
    cooldownMode: SegmentMode, onCooldownModeChange: (SegmentMode) -> Unit,
    cooldownMinutes: Int, onCooldownMinutesChange: (Int) -> Unit,
    cooldownKm: Int, onCooldownKmChange: (Int) -> Unit,
    cooldownDecimal: Int, onCooldownDecimalChange: (Int) -> Unit,
    sets: Int, onSetsChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        IntervalSection(title = "준비운동") {
            SegmentDurationEditor(
                mode = warmupMode,
                onModeChange = onWarmupModeChange,
                minutes = warmupMinutes,
                onMinutesChange = onWarmupMinutesChange,
                km = warmupKm,
                onKmChange = onWarmupKmChange,
                decimal = warmupDecimal,
                onDecimalChange = onWarmupDecimalChange,
            )
        }

        SectionDivider()

        IntervalSection(title = "운동") {
            SegmentDurationEditor(
                mode = workMode,
                onModeChange = onWorkModeChange,
                minutes = workMinutes,
                onMinutesChange = onWorkMinutesChange,
                km = workKm,
                onKmChange = onWorkKmChange,
                decimal = workDecimal,
                onDecimalChange = onWorkDecimalChange,
            )
            PaceEditor(
                title = "운동 페이스",
                enabled = workHasPace,
                onEnabledChange = onWorkHasPaceChange,
                paceSeconds = workPace,
                onPaceChange = onWorkPaceChange,
            )
        }

        SectionDivider()

        IntervalSection(title = "회복") {
            SegmentDurationEditor(
                mode = recoveryMode,
                onModeChange = onRecoveryModeChange,
                minutes = recoveryMinutes,
                onMinutesChange = onRecoveryMinutesChange,
                km = recoveryKm,
                onKmChange = onRecoveryKmChange,
                decimal = recoveryDecimal,
                onDecimalChange = onRecoveryDecimalChange,
            )
            PaceEditor(
                title = "회복 페이스",
                enabled = recoveryHasPace,
                onEnabledChange = onRecoveryHasPaceChange,
                paceSeconds = recoveryPace,
                onPaceChange = onRecoveryPaceChange,
            )
        }

        SectionDivider()

        IntervalSection(title = "쿨다운") {
            SegmentDurationEditor(
                mode = cooldownMode,
                onModeChange = onCooldownModeChange,
                minutes = cooldownMinutes,
                onMinutesChange = onCooldownMinutesChange,
                km = cooldownKm,
                onKmChange = onCooldownKmChange,
                decimal = cooldownDecimal,
                onDecimalChange = onCooldownDecimalChange,
            )
        }

        SectionDivider()

        IntervalSection(title = "총 반복 횟수") {
            val setValues = remember { (1..20).toList() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                WheelPicker(
                    values = setValues,
                    selectedValue = sets,
                    onValueChange = onSetsChange,
                    label = { it.toString() },
                    modifier = Modifier.weight(1f),
                )
                UnitLabel("회")
            }
        }

        Spacer(Modifier.height(16.dp))
        IntervalGoalSummary(
            warmup = segmentLabel(warmupMode, warmupMinutes, warmupKm, warmupDecimal),
            work = segmentLabel(workMode, workMinutes, workKm, workDecimal),
            recovery = segmentLabel(recoveryMode, recoveryMinutes, recoveryKm, recoveryDecimal),
            cooldown = segmentLabel(cooldownMode, cooldownMinutes, cooldownKm, cooldownDecimal),
            sets = sets,
        )
    }
}

@Composable
private fun SegmentDurationEditor(
    mode: SegmentMode,
    onModeChange: (SegmentMode) -> Unit,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    km: Int,
    onKmChange: (Int) -> Unit,
    decimal: Int,
    onDecimalChange: (Int) -> Unit,
) {
    ToggleRow(
        options = SegmentMode.entries.map { it.label },
        selectedIndex = if (mode == SegmentMode.TIME) 0 else 1,
        onSelect = { onModeChange(if (it == 0) SegmentMode.TIME else SegmentMode.DISTANCE) },
    )
    Spacer(Modifier.height(12.dp))
    if (mode == SegmentMode.TIME) {
        MinuteWheelPicker(
            minutes = minutes,
            onMinutesChange = onMinutesChange,
            maxMinutes = 120,
        )
    } else {
        DistanceWheelPicker(
            km = km,
            decimal = decimal,
            onKmChange = onKmChange,
            onDecimalChange = onDecimalChange,
            maxKm = 20,
        )
    }
}

@Composable
private fun PaceEditor(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    paceSeconds: Int,
    onPaceChange: (Int) -> Unit,
) {
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalSectionLabel(title)
        Spacer(Modifier.weight(1f))
        ToggleRow(
            options = listOf("설정", "없음"),
            selectedIndex = if (enabled) 0 else 1,
            onSelect = { onEnabledChange(it == 0) },
        )
    }
    if (enabled) {
        Spacer(Modifier.height(10.dp))
        PaceWheelPicker(
            seconds = paceSeconds,
            onSecondsChange = onPaceChange,
        )
    }
}

@Composable
internal fun TimeWheelPicker(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    val hourValues = remember { (0..5).toList() }
    val minuteValues = remember { (0..59).toList() }
    WheelFrame {
        WheelPicker(
            values = hourValues,
            selectedValue = hours,
            onValueChange = onHoursChange,
            label = { it.toString() },
            modifier = Modifier.weight(1f),
        )
        UnitLabel("시간")
        WheelPicker(
            values = minuteValues,
            selectedValue = minutes,
            onValueChange = { onMinutesChange(it.coerceAtLeast(if (hours == 0) 1 else 0)) },
            label = { it.toString().padStart(2, '0') },
            modifier = Modifier.weight(1f),
        )
        UnitLabel("분")
    }
}

@Composable
private fun MinuteWheelPicker(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    maxMinutes: Int,
) {
    val minuteValues = remember(maxMinutes) { (1..maxMinutes).toList() }
    WheelFrame {
        WheelPicker(
            values = minuteValues,
            selectedValue = minutes.coerceIn(1, maxMinutes),
            onValueChange = onMinutesChange,
            label = { it.toString() },
            modifier = Modifier.weight(1f),
        )
        UnitLabel("분")
    }
}

@Composable
internal fun DistanceWheelPicker(
    km: Int,
    decimal: Int,
    onKmChange: (Int) -> Unit,
    onDecimalChange: (Int) -> Unit,
    maxKm: Int = 50,
) {
    val kmValues = remember(maxKm) { (0..maxKm).toList() }
    val decimalValues = remember { (0..99).toList() }
    WheelFrame {
        WheelPicker(
            values = kmValues,
            selectedValue = km.coerceIn(0, maxKm),
            onValueChange = { value ->
                onKmChange(value)
                if (value == 0 && decimal == 0) onDecimalChange(10)
            },
            label = { it.toString() },
            modifier = Modifier.weight(1f),
        )
        UnitLabel("km")
        Text(
            text = ".",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        WheelPicker(
            values = decimalValues,
            selectedValue = decimal.coerceIn(0, 99),
            onValueChange = { value ->
                onDecimalChange(value)
                if (km == 0 && value == 0) onDecimalChange(10)
            },
            label = { it.toString().padStart(2, '0') },
            modifier = Modifier.weight(1f),
        )
        UnitLabel("km")
    }
}

@Composable
internal fun DateWheelPicker(
    date: java.time.LocalDate,
    onDateChange: (java.time.LocalDate) -> Unit,
) {
    val today = remember { java.time.LocalDate.now() }
    val yearValues = remember(today) { (today.year..today.year + 5).toList() }
    val monthValues = remember { (1..12).toList() }
    val maxDay = remember(date.year, date.monthValue) {
        java.time.YearMonth.of(date.year, date.monthValue).lengthOfMonth()
    }
    val dayValues = remember(maxDay) { (1..maxDay).toList() }

    WheelFrame {
        WheelPicker(
            values = yearValues,
            selectedValue = date.year.coerceIn(yearValues.first(), yearValues.last()),
            onValueChange = { year ->
                val day = date.dayOfMonth.coerceAtMost(
                    java.time.YearMonth.of(year, date.monthValue).lengthOfMonth(),
                )
                onDateChange(java.time.LocalDate.of(year, date.monthValue, day))
            },
            label = { it.toString() },
            modifier = Modifier.weight(1.35f),
        )
        UnitLabel("년")
        WheelPicker(
            values = monthValues,
            selectedValue = date.monthValue,
            onValueChange = { month ->
                val day = date.dayOfMonth.coerceAtMost(
                    java.time.YearMonth.of(date.year, month).lengthOfMonth(),
                )
                onDateChange(java.time.LocalDate.of(date.year, month, day))
            },
            label = { it.toString().padStart(2, '0') },
            modifier = Modifier.weight(0.85f),
        )
        UnitLabel("월")
        WheelPicker(
            values = dayValues,
            selectedValue = date.dayOfMonth.coerceAtMost(maxDay),
            onValueChange = { day ->
                onDateChange(java.time.LocalDate.of(date.year, date.monthValue, day))
            },
            label = { it.toString().padStart(2, '0') },
            modifier = Modifier.weight(0.85f),
        )
        UnitLabel("일")
    }
}

@Composable
private fun PaceWheelPicker(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
) {
    val minutes = (seconds / 60).coerceIn(3, 12)
    val sec = ((seconds % 60) / 5 * 5).coerceIn(0, 55)
    val minuteValues = remember { (3..12).toList() }
    val secondValues = remember { (0..55 step 5).toList() }
    WheelFrame {
        WheelPicker(
            values = minuteValues,
            selectedValue = minutes,
            onValueChange = { onSecondsChange(it * 60 + sec) },
            label = { it.toString() },
            modifier = Modifier.weight(1f),
        )
        UnitLabel("분")
        WheelPicker(
            values = secondValues,
            selectedValue = sec,
            onValueChange = { onSecondsChange(minutes * 60 + it) },
            label = { it.toString().padStart(2, '0') },
            modifier = Modifier.weight(1f),
        )
        UnitLabel("/km")
    }
}

@Composable
private fun <T> WheelPicker(
    values: List<T>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 52.dp,
) {
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val centerIndex by remember {
        derivedStateOf {
            val offsetItems = (listState.firstVisibleItemScrollOffset / itemHeightPx).roundToInt()
            (listState.firstVisibleItemIndex + offsetItems).coerceIn(values.indices)
        }
    }

    LaunchedEffect(listState, values) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { index -> onValueChange(values[index]) }
    }

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling && values.isNotEmpty()) {
                    listState.animateScrollToItem(centerIndex)
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(itemHeight * 3),
        contentPadding = PaddingValues(vertical = itemHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(values.size) { index ->
            val selected = index == centerIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable {
                        scope.launch { listState.animateScrollToItem(index) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(values[index]),
                    fontSize = if (selected) 34.sp else 20.sp,
                    lineHeight = if (selected) 38.sp else 24.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WheelFrame(content: @Composable RowScope.() -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .goalCardBorder(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 선택 하이라이트 바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .align(Alignment.Center)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.02f),
                                primary.copy(alpha = 0.12f),
                                primary.copy(alpha = 0.02f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = primary.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(10.dp),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content,
            )
        }
    }
}

@Composable
private fun UnitLabel(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun IntervalSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun GoalSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ToggleRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        options.forEachIndexed { idx, label ->
            val active = idx == selectedIndex
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 15.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SimpleGoalSummary(
    title: String,
    value: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .goalCardBorder(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalGoalSummary(
    warmup: String,
    work: String,
    recovery: String,
    cooldown: String,
    sets: Int,
) {
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .goalCardBorder(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "인터벌 훈련 순서",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            IntervalSummaryStep(
                number = "1",
                title = "준비운동",
                detail = warmup,
                icon = Icons.Filled.Timer,
            )
            IntervalSummaryStep(
                number = "2",
                title = "운동 $work  +  회복 $recovery",
                detail = "${sets}세트 반복",
                icon = Icons.Filled.Repeat,
                emphasized = true,
            )
            IntervalSummaryStep(
                number = "3",
                title = "쿨다운",
                detail = cooldown,
                icon = Icons.Filled.Flag,
            )
        }
    }
}

@Composable
private fun IntervalSummaryStep(
    number: String,
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emphasized: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = if (emphasized) 0.22f else 0.12f,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier.width(19.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = if (emphasized) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun Modifier.goalCardBorder(shape: Shape): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    return border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                primary.copy(alpha = 0.65f),
                primary.copy(alpha = 0.15f),
                primary.copy(alpha = 0.08f),
            ),
        ),
        shape = shape,
    )
}

private fun segmentDuration(
    mode: SegmentMode,
    minutes: Int,
    km: Int,
    decimal: Int,
): IntervalDuration = when (mode) {
    SegmentMode.TIME -> IntervalDuration.ByTime(minutes.coerceAtLeast(1))
    SegmentMode.DISTANCE -> IntervalDuration.ByDistance(distanceMeters(km, decimal))
}

private fun segmentLabel(mode: SegmentMode, minutes: Int, km: Int, decimal: Int): String =
    when (mode) {
        SegmentMode.TIME -> timeLabel(minutes.coerceAtLeast(1))
        SegmentMode.DISTANCE -> distanceLabel(distanceMeters(km, decimal))
    }

private fun distanceKm(km: Int, decimal: Int): Float =
    (distanceMeters(km, decimal) / 1000f)

private fun distanceMeters(km: Int, decimal: Int): Int =
    ((km.coerceAtLeast(0) * 1000) + (decimal.coerceIn(0, 99) * 10)).coerceAtLeast(100)

private fun distanceLabel(meters: Int): String {
    val km = meters / 1000
    val decimal = (meters % 1000) / 10
    return if (decimal == 0) "${km}km" else "%d.%02dkm".format(km, decimal)
}

private fun timeLabel(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        else -> "${minutes}분"
    }
}
