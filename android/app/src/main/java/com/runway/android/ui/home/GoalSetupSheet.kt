package com.runway.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runway.android.ui.components.RunwayPrimaryButton

private val Lime = Color(0xFFA4E168)
private val SheetBg = Color(0xFF16171F)
private val ChipBg = Color(0xFF1E1F2A)
private val DividerColor = Color(0xFF2A2B38)

// ─── Entry point ─────────────────────────────────────────────────────────────

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

// ─── Content ─────────────────────────────────────────────────────────────────

private enum class GoalTab(val label: String) { TIME("시간별"), DISTANCE("거리별"), INTERVAL("인터벌") }

@Composable
private fun GoalSetupContent(
    onDismiss: () -> Unit,
    onConfirm: (RunGoal) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(GoalTab.DISTANCE) }

    // ── 시간별 state ──
    var selectedMinutes by remember { mutableIntStateOf(30) }

    // ── 거리별 state ──
    var selectedKm by remember { mutableStateOf(5f) }

    // ── 인터벌 state ──
    var warmupType  by remember { mutableStateOf<WarmupType>(WarmupType.TIME) }
    var warmupTime  by remember { mutableIntStateOf(10) }          // minutes
    var warmupDist  by remember { mutableIntStateOf(1000) }        // metres

    var workDistM   by remember { mutableIntStateOf(1000) }        // metres
    var workPace    by remember { mutableIntStateOf(300) }         // sec/km
    var workHasPace by remember { mutableStateOf(true) }

    var recovDistM   by remember { mutableIntStateOf(400) }        // metres
    var recovPace    by remember { mutableIntStateOf(420) }        // sec/km
    var recovHasPace by remember { mutableStateOf(false) }

    var sets by remember { mutableIntStateOf(3) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Title ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "목표 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color.White.copy(alpha = 0.5f))
            }
        }

        HorizontalDivider(color = DividerColor)

        // ── Tab row ─────────────────────────────────────────────────────────
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
                        .background(if (active) Lime else ChipBg)
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        HorizontalDivider(color = DividerColor)

        // ── Tab content ─────────────────────────────────────────────────────
        when (selectedTab) {
            GoalTab.TIME ->
                TimeGoalContent(
                    selectedMinutes = selectedMinutes,
                    onSelect = { selectedMinutes = it },
                )
            GoalTab.DISTANCE ->
                DistanceGoalContent(
                    selectedKm = selectedKm,
                    onSelect = { selectedKm = it },
                )
            GoalTab.INTERVAL ->
                IntervalGoalContent(
                    warmupType = warmupType,
                    onWarmupTypeChange = { warmupType = it },
                    warmupTime = warmupTime,
                    onWarmupTimeChange = { warmupTime = it },
                    warmupDist = warmupDist,
                    onWarmupDistChange = { warmupDist = it },
                    workDistM = workDistM,
                    onWorkDistChange = { workDistM = it },
                    workPace = workPace,
                    onWorkPaceChange = { workPace = it },
                    workHasPace = workHasPace,
                    onWorkHasPaceChange = { workHasPace = it },
                    recovDistM = recovDistM,
                    onRecovDistChange = { recovDistM = it },
                    recovPace = recovPace,
                    onRecovPaceChange = { recovPace = it },
                    recovHasPace = recovHasPace,
                    onRecovHasPaceChange = { recovHasPace = it },
                    sets = sets,
                    onSetsChange = { sets = it },
                )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(Modifier.height(12.dp))

        // ── Confirm button ──────────────────────────────────────────────────
        RunwayPrimaryButton(
            text = "시작하기",
            onClick = {
                val goal = when (selectedTab) {
                    GoalTab.TIME     -> RunGoal.TimeGoal(selectedMinutes)
                    GoalTab.DISTANCE -> RunGoal.DistanceGoal(selectedKm)
                    GoalTab.INTERVAL -> RunGoal.IntervalGoal(
                        warmup = IntervalSegment(
                            duration = if (warmupType == WarmupType.TIME)
                                IntervalDuration.ByTime(warmupTime)
                            else IntervalDuration.ByDistance(warmupDist),
                        ),
                        work = IntervalSegment(
                            duration = IntervalDuration.ByDistance(workDistM),
                            paceTargetSecPerKm = if (workHasPace) workPace else null,
                        ),
                        recovery = IntervalSegment(
                            duration = IntervalDuration.ByDistance(recovDistM),
                            paceTargetSecPerKm = if (recovHasPace) recovPace else null,
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

// ─── Time goal ────────────────────────────────────────────────────────────────

@Composable
private fun TimeGoalContent(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
        GoalSectionLabel("목표 시간")
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GoalPresets.timeMinutes) { min ->
                val label = if (min >= 60) "${min / 60}시간 ${if (min % 60 != 0) "${min % 60}분" else ""}".trim()
                            else "${min}분"
                SelectChip(label = label, selected = selectedMinutes == min, onClick = { onSelect(min) })
            }
        }
        Spacer(Modifier.height(24.dp))
        GoalSummaryBox(text = "${selectedMinutes}분 동안 달리기")
    }
}

// ─── Distance goal ────────────────────────────────────────────────────────────

@Composable
private fun DistanceGoalContent(selectedKm: Float, onSelect: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
        GoalSectionLabel("목표 거리")
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GoalPresets.distanceKm) { km ->
                val label = if (km == km.toInt().toFloat()) "${km.toInt()}km" else "${km}km"
                SelectChip(label = label, selected = selectedKm == km, onClick = { onSelect(km) })
            }
        }
        Spacer(Modifier.height(24.dp))
        GoalSummaryBox(
            text = if (selectedKm == selectedKm.toInt().toFloat())
                "${selectedKm.toInt()}km 달리기"
            else "${selectedKm}km 달리기"
        )
    }
}

// ─── Interval goal ────────────────────────────────────────────────────────────

private enum class WarmupType { TIME, DISTANCE }

@Composable
private fun IntervalGoalContent(
    warmupType: WarmupType, onWarmupTypeChange: (WarmupType) -> Unit,
    warmupTime: Int, onWarmupTimeChange: (Int) -> Unit,
    warmupDist: Int, onWarmupDistChange: (Int) -> Unit,
    workDistM: Int, onWorkDistChange: (Int) -> Unit,
    workPace: Int, onWorkPaceChange: (Int) -> Unit,
    workHasPace: Boolean, onWorkHasPaceChange: (Boolean) -> Unit,
    recovDistM: Int, onRecovDistChange: (Int) -> Unit,
    recovPace: Int, onRecovPaceChange: (Int) -> Unit,
    recovHasPace: Boolean, onRecovHasPaceChange: (Boolean) -> Unit,
    sets: Int, onSetsChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {

        // ── 준비운동 ────────────────────────────────────────────────────────
        IntervalSection(title = "준비운동") {
            ToggleRow(
                options = listOf("시간", "거리"),
                selectedIndex = if (warmupType == WarmupType.TIME) 0 else 1,
                onSelect = { onWarmupTypeChange(if (it == 0) WarmupType.TIME else WarmupType.DISTANCE) },
            )
            Spacer(Modifier.height(8.dp))
            if (warmupType == WarmupType.TIME) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GoalPresets.warmupTimes) { min ->
                        SelectChip("${min}분", warmupTime == min) { onWarmupTimeChange(min) }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GoalPresets.warmupDistances) { m ->
                        SelectChip(distLabel(m), warmupDist == m) { onWarmupDistChange(m) }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(Modifier.height(4.dp))

        // ── 운동 ───────────────────────────────────────────────────────────
        IntervalSection(title = "운동") {
            GoalSectionLabel("거리")
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GoalPresets.workDistances) { m ->
                    SelectChip(distLabel(m), workDistM == m) { onWorkDistChange(m) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalSectionLabel("페이스 목표")
                Spacer(Modifier.weight(1f))
                ToggleRow(
                    options = listOf("설정", "없음"),
                    selectedIndex = if (workHasPace) 0 else 1,
                    onSelect = { onWorkHasPaceChange(it == 0) },
                )
            }
            if (workHasPace) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GoalPresets.workPaces) { pace ->
                        SelectChip(GoalPresets.paceLabel(pace), workPace == pace) { onWorkPaceChange(pace) }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(Modifier.height(4.dp))

        // ── 회복 ───────────────────────────────────────────────────────────
        IntervalSection(title = "회복") {
            GoalSectionLabel("거리")
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GoalPresets.recoveryDistances) { m ->
                    SelectChip(distLabel(m), recovDistM == m) { onRecovDistChange(m) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalSectionLabel("페이스 목표")
                Spacer(Modifier.weight(1f))
                ToggleRow(
                    options = listOf("설정", "없음"),
                    selectedIndex = if (recovHasPace) 0 else 1,
                    onSelect = { onRecovHasPaceChange(it == 0) },
                )
            }
            if (recovHasPace) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GoalPresets.recoveryPaces) { pace ->
                        SelectChip(GoalPresets.paceLabel(pace), recovPace == pace) { onRecovPaceChange(pace) }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(Modifier.height(4.dp))

        // ── 반복 횟수 ───────────────────────────────────────────────────────
        IntervalSection(title = "반복 횟수") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GoalPresets.sets) { n ->
                    SelectChip("${n}세트", sets == n) { onSetsChange(n) }
                }
            }
        }

        // ── Summary ─────────────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        val warmupLabel = if (warmupType == WarmupType.TIME) "${warmupTime}분" else distLabel(warmupDist)
        val workLabel = distLabel(workDistM) + if (workHasPace) " @ ${GoalPresets.paceLabel(workPace)}" else ""
        val recovLabel = distLabel(recovDistM) + if (recovHasPace) " @ ${GoalPresets.paceLabel(recovPace)}" else ""
        GoalSummaryBox(text = "준비 $warmupLabel → 운동 $workLabel × ${sets}세트 → 회복 $recovLabel")
    }
}

// ─── Small helpers ────────────────────────────────────────────────────────────

private fun distLabel(metres: Int): String =
    if (metres >= 1000) "${"%.1f".format(metres / 1000f)}km" else "${metres}m"

@Composable
private fun IntervalSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
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
        color = Color.White.copy(alpha = 0.55f),
    )
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor   = if (selected) Lime else ChipBg
    val textColor = if (selected) Color(0xFF0A0B10) else Color.White.copy(alpha = 0.75f)
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (!selected) Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ToggleRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ChipBg),
    ) {
        options.forEachIndexed { idx, label ->
            val active = idx == selectedIndex
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) Color(0xFF0A0B10) else Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Lime else Color.Transparent)
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun GoalSummaryBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Lime.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Lime.copy(alpha = 0.85f),
        )
    }
}
