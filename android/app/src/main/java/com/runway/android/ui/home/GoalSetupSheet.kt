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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runway.android.ui.components.RunwayPrimaryButton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SheetBg = Color(0xFF16171F)
private val ChipBg = Color(0xFF1E1F2A)
private val DividerColor = Color(0xFF2A2B38)

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
        containerColor = SheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp)),
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
                color = Color.White,
            )
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color.White.copy(alpha = 0.5f))
            }
        }

        HorizontalDivider(color = DividerColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GoalTab.entries.forEach { tab ->
                val active = tab == selectedTab
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color(0xFF0A0B10) else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) MaterialTheme.colorScheme.primary else ChipBg)
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }

        HorizontalDivider(color = DividerColor)

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
        HorizontalDivider(color = DividerColor)
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
        GoalSummaryBox(text = "${timeLabel(totalMinutes)} 동안 달리기")
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
        GoalSummaryBox(text = "${distanceLabel(distanceMeters(km, decimal))} 달리기")
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
        GoalSummaryBox(
            text = "준비 ${segmentLabel(warmupMode, warmupMinutes, warmupKm, warmupDecimal)} → " +
                "[운동 ${segmentLabel(workMode, workMinutes, workKm, workDecimal)} + " +
                "회복 ${segmentLabel(recoveryMode, recoveryMinutes, recoveryKm, recoveryDecimal)}] × ${sets}회 → " +
                "쿨다운 ${segmentLabel(cooldownMode, cooldownMinutes, cooldownKm, cooldownDecimal)}",
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
private fun TimeWheelPicker(
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
private fun DistanceWheelPicker(
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
            color = Color.White.copy(alpha = 0.85f),
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
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.28f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WheelFrame(content: @Composable RowScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(166.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .align(Alignment.Center)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
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
    HorizontalDivider(color = DividerColor)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun IntervalSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
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
        color = Color.White.copy(alpha = 0.58f),
    )
}

@Composable
private fun ToggleRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ChipBg),
    ) {
        options.forEachIndexed { idx, label ->
            val active = idx == selectedIndex
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) Color(0xFF0A0B10) else Color.White.copy(alpha = 0.58f),
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
private fun GoalSummaryBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
        )
    }
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
