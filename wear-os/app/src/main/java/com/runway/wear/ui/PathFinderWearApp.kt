package com.runway.wear.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import com.runway.wear.R
import com.runway.wear.WatchViewModel
import com.runway.wear.model.GoalCompletionAction
import com.runway.wear.model.IntervalTarget
import com.runway.wear.model.PhoneAuthState
import com.runway.wear.model.RunGoal
import com.runway.wear.model.WatchRunState
import com.runway.wear.model.WatchScreen
import com.runway.wear.model.formatDuration
import com.runway.wear.model.formatPace
import kotlinx.coroutines.launch

private val Background = Color(0xFF090B0F)
private val SurfaceColor = Color(0xFF171A20)
private val Accent = Color(0xFFB8FF00)
private val Muted = Color(0xFF9AA3AF)
private val Danger = Color(0xFFFF665E)

@Composable
fun PathFinderWearApp(viewModel: WatchViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(
        enabled = state.phoneAuthState == PhoneAuthState.LOGGED_IN &&
            state.screen != WatchScreen.HOME,
        onBack = viewModel::navigateBack,
    )
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
            when (state.phoneAuthState) {
                PhoneAuthState.CHECKING -> AuthCheckingScreen()
                PhoneAuthState.LOGGED_OUT -> PhoneLoginScreen(
                    isPhoneConnected = state.isPhoneConnected,
                    message = state.authMessage,
                    onOpenPhone = viewModel::openPhoneLogin,
                    onRetry = viewModel::refreshAuthState,
                )
                PhoneAuthState.LOGGED_IN -> when (state.screen) {
                WatchScreen.HOME -> HomeScreen(state, viewModel::start) {
                    viewModel.navigate(WatchScreen.GOAL_TYPE)
                }
                WatchScreen.GOAL_TYPE -> GoalTypeScreen(viewModel)
                WatchScreen.TIME_GOAL -> TimeGoalScreen(viewModel)
                WatchScreen.DISTANCE_GOAL -> DistanceGoalScreen(viewModel)
                WatchScreen.INTERVAL_GOAL -> IntervalGoalScreen(viewModel)
                WatchScreen.TRACKING -> TrackingScreen(state, viewModel)
                WatchScreen.PAUSED -> PausedScreen(state, viewModel)
                WatchScreen.SUMMARY -> SummaryScreen(state, viewModel::returnHome)
                }
            }
        }
    }
}

@Composable
private fun AuthCheckingScreen() {
    WatchPage(scrollable = false) {
        Image(
            painter = painterResource(R.drawable.app_logo_mark),
            contentDescription = "PathFinder",
            modifier = Modifier.size(if (compact) 42.dp else 56.dp),
        )
        Text("로그인 확인 중", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("휴대폰과 연결하고 있습니다", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun PhoneLoginScreen(
    isPhoneConnected: Boolean,
    message: String?,
    onOpenPhone: () -> Unit,
    onRetry: () -> Unit,
) {
    WatchPage(scrollable = true) {
        Image(
            painter = painterResource(R.drawable.app_logo_mark),
            contentDescription = "PathFinder",
            modifier = Modifier.size(if (compact) 38.dp else 52.dp),
        )
        Text("휴대폰 로그인이 필요해요", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            if (isPhoneConnected) {
                "휴대폰 앱에서 로그인하면 워치에서 바로 시작할 수 있습니다"
            } else {
                "휴대폰과 워치의 연결을 확인해 주세요"
            },
            color = Muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
        PrimaryAction(
            label = "휴대폰에서 로그인",
            icon = Icons.Filled.PhoneAndroid,
            onClick = onOpenPhone,
            compact = compact,
        )
        message?.let { StatusText(it) }
        Text(
            text = "로그인 확인",
            color = Accent,
            fontSize = 11.sp,
            modifier = Modifier.clickable(onClick = onRetry).padding(6.dp),
        )
    }
}

@Composable
private fun HomeScreen(
    state: WatchRunState,
    onStart: (RunGoal) -> Unit,
    onGoal: () -> Unit,
) {
    WatchPage(scrollable = true) {
        Image(
            painter = painterResource(R.drawable.app_logo_mark),
            contentDescription = "PathFinder",
            modifier = Modifier.size(if (compact) 34.dp else 52.dp),
        )
        Text("PathFinder", fontWeight = FontWeight.Bold, fontSize = if (compact) 16.sp else 18.sp)
        ConnectionLabel(state.isPhoneConnected)
        PrimaryAction(
            label = "바로 시작",
            icon = Icons.Filled.PlayArrow,
            onClick = { onStart(RunGoal.Free) },
            compact = compact,
        )
        SecondaryAction("목표 설정", Icons.Filled.Flag, onClick = onGoal, compact = compact)
        Text(
            if (state.isPhoneConnected) "폰 연결됨 · 독립 GPS 준비" else "독립 GPS 모드 · 동기화 대기",
            color = Muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
        state.phoneStatusMessage?.let { StatusText(it) }
    }
}

@Composable
private fun GoalTypeScreen(viewModel: WatchViewModel) {
    WatchPage(scrollable = true) {
        BackTitle("목표 선택", viewModel::navigateBack)
        SecondaryAction("시간", Icons.Filled.Timer, onClick = {
            viewModel.navigate(WatchScreen.TIME_GOAL)
        })
        SecondaryAction("거리", Icons.Filled.Route, onClick = {
            viewModel.navigate(WatchScreen.DISTANCE_GOAL)
        })
        SecondaryAction("인터벌", Icons.AutoMirrored.Filled.DirectionsRun, onClick = {
            viewModel.navigate(WatchScreen.INTERVAL_GOAL)
        })
    }
}

@Composable
private fun TimeGoalScreen(viewModel: WatchViewModel) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    var completion by remember { mutableStateOf(GoalCompletionAction.PAUSE) }
    var selected by remember { mutableStateOf<String?>(null) }

    RotarySettingPage(
        title = "시간 목표",
        selected = selected,
        onBack = viewModel::navigateBack,
        onRotate = { delta ->
            when (selected) {
                "hours" -> hours = wrap(hours, delta, 0, 5)
                "minutes" -> minutes = wrap(minutes, delta * 5, 0, 55, 5)
            }
        },
    ) {
        SettingLabel("목표 시간")
        Row(verticalAlignment = Alignment.CenterVertically) {
            DialValue("$hours", "시간", selected == "hours") {
                selected = toggleSelection(selected, "hours")
            }
            Text(":", color = Muted, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 4.dp))
            DialValue("%02d".format(minutes), "분", selected == "minutes") {
                selected = toggleSelection(selected, "minutes")
            }
        }
        Text("${hours * 60 + minutes}분 동안 달리기", color = Muted, fontSize = 11.sp)
        CompletionChoice(completion) { completion = it }
        PrimaryAction("시작하기", Icons.Filled.PlayArrow) {
            val totalMinutes = (hours * 60 + minutes).coerceAtLeast(1)
            viewModel.start(RunGoal.Time(totalMinutes, completion))
        }
    }
}

@Composable
private fun DistanceGoalScreen(viewModel: WatchViewModel) {
    var km by remember { mutableIntStateOf(5) }
    var decimal by remember { mutableIntStateOf(0) }
    var completion by remember { mutableStateOf(GoalCompletionAction.PAUSE) }
    var selected by remember { mutableStateOf<String?>(null) }

    RotarySettingPage(
        title = "거리 목표",
        selected = selected,
        onBack = viewModel::navigateBack,
        onRotate = { delta ->
            when (selected) {
                "km" -> km = wrap(km, delta, 0, 50)
                "decimal" -> decimal = wrap(decimal, delta, 0, 9)
            }
        },
    ) {
        SettingLabel("목표 거리")
        Row(verticalAlignment = Alignment.CenterVertically) {
            DialValue("$km", "km", selected == "km") {
                selected = toggleSelection(selected, "km")
            }
            Text(".", color = Muted, fontSize = 26.sp)
            DialValue("$decimal", "100m", selected == "decimal") {
                selected = toggleSelection(selected, "decimal")
            }
        }
        Text("%.1fkm 달리기".format(km + decimal / 10f), color = Muted, fontSize = 11.sp)
        CompletionChoice(completion) { completion = it }
        PrimaryAction("시작하기", Icons.Filled.PlayArrow) {
            val meters = (km * 1000 + decimal * 100).coerceAtLeast(100)
            viewModel.start(RunGoal.Distance(meters, completion))
        }
    }
}

private enum class SegmentMode { TIME, DISTANCE }

@Composable
private fun IntervalGoalScreen(viewModel: WatchViewModel) {
    var workMode by remember { mutableStateOf(SegmentMode.TIME) }
    var workMinutes by remember { mutableIntStateOf(3) }
    var workSeconds by remember { mutableIntStateOf(0) }
    var workKm by remember { mutableIntStateOf(0) }
    var workDecimal by remember { mutableIntStateOf(4) }
    var recoveryMode by remember { mutableStateOf(SegmentMode.TIME) }
    var recoveryMinutes by remember { mutableIntStateOf(1) }
    var recoverySeconds by remember { mutableIntStateOf(0) }
    var recoveryKm by remember { mutableIntStateOf(0) }
    var recoveryDecimal by remember { mutableIntStateOf(2) }
    var sets by remember { mutableIntStateOf(4) }
    var completion by remember { mutableStateOf(GoalCompletionAction.PAUSE) }
    var selected by remember { mutableStateOf<String?>(null) }

    RotarySettingPage(
        title = "인터벌",
        selected = selected,
        onBack = viewModel::navigateBack,
        onRotate = { delta ->
            when (selected) {
                "workMinutes" -> workMinutes = wrap(workMinutes, delta, 0, 60)
                "workSeconds" -> workSeconds = wrap(workSeconds, delta * 10, 0, 50, 10)
                "workKm" -> workKm = wrap(workKm, delta, 0, 20)
                "workDecimal" -> workDecimal = wrap(workDecimal, delta, 0, 9)
                "recoveryMinutes" -> recoveryMinutes = wrap(recoveryMinutes, delta, 0, 30)
                "recoverySeconds" -> recoverySeconds = wrap(recoverySeconds, delta * 10, 0, 50, 10)
                "recoveryKm" -> recoveryKm = wrap(recoveryKm, delta, 0, 10)
                "recoveryDecimal" -> recoveryDecimal = wrap(recoveryDecimal, delta, 0, 9)
                "sets" -> sets = wrap(sets, delta, 1, 20)
            }
        },
    ) {
        IntervalSegmentEditor(
            title = "운동",
            mode = workMode,
            onModeChange = { workMode = it; selected = null },
            minutes = workMinutes,
            seconds = workSeconds,
            km = workKm,
            decimal = workDecimal,
            selected = selected,
            prefix = "work",
            onSelect = { selected = toggleSelection(selected, it) },
        )
        IntervalSegmentEditor(
            title = "회복",
            mode = recoveryMode,
            onModeChange = { recoveryMode = it; selected = null },
            minutes = recoveryMinutes,
            seconds = recoverySeconds,
            km = recoveryKm,
            decimal = recoveryDecimal,
            selected = selected,
            prefix = "recovery",
            onSelect = { selected = toggleSelection(selected, it) },
        )
        SettingLabel("반복 횟수")
        DialValue("$sets", "회", selected == "sets") {
            selected = toggleSelection(selected, "sets")
        }
        CompletionChoice(completion) { completion = it }
        PrimaryAction("시작하기", Icons.Filled.PlayArrow) {
            viewModel.start(
                RunGoal.Interval(
                    work = intervalTarget(workMode, workMinutes, workSeconds, workKm, workDecimal),
                    recovery = intervalTarget(
                        recoveryMode,
                        recoveryMinutes,
                        recoverySeconds,
                        recoveryKm,
                        recoveryDecimal,
                    ),
                    sets = sets,
                    completionAction = completion,
                ),
            )
        }
    }
}

@Composable
private fun IntervalSegmentEditor(
    title: String,
    mode: SegmentMode,
    onModeChange: (SegmentMode) -> Unit,
    minutes: Int,
    seconds: Int,
    km: Int,
    decimal: Int,
    selected: String?,
    prefix: String,
    onSelect: (String) -> Unit,
) {
    SettingLabel(title)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ModeChip("시간", mode == SegmentMode.TIME) { onModeChange(SegmentMode.TIME) }
        ModeChip("거리", mode == SegmentMode.DISTANCE) { onModeChange(SegmentMode.DISTANCE) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (mode == SegmentMode.TIME) {
            DialValue("$minutes", "분", selected == "${prefix}Minutes") {
                onSelect("${prefix}Minutes")
            }
            Spacer(Modifier.width(5.dp))
            DialValue("%02d".format(seconds), "초", selected == "${prefix}Seconds") {
                onSelect("${prefix}Seconds")
            }
        } else {
            DialValue("$km", "km", selected == "${prefix}Km") {
                onSelect("${prefix}Km")
            }
            Text(".", color = Muted, fontSize = 24.sp)
            DialValue("$decimal", "100m", selected == "${prefix}Decimal") {
                onSelect("${prefix}Decimal")
            }
        }
    }
}

@Composable
private fun TrackingScreen(state: WatchRunState, viewModel: WatchViewModel) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                TrackingControls(state, viewModel)
            } else {
                TrackingMetrics(state)
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(2) { index ->
                Box(
                    Modifier
                        .size(if (pagerState.currentPage == index) 7.dp else 5.dp)
                        .background(
                            if (pagerState.currentPage == index) Accent else Muted.copy(alpha = 0.45f),
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun TrackingMetrics(state: WatchRunState) {
    WatchPage {
        Text(
            state.remainingLabel ?: "자유 러닝",
            color = if (state.gpsStatus == "POOR") Danger else Accent,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Text(formatDuration(state.elapsedSeconds), fontSize = if (compact) 32.sp else 38.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Metric("페이스", formatPace(state.paceMinPerKm))
            Metric("거리", "%.2fkm".format(state.distanceMeters / 1000.0))
        }
        state.progressPercent?.let { progress ->
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = Accent,
                trackColor = Color.White.copy(alpha = 0.12f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Metric("심박", state.heartRateBpm?.toString() ?: "--")
            Metric("케이던스", state.cadenceSpm?.toString() ?: "--")
        }
        Text("오른쪽으로 밀어 제어", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun TrackingControls(state: WatchRunState, viewModel: WatchViewModel) {
    WatchPage {
        Text(
            if (state.isPaused) "일시정지됨" else "러닝 제어",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Text(
            "${formatDuration(state.elapsedSeconds)} · %.2fkm".format(state.distanceMeters / 1000.0),
            color = Muted,
            fontSize = 12.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            RunControlButton(
                label = if (state.isPaused) "재생" else "일시정지",
                icon = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                color = Accent,
                onClick = if (state.isPaused) viewModel::resume else viewModel::pause,
                compact = compact,
            )
            RunControlButton(
                label = "종료",
                icon = Icons.Filled.Stop,
                color = Color.White,
                onClick = viewModel::finish,
                compact = compact,
            )
            RunControlButton(
                label = "취소",
                icon = Icons.Filled.Close,
                color = Danger,
                onClick = viewModel::abandon,
                compact = compact,
            )
        }
    }
}

@Composable
private fun PausedScreen(state: WatchRunState, viewModel: WatchViewModel) {
    TrackingControls(state, viewModel)
}

@Composable
private fun SummaryScreen(state: WatchRunState, onDone: () -> Unit) {
    WatchPage(scrollable = true) {
        Text("런 완료", color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("%.2f km".format(state.distanceMeters / 1000.0), fontWeight = FontWeight.Bold, fontSize = 32.sp)
        Text(formatDuration(state.elapsedSeconds), fontSize = 18.sp)
        Text("평균 페이스 ${formatPace(state.paceMinPerKm)}/km", color = Muted, fontSize = 11.sp)
        Text("상세 기록은 폰에서 확인하세요", color = Muted, fontSize = 10.sp)
        PrimaryAction("완료", Icons.Filled.Check, onClick = onDone)
    }
}

@Composable
private fun RotarySettingPage(
    title: String,
    selected: String?,
    onBack: () -> Unit,
    onRotate: (Int) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val valueState = rememberScrollableState { delta ->
        onRotate(if (delta > 0f) 1 else -1)
        delta
    }
    val rotaryBehavior = RotaryScrollableDefaults.behavior(
        scrollableState = if (selected == null) scrollState else valueState,
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight <= 260.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .requestFocusOnHierarchyActive()
                .rotaryScrollable(
                    behavior = rotaryBehavior,
                    focusRequester = focusRequester,
                )
                .verticalScroll(scrollState)
                .padding(horizontal = if (compact) 18.dp else 24.dp)
                .padding(top = if (compact) 8.dp else 14.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
        ) {
            BackTitle(title, onBack)
            content()
            Text(
                if (selected == null) "숫자를 누른 뒤 베젤을 돌리세요" else "베젤을 돌려 값 변경 · 다시 눌러 완료",
                color = Muted,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class WatchPageScope(val compact: Boolean)

@Composable
private fun WatchPage(
    scrollable: Boolean = true,
    content: @Composable WatchPageScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight <= 260.dp
        val scrollState = rememberScrollState()
        val focusRequester = remember { FocusRequester() }
        val rotaryBehavior = RotaryScrollableDefaults.behavior(scrollableState = scrollState)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (scrollable) {
                        Modifier
                            .requestFocusOnHierarchyActive()
                            .rotaryScrollable(
                                behavior = rotaryBehavior,
                                focusRequester = focusRequester,
                            )
                            .verticalScroll(scrollState)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = if (compact) 18.dp else 24.dp)
                .padding(
                    top = if (compact) 10.dp else 16.dp,
                    bottom = if (scrollable) {
                        if (compact) 28.dp else 36.dp
                    } else {
                        if (compact) 10.dp else 16.dp
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                if (compact) 7.dp else 10.dp,
                Alignment.CenterVertically,
            ),
        ) {
            WatchPageScope(compact).content()
        }
    }
}

@Composable
private fun DialValue(
    value: String,
    unit: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .heightIn(min = 54.dp)
            .background(if (selected) Accent.copy(alpha = 0.12f) else SurfaceColor, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) Accent else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = if (selected) Accent else Color.White)
        Text(unit, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun CompletionChoice(
    selected: GoalCompletionAction,
    onSelected: (GoalCompletionAction) -> Unit,
) {
    SettingLabel("목표 달성 후")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ModeChip("자동 일시정지", selected == GoalCompletionAction.PAUSE) {
            onSelected(GoalCompletionAction.PAUSE)
        }
        ModeChip("계속 기록", selected == GoalCompletionAction.CONTINUE) {
            onSelected(GoalCompletionAction.CONTINUE)
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Accent else SurfaceColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BackTitle(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceColor, CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "뒤로가기",
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun ConnectionLabel(connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(if (connected) Accent else Muted, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(if (connected) "폰 연결됨" else "오프라인 가능", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun PrimaryAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(if (compact) 36.dp else 44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun SecondaryAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: Color = Color.White,
    compact: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(if (compact) 36.dp else 42.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor, contentColor = color),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun RunControlButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    compact: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(if (compact) 62.dp else 72.dp)
                .background(
                    color = if (color == Accent) Accent else SurfaceColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = 1.dp,
                    color = if (color == Accent) Color.Transparent else color.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (color == Accent) Color.Black else color,
                modifier = Modifier.size(if (compact) 28.dp else 32.dp),
            )
        }
        Text(
            text = label,
            color = if (color == Danger) Danger else Muted,
            fontSize = 9.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 9.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatusText(text: String) {
    Text(text, color = Danger, fontSize = 9.sp, textAlign = TextAlign.Center)
}

private fun toggleSelection(current: String?, target: String): String? =
    if (current == target) null else target

private fun wrap(value: Int, delta: Int, min: Int, max: Int, step: Int = 1): Int {
    val next = value + delta
    return when {
        next > max -> min
        next < min -> max
        else -> (next / step) * step
    }
}

private fun intervalTarget(
    mode: SegmentMode,
    minutes: Int,
    seconds: Int,
    km: Int,
    decimal: Int,
): IntervalTarget = when (mode) {
    SegmentMode.TIME -> IntervalTarget.Time((minutes * 60 + seconds).coerceAtLeast(10))
    SegmentMode.DISTANCE -> IntervalTarget.Distance((km * 1000 + decimal * 100).coerceAtLeast(100))
}
