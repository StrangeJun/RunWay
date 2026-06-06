package com.runway.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runway.wear.WatchViewModel
import com.runway.wear.model.RunGoal
import com.runway.wear.model.WatchRunState
import com.runway.wear.model.WatchScreen
import com.runway.wear.model.formatDuration
import com.runway.wear.model.formatPace

private val Background = Color(0xFF090B0F)
private val SurfaceColor = Color(0xFF171A20)
private val Accent = Color(0xFF5EE18A)
private val Muted = Color(0xFF9AA3AF)
private val Danger = Color(0xFFFF665E)

@Composable
fun PathFinderWearApp(viewModel: WatchViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Accent,
            surface = SurfaceColor,
            background = Background,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            when (state.screen) {
                WatchScreen.HOME -> HomeScreen(state, viewModel::start) {
                    viewModel.navigate(WatchScreen.GOAL_TYPE)
                }
                WatchScreen.GOAL_TYPE -> GoalTypeScreen(viewModel)
                WatchScreen.TIME_GOAL -> TimeGoalScreen(viewModel)
                WatchScreen.DISTANCE_GOAL -> DistanceGoalScreen(viewModel)
                WatchScreen.INTERVAL_GOAL -> IntervalGoalScreen(viewModel)
                WatchScreen.TRACKING -> TrackingScreen(state, viewModel::pause)
                WatchScreen.PAUSED -> PausedScreen(state, viewModel)
                WatchScreen.SUMMARY -> SummaryScreen(state, viewModel::returnHome)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: WatchRunState,
    onStart: (RunGoal) -> Unit,
    onGoal: () -> Unit,
) {
    WatchPage {
        Text("PathFinder", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        ConnectionLabel(state.isPhoneConnected)
        PrimaryAction(
            label = "바로 시작",
            icon = Icons.Filled.PlayArrow,
            onClick = { onStart(RunGoal.Free) },
        )
        SecondaryAction("목표 설정", Icons.Filled.Flag, onGoal)
        Text(
            if (state.isPhoneConnected) "폰 연결됨 · 독립 GPS 준비" else "독립 GPS 모드 · 동기화 대기",
            color = Muted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        state.phoneStatusMessage?.let {
            Text(it, color = Danger, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GoalTypeScreen(viewModel: WatchViewModel) {
    WatchPage {
        BackTitle("목표 선택") { viewModel.navigate(WatchScreen.HOME) }
        SecondaryAction("시간", Icons.Filled.Timer, onClick = {
            viewModel.navigate(WatchScreen.TIME_GOAL)
        })
        SecondaryAction("거리", Icons.Filled.Route, onClick = {
            viewModel.navigate(WatchScreen.DISTANCE_GOAL)
        })
        SecondaryAction("인터벌", Icons.Filled.DirectionsRun, onClick = {
            viewModel.navigate(WatchScreen.INTERVAL_GOAL)
        })
    }
}

@Composable
private fun TimeGoalScreen(viewModel: WatchViewModel) {
    var minutes by remember { mutableIntStateOf(30) }
    PickerPage(
        title = "시간 목표",
        value = "$minutes 분",
        summary = "${minutes}분 동안 달리기",
        onMinus = { minutes = (minutes - 5).coerceAtLeast(5) },
        onPlus = { minutes = (minutes + 5).coerceAtMost(360) },
        onBack = { viewModel.navigate(WatchScreen.GOAL_TYPE) },
        onStart = { viewModel.start(RunGoal.Time(minutes)) },
    )
}

@Composable
private fun DistanceGoalScreen(viewModel: WatchViewModel) {
    var halfKmUnits by remember { mutableIntStateOf(10) }
    val km = halfKmUnits / 2f
    PickerPage(
        title = "거리 목표",
        value = "%.1f km".format(km),
        summary = "%.1fkm 달리기".format(km),
        onMinus = { halfKmUnits = (halfKmUnits - 1).coerceAtLeast(1) },
        onPlus = { halfKmUnits = (halfKmUnits + 1).coerceAtMost(100) },
        onBack = { viewModel.navigate(WatchScreen.GOAL_TYPE) },
        onStart = { viewModel.start(RunGoal.Distance((km * 1000).toInt())) },
    )
}

@Composable
private fun IntervalGoalScreen(viewModel: WatchViewModel) {
    var workMinutes by remember { mutableIntStateOf(5) }
    var restMinutes by remember { mutableIntStateOf(2) }
    var sets by remember { mutableIntStateOf(4) }
    WatchPage(scrollable = true) {
        BackTitle("인터벌") { viewModel.navigate(WatchScreen.GOAL_TYPE) }
        CompactStepper("운동", "$workMinutes 분",
            { workMinutes = (workMinutes - 1).coerceAtLeast(1) },
            { workMinutes = (workMinutes + 1).coerceAtMost(60) })
        CompactStepper("휴식", "$restMinutes 분",
            { restMinutes = (restMinutes - 1).coerceAtLeast(1) },
            { restMinutes = (restMinutes + 1).coerceAtMost(30) })
        CompactStepper("반복", "$sets 회",
            { sets = (sets - 1).coerceAtLeast(1) },
            { sets = (sets + 1).coerceAtMost(20) })
        PrimaryAction("시작", Icons.Filled.PlayArrow) {
            viewModel.start(RunGoal.Interval(workMinutes * 60, restMinutes * 60, sets))
        }
    }
}

@Composable
private fun TrackingScreen(state: WatchRunState, onPause: () -> Unit) {
    WatchPage {
        Text(
            state.remainingLabel ?: "자유 러닝",
            color = if (state.gpsStatus == "POOR") Danger else Accent,
            fontSize = 12.sp,
        )
        Text(
            formatDuration(state.elapsedSeconds),
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Metric("페이스", formatPace(state.paceMinPerKm))
            Metric("거리", "%.2fkm".format(state.distanceMeters / 1000.0))
        }
        state.progressPercent?.let { progress ->
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Accent,
                trackColor = Color.White.copy(alpha = 0.12f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("심박", state.heartRateBpm?.let { "$it" } ?: "--")
            Metric("케이던스", state.cadenceSpm?.let { "$it" } ?: "--")
        }
        state.phoneStatusMessage?.let {
            Text(it, color = Danger, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
        IconButton(
            onClick = onPause,
            modifier = Modifier.size(54.dp).background(Color.White.copy(alpha = 0.12f), CircleShape),
        ) {
            Icon(Icons.Filled.Pause, contentDescription = "일시정지", tint = Color.White)
        }
    }
}

@Composable
private fun PausedScreen(state: WatchRunState, viewModel: WatchViewModel) {
    WatchPage {
        Text("일시정지", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "${formatDuration(state.elapsedSeconds)} · %.2fkm".format(state.distanceMeters / 1000.0),
            color = Muted,
        )
        PrimaryAction("계속하기", Icons.Filled.PlayArrow, viewModel::resume)
        SecondaryAction("런 완료", Icons.Filled.Check, viewModel::finish)
        SecondaryAction("포기", Icons.Filled.Close, viewModel::abandon, Danger)
    }
}

@Composable
private fun SummaryScreen(state: WatchRunState, onDone: () -> Unit) {
    WatchPage {
        Text("런 완료", color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "%.2f km".format(state.distanceMeters / 1000.0),
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
        )
        Text(formatDuration(state.elapsedSeconds), fontSize = 18.sp)
        Text("평균 페이스 ${formatPace(state.paceMinPerKm)}/km", color = Muted, fontSize = 12.sp)
        Text("상세 기록은 폰에서 확인하세요", color = Muted, fontSize = 11.sp)
        PrimaryAction("완료", Icons.Filled.Check, onDone)
    }
}

@Composable
private fun PickerPage(
    title: String,
    value: String,
    summary: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    WatchPage {
        BackTitle(title, onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Filled.Remove, "감소", onMinus)
            Text(
                value,
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            RoundIcon(Icons.Filled.Add, "증가", onPlus)
        }
        Text(summary, color = Muted, fontSize = 12.sp)
        PrimaryAction("시작하기", Icons.Filled.PlayArrow, onStart)
    }
}

@Composable
private fun CompactStepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(label, color = Muted, fontSize = 10.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Row {
            RoundIcon(Icons.Filled.Remove, "$label 감소", onMinus, 34)
            RoundIcon(Icons.Filled.Add, "$label 증가", onPlus, 34)
        }
    }
}

@Composable
private fun WatchPage(
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 18.dp)
    Column(
        modifier = if (scrollable) base.verticalScroll(rememberScrollState()) else base,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        content = content,
    )
}

@Composable
private fun BackTitle(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "뒤로")
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun ConnectionLabel(connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (connected) Accent else Muted, CircleShape),
        )
        Spacer(Modifier.size(5.dp))
        Text(if (connected) "폰 연결됨" else "오프라인 가능", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun PrimaryAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: Color = Color.White,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceColor,
            contentColor = color,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.size(6.dp))
        Text(label)
    }
}

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Int = 42,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size.dp).background(SurfaceColor, CircleShape),
    ) {
        Icon(icon, contentDescription = description, tint = Accent)
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 10.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
