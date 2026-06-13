package com.runway.wear.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.runway.wear.model.RunGoal
import com.runway.wear.model.WatchRunState
import com.runway.wear.model.WatchScreen
import com.runway.wear.model.formatDuration
import com.runway.wear.model.formatPace
import com.runway.wear.data.OfflineCourse
import com.runway.wear.data.OfflineCourseRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private val Background = Color(0xFF090B0F)
private val SurfaceColor = Color(0xFF0B100E)
private val Accent = Color(0xFFB8FF00)
private val Muted = Color(0xFF9AA3AF)
private val Danger = Color(0xFFFF665E)
private val Grid = Color(0xFF173028)

@Composable
fun RunWayWearApp(
    viewModel: WatchViewModel = viewModel(),
    launchToken: Int = 0,
    isAmbient: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLaunchAnimation by remember(launchToken) { mutableStateOf(true) }
    LaunchedEffect(launchToken) {
        delay(700)
        showLaunchAnimation = false
    }
    BackHandler(
        enabled = state.screen != WatchScreen.HOME,
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
        Box(modifier = Modifier.fillMaxSize().background(Background)) {
            RunWayGridBackdrop()
            if (showLaunchAnimation) {
                LaunchAnimationScreen()
            } else if (isAmbient && (state.screen == WatchScreen.TRACKING || state.screen == WatchScreen.PAUSED)) {
                AmbientTrackingScreen(state)
            } else when (state.screen) {
                WatchScreen.HOME -> HomeScreen(
                    state = state,
                    onStart = viewModel::start,
                    onGoal = { viewModel.navigate(WatchScreen.GOAL_TYPE) },
                    onCourses = { viewModel.navigate(WatchScreen.COURSE_LIST) },
                    onLogin = viewModel::openPhoneApp,
                )
                WatchScreen.COURSE_LIST -> CourseListScreen(state, viewModel)
                WatchScreen.COURSE_DETAIL -> CourseDetailScreen(state, viewModel)
                WatchScreen.GOAL_TYPE -> GoalTypeScreen(viewModel)
                WatchScreen.TIME_GOAL -> TimeGoalScreen(viewModel)
                WatchScreen.DISTANCE_GOAL -> DistanceGoalScreen(viewModel)
                WatchScreen.INTERVAL_GOAL -> IntervalGoalScreen(viewModel)
                WatchScreen.SETTINGS -> RunningSettingsScreen(state, viewModel)
                WatchScreen.PREPARING -> PreparingScreen(
                    state = state,
                    onConfirm = viewModel::confirmPreparing,
                    onCancel = viewModel::cancelPreparing,
                )
                WatchScreen.COUNTDOWN -> CountdownScreen(
                    onFinished = viewModel::commitCountdown,
                    onCancel = viewModel::cancelCountdown,
                )
                WatchScreen.TRACKING -> TrackingScreen(state, viewModel)
                WatchScreen.PAUSED -> PausedScreen(state, viewModel)
                WatchScreen.SUMMARY -> SummaryScreen(state, viewModel::returnHome, viewModel::openRunOnPhone)
            }
        }
    }
}

@Composable
private fun LaunchAnimationScreen() {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.72f,
        animationSpec = tween(420),
        label = "launchScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(320),
        label = "launchAlpha",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo_mark),
            contentDescription = "RunWay",
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
        )
        Spacer(Modifier.height(10.dp))
        Row {
            Text(
                "Run",
                color = Accent.copy(alpha = alpha),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
            )
            Text(
                "Way",
                color = Color.White.copy(alpha = alpha),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    state: WatchRunState,
    onStart: (RunGoal) -> Unit,
    onGoal: () -> Unit,
    onCourses: () -> Unit,
    onLogin: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageHeight = maxHeight
        val compact = maxHeight <= 260.dp
        val scrollState = rememberScrollState()
        val focusRequester = remember { FocusRequester() }
        val rotaryBehavior = RotaryScrollableDefaults.behavior(scrollableState = scrollState)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .requestFocusOnHierarchyActive()
                .rotaryScrollable(
                    behavior = rotaryBehavior,
                    focusRequester = focusRequester,
                )
                .verticalScroll(scrollState),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pageHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo_mark),
                    contentDescription = "RunWay",
                    modifier = Modifier.size(if (compact) 38.dp else 48.dp),
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row {
                        Text(
                            "Run",
                            color = Accent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (compact) 20.sp else 23.sp,
                        )
                        Text(
                            "Way",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (compact) 20.sp else 23.sp,
                        )
                    }
                    Text(
                        "RUN YOUR WAY",
                        color = Color.White.copy(alpha = 0.38f),
                        fontSize = 8.sp,
                        letterSpacing = 1.5.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GpsStatusIndicator(state.gpsReady)
                    ConnectionLabel(state.isPhoneConnected)
                }

                Surface(
                    onClick = { onStart(RunGoal.Free) },
                    modifier = Modifier
                        .size(if (compact) 74.dp else 84.dp)
                        .border(2.dp, Accent.copy(alpha = 0.62f), CircleShape),
                    shape = CircleShape,
                    color = Accent,
                    shadowElevation = 10.dp,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF081000),
                            modifier = Modifier.size(27.dp),
                        )
                        Text(
                            "START",
                            color = Color(0xFF081000),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pageHeight)
                    .padding(horizontal = 28.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "RUN OPTIONS",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(14.dp))
                HomeAction(
                    label = "SET GOAL",
                    icon = Icons.Filled.Flag,
                    onClick = onGoal,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(9.dp))
                HomeAction(
                    label = "COURSE",
                    icon = Icons.Filled.Route,
                    onClick = onCourses,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.isPhoneConnected && state.phoneLoggedIn == false) {
                    Spacer(Modifier.height(9.dp))
                    SecondaryAction(
                        label = "핸드폰에서 로그인하기",
                        icon = Icons.Filled.PhoneAndroid,
                        onClick = onLogin,
                        color = Danger,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (state.isPhoneConnected) "PHONE CONNECTED" else "STANDALONE GPS",
                    color = Muted,
                    fontSize = 8.sp,
                    letterSpacing = 0.7.sp,
                    textAlign = TextAlign.Center,
                )
                state.phoneStatusMessage?.let { StatusText(it) }
            }
        }
    }
}

@Composable
private fun HomeAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.border(
            1.dp,
            Accent.copy(alpha = 0.26f),
            RoundedCornerShape(14.dp),
        ),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceColor.copy(alpha = 0.92f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
        }
    }
}

@Composable
private fun RunWayGridBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 26.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(Grid.copy(alpha = 0.28f), Offset(x, 0f), Offset(x, size.height), 0.7f)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(Grid.copy(alpha = 0.28f), Offset(0f, y), Offset(size.width, y), 0.7f)
            y += step
        }
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Accent.copy(alpha = 0.1f), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.65f,
            ),
            radius = size.minDimension * 0.65f,
            center = center,
        )
    }
}

@Composable
private fun CourseListScreen(state: WatchRunState, viewModel: WatchViewModel) {
    val courses by OfflineCourseRepository.courses.collectAsStateWithLifecycle()
    WatchPage(scrollable = true) {
        BackTitle("저장된 코스", viewModel::navigateBack)
        SecondaryAction(
            label = if (state.isSyncingCourses) "불러오는 중..." else "주변 코스 불러오기",
            icon = Icons.Filled.Route,
            onClick = viewModel::syncNearbyCourses,
            color = if (state.isPhoneConnected) Accent else Muted,
        )
        if (!state.isPhoneConnected) {
            Text("새 코스 저장은 폰 연결 시 가능합니다", color = Muted, fontSize = 9.sp)
        }
        if (courses.isEmpty()) {
            Text(
                "저장된 코스가 없습니다\n폰 연결 후 주변 코스를 저장하세요",
                color = Muted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        } else {
            courses.forEach { course ->
                CourseCard(course) { viewModel.selectCourse(course.courseId) }
            }
        }
        state.phoneStatusMessage?.let { Text(it, color = Muted, fontSize = 9.sp) }
    }
}

@Composable
private fun CourseCard(course: OfflineCourse, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(course.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f, fill = false))
            Box(
                modifier = Modifier
                    .background(
                        if (course.isPublic) Accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    if (course.isPublic) "공개" else "개인",
                    color = if (course.isPublic) Accent else Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            "%.1fkm · 현재 위치에서 %.0fm".format(
                course.distanceMeters / 1000.0,
                course.distanceFromMeMeters,
            ),
            color = Muted,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun CourseDetailScreen(state: WatchRunState, viewModel: WatchViewModel) {
    val course = OfflineCourseRepository.courses.value
        .firstOrNull { it.courseId == state.selectedCourseId }
    WatchPage(scrollable = true) {
        BackTitle("코스 도전", viewModel::navigateBack)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(course?.name ?: state.courseName.orEmpty(), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f, fill = false))
            if (course != null) {
                Box(
                    modifier = Modifier
                        .background(
                            if (course.isPublic) Accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        if (course.isPublic) "공개" else "개인",
                        color = if (course.isPublic) Accent else Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            "%.2f km".format((course?.distanceMeters ?: state.courseDistanceMeters) / 1000.0),
            color = Accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        course?.description?.let {
            Text(it, color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
        Text("경로 ${course?.points?.size ?: 0}개 지점 오프라인 저장됨", color = Muted, fontSize = 9.sp)
        PrimaryAction("코스 도전 시작", Icons.Filled.PlayArrow, onClick = viewModel::startSelectedCourse)
        SecondaryAction(
            label = "에서 경로살펴보기",
            icon = Icons.Filled.PhoneAndroid,
            onClick = viewModel::openCourseOnPhone,
            color = if (state.isPhoneConnected) Color.White else Muted,
        )
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
    var completion by remember {
        mutableStateOf(viewModel.state.value.goalCompletionAction)
    }
    var selected by remember { mutableStateOf<String?>(null) }

    RotarySettingPage(
        title = "시간 목표",
        selected = selected,
        onBack = viewModel::navigateBack,
        onRotate = { delta ->
            when (selected) {
                "hours" -> hours = wrap(hours, delta, 0, 5)
                "minutes" -> minutes = wrap(minutes, delta, 0, 59)
            }
        },
        picker = when (selected) {
            "hours" -> NumberPickerSpec("시간", hours, 0, 5, onValueChange = { hours = it })
            "minutes" -> NumberPickerSpec("분", minutes, 0, 59, "%02d", onValueChange = { minutes = it })
            else -> null
        },
        onDismissPicker = { selected = null },
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
    var completion by remember {
        mutableStateOf(viewModel.state.value.goalCompletionAction)
    }
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
        picker = when (selected) {
            "km" -> NumberPickerSpec("킬로미터", km, 0, 50, onValueChange = { km = it })
            "decimal" -> NumberPickerSpec("100미터 단위", decimal, 0, 9, onValueChange = { decimal = it })
            else -> null
        },
        onDismissPicker = { selected = null },
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
    var completion by remember {
        mutableStateOf(viewModel.state.value.goalCompletionAction)
    }
    var selected by remember { mutableStateOf<String?>(null) }

    RotarySettingPage(
        title = "인터벌",
        selected = selected,
        onBack = viewModel::navigateBack,
        onRotate = { delta ->
            when (selected) {
                "workMinutes" -> workMinutes = wrap(workMinutes, delta, 0, 60)
                "workSeconds" -> workSeconds = wrap(workSeconds, delta, 0, 59)
                "workKm" -> workKm = wrap(workKm, delta, 0, 20)
                "workDecimal" -> workDecimal = wrap(workDecimal, delta, 0, 9)
                "recoveryMinutes" -> recoveryMinutes = wrap(recoveryMinutes, delta, 0, 30)
                "recoverySeconds" -> recoverySeconds = wrap(recoverySeconds, delta, 0, 59)
                "recoveryKm" -> recoveryKm = wrap(recoveryKm, delta, 0, 10)
                "recoveryDecimal" -> recoveryDecimal = wrap(recoveryDecimal, delta, 0, 9)
                "sets" -> sets = wrap(sets, delta, 1, 20)
            }
        },
        picker = intervalPickerSpec(
            selected = selected,
            workMinutes = workMinutes,
            workSeconds = workSeconds,
            workKm = workKm,
            workDecimal = workDecimal,
            recoveryMinutes = recoveryMinutes,
            recoverySeconds = recoverySeconds,
            recoveryKm = recoveryKm,
            recoveryDecimal = recoveryDecimal,
            sets = sets,
            onWorkMinutes = { workMinutes = it },
            onWorkSeconds = { workSeconds = it },
            onWorkKm = { workKm = it },
            onWorkDecimal = { workDecimal = it },
            onRecoveryMinutes = { recoveryMinutes = it },
            onRecoverySeconds = { recoverySeconds = it },
            onRecoveryKm = { recoveryKm = it },
            onRecoveryDecimal = { recoveryDecimal = it },
            onSets = { sets = it },
        ),
        onDismissPicker = { selected = null },
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
            when {
                state.isOffCourse -> "코스 이탈 · 경로로 돌아가세요"
                state.courseName != null -> "${state.courseName} ${state.courseProgressPercent}%"
                else -> state.remainingLabel ?: "자유 러닝"
            },
            color = if (state.gpsStatus == "POOR" || state.isOffCourse) Danger else Accent,
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
        if (state.courseName != null) {
            LinearProgressIndicator(
                progress = { state.courseProgressPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = if (state.isOffCourse) Danger else Accent,
                trackColor = Color.White.copy(alpha = 0.12f),
            )
            state.distanceToCourseMeters?.let {
                Text("코스까지 ${it.toInt()}m", color = if (state.isOffCourse) Danger else Muted, fontSize = 9.sp)
            }
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
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }

    if (confirmAction != null) {
        ConfirmDialog(
            message = if (confirmAction == ConfirmAction.FINISH) "러닝을 종료하시겠어요?" else "러닝을 취소하시겠어요?",
            confirmLabel = if (confirmAction == ConfirmAction.FINISH) "종료" else "취소",
            confirmColor = if (confirmAction == ConfirmAction.FINISH) Accent else Danger,
            onConfirm = {
                val action = confirmAction
                confirmAction = null
                if (action == ConfirmAction.FINISH) viewModel.finish() else viewModel.abandon()
            },
            onDismiss = { confirmAction = null },
        )
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val btnSize = if (maxHeight <= 200.dp) 50.dp else 58.dp
        val iconSize = if (maxHeight <= 200.dp) 22.dp else 26.dp
        val gap = if (maxHeight <= 200.dp) 6.dp else 8.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = if (maxHeight <= 200.dp) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            if (state.isOffCourse && state.isPaused) {
                Text(
                    "경로 복귀 시 자동 재개",
                    color = Danger,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    "${formatDuration(state.elapsedSeconds)} · %.2fkm".format(state.distanceMeters / 1000.0),
                    color = if (state.isPaused) Accent else Muted,
                    fontSize = 11.sp,
                    fontWeight = if (state.isPaused) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                CompactControlButton(
                    label = if (state.isPaused) "재생" else "일시정지",
                    icon = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    color = if (state.isOffCourse && state.isPaused) Muted else Accent,
                    size = btnSize,
                    iconSize = iconSize,
                    onClick = if (state.isPaused) viewModel::resume else viewModel::pause,
                )
                CompactControlButton(
                    label = "종료",
                    icon = Icons.Filled.Stop,
                    color = Color.White,
                    size = btnSize,
                    iconSize = iconSize,
                    onClick = { confirmAction = ConfirmAction.FINISH },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                CompactControlButton(
                    label = "취소",
                    icon = Icons.Filled.Close,
                    color = Danger,
                    size = btnSize,
                    iconSize = iconSize,
                    onClick = { confirmAction = ConfirmAction.ABANDON },
                )
                CompactControlButton(
                    label = "설정",
                    icon = Icons.Filled.Settings,
                    color = Color.White,
                    size = btnSize,
                    iconSize = iconSize,
                    onClick = viewModel::openSettings,
                )
            }
        }
    }
}

private enum class ConfirmAction { FINISH, ABANDON }

@Composable
private fun RunningSettingsScreen(
    state: WatchRunState,
    viewModel: WatchViewModel,
) {
    WatchPage {
        BackTitle("러닝 설정", viewModel::navigateBack)
        SettingToggleRow(
            label = "음성 안내",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            checked = state.voiceGuidanceEnabled,
            onCheckedChange = viewModel::setVoiceGuidanceEnabled,
        )
        SettingToggleRow(
            label = "자동 일시정지",
            icon = Icons.Filled.Pause,
            checked = state.autoPauseEnabled,
            onCheckedChange = viewModel::setAutoPauseEnabled,
        )
        SettingLabel("목표 달성 후")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeChip(
                "자동 일시정지",
                state.goalCompletionAction == GoalCompletionAction.PAUSE,
            ) {
                viewModel.setGoalCompletionAction(GoalCompletionAction.PAUSE)
            }
            ModeChip(
                "계속 기록",
                state.goalCompletionAction == GoalCompletionAction.CONTINUE,
            ) {
                viewModel.setGoalCompletionAction(GoalCompletionAction.CONTINUE)
            }
        }
        Text(
            "자동 일시정지는 다음 러닝부터 적용됩니다",
            color = Muted,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PausedScreen(state: WatchRunState, viewModel: WatchViewModel) {
    TrackingControls(state, viewModel)
}

@Composable
private fun SummaryScreen(
    state: WatchRunState,
    onDone: () -> Unit,
    onOpenRunOnPhone: () -> Unit = {},
) {
    WatchPage(scrollable = true) {
        Text("런 완료", color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("%.2f km".format(state.distanceMeters / 1000.0), fontWeight = FontWeight.Bold, fontSize = 32.sp)
        Text(formatDuration(state.elapsedSeconds), fontSize = 18.sp)
        Text("평균 페이스 ${formatPace(state.paceMinPerKm)}/km", color = Muted, fontSize = 11.sp)
        Text(
            state.phoneStatusMessage ?: "폰 연결 시 기록과 GPS 경로를 자동 동기화합니다",
            color = Muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
        if (state.syncedRunId != null && state.isPhoneConnected) {
            SecondaryAction(
                label = "에서 코스로 등록",
                icon = Icons.Filled.PhoneAndroid,
                onClick = onOpenRunOnPhone,
            )
        }
        PrimaryAction("완료", Icons.Filled.Check, onClick = onDone)
    }
}

@Composable
private fun RotarySettingPage(
    title: String,
    selected: String?,
    onBack: () -> Unit,
    onRotate: (Int) -> Unit,
    picker: NumberPickerSpec?,
    onDismissPicker: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (picker != null) {
        FullScreenNumberPicker(picker, onDismissPicker)
        return
    }

    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    var lastStepMs by remember { mutableStateOf(0L) }
    val valueState = rememberScrollableState { delta ->
        rotaryAccumulator += delta
        if (kotlin.math.abs(rotaryAccumulator) >= ROTARY_STEP_THRESHOLD) {
            val now = System.currentTimeMillis()
            val direction = if (rotaryAccumulator > 0f) 1 else -1
            rotaryAccumulator = 0f
            if (now - lastStepMs >= ROTARY_DEBOUNCE_MS) {
                lastStepMs = now
                onRotate(direction)
            }
        }
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
                "숫자를 누르면 확대 선택 화면이 열립니다",
                color = Muted,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class NumberPickerSpec(
    val title: String,
    val value: Int,
    val min: Int,
    val max: Int,
    val format: String = "%d",
    val onValueChange: (Int) -> Unit,
)

@Composable
private fun FullScreenNumberPicker(
    spec: NumberPickerSpec,
    onDone: () -> Unit,
) {
    val values = remember(spec.min, spec.max) { (spec.min..spec.max).toList() }
    var localValue by remember(spec.title) { mutableIntStateOf(spec.value) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (spec.value - spec.min).coerceIn(0, values.lastIndex),
    )
    val scope = rememberCoroutineScope()
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    var lastStepMs by remember { mutableStateOf(0L) }
    val rotaryState = rememberScrollableState { delta ->
        rotaryAccumulator += delta
        if (kotlin.math.abs(rotaryAccumulator) >= ROTARY_STEP_THRESHOLD) {
            val now = System.currentTimeMillis()
            val direction = if (rotaryAccumulator > 0f) 1 else -1
            rotaryAccumulator = 0f
            val next = (localValue + direction).coerceIn(spec.min, spec.max)
            if (next != localValue && now - lastStepMs >= ROTARY_DEBOUNCE_MS) {
                lastStepMs = now
                localValue = next
                scope.launch { listState.scrollToItem(next - spec.min) }
            }
        }
        delta
    }
    val focusRequester = remember { FocusRequester() }
    val rotaryBehavior = RotaryScrollableDefaults.behavior(rotaryState)

    val commitAndDone = {
        spec.onValueChange(localValue)
        onDone()
    }

    BackHandler(onBack = commitAndDone)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    val layout = listState.layoutInfo
                    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                    val centered = layout.visibleItemsInfo.minByOrNull {
                        kotlin.math.abs((it.offset + it.size / 2) - center)
                    } ?: return@collect
                    localValue = values[centered.index]
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(rotaryBehavior, focusRequester),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 72.dp),
            flingBehavior = rememberSnapFlingBehavior(listState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(values, key = { it }) { value ->
                val selected = value == localValue
                Text(
                    text = spec.format.format(value),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            localValue = value
                            commitAndDone()
                        }
                        .padding(vertical = 8.dp),
                    color = if (selected) Accent else Muted,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            spec.title,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Button(
            onClick = commitAndDone,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).height(38.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
        ) {
            Text("확인", fontWeight = FontWeight.Bold)
        }
    }
}

private const val ROTARY_STEP_THRESHOLD = 96f
private const val ROTARY_DEBOUNCE_MS = 150L

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
private fun GpsStatusIndicator(gpsReady: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "gps_blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(8.dp).background(
                color = if (gpsReady) Accent else Accent.copy(alpha = blinkAlpha),
                shape = CircleShape,
            ),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (gpsReady) "GPS 준비됨" else "GPS 탐색 중",
            color = if (gpsReady) Accent else Muted,
            fontSize = 10.sp,
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
private fun CompactControlButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .background(
                    color = if (color == Accent) Accent else SurfaceColor,
                    shape = RoundedCornerShape(10.dp),
                )
                .border(
                    width = 1.dp,
                    color = if (color == Accent) Color.Transparent else color.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (color == Accent) Color.Black else color,
                modifier = Modifier.size(iconSize),
            )
        }
        Text(label, color = if (color == Danger) Danger else Muted, fontSize = 10.sp, maxLines = 1)
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
                .size(if (compact) 72.dp else 82.dp)
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
private fun SettingToggleRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor, RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) Accent else Muted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

private fun intervalPickerSpec(
    selected: String?,
    workMinutes: Int,
    workSeconds: Int,
    workKm: Int,
    workDecimal: Int,
    recoveryMinutes: Int,
    recoverySeconds: Int,
    recoveryKm: Int,
    recoveryDecimal: Int,
    sets: Int,
    onWorkMinutes: (Int) -> Unit,
    onWorkSeconds: (Int) -> Unit,
    onWorkKm: (Int) -> Unit,
    onWorkDecimal: (Int) -> Unit,
    onRecoveryMinutes: (Int) -> Unit,
    onRecoverySeconds: (Int) -> Unit,
    onRecoveryKm: (Int) -> Unit,
    onRecoveryDecimal: (Int) -> Unit,
    onSets: (Int) -> Unit,
): NumberPickerSpec? = when (selected) {
    "workMinutes" -> NumberPickerSpec("운동 분", workMinutes, 0, 60, onValueChange = onWorkMinutes)
    "workSeconds" -> NumberPickerSpec(
        "운동 초",
        workSeconds,
        0,
        59,
        "%02d",
        onWorkSeconds,
    )
    "workKm" -> NumberPickerSpec("운동 km", workKm, 0, 20, onValueChange = onWorkKm)
    "workDecimal" -> NumberPickerSpec("운동 100m", workDecimal, 0, 9, onValueChange = onWorkDecimal)
    "recoveryMinutes" -> NumberPickerSpec(
        "회복 분",
        recoveryMinutes,
        0,
        30,
        onValueChange = onRecoveryMinutes,
    )
    "recoverySeconds" -> NumberPickerSpec(
        "회복 초",
        recoverySeconds,
        0,
        59,
        "%02d",
        onRecoverySeconds,
    )
    "recoveryKm" -> NumberPickerSpec("회복 km", recoveryKm, 0, 10, onValueChange = onRecoveryKm)
    "recoveryDecimal" -> NumberPickerSpec(
        "회복 100m",
        recoveryDecimal,
        0,
        9,
        onValueChange = onRecoveryDecimal,
    )
    "sets" -> NumberPickerSpec("반복 횟수", sets, 1, 20, onValueChange = onSets)
    else -> null
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

@Composable
private fun PreparingScreen(
    state: WatchRunState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gps_blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )

    WatchPage {
        Text("준비 중", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))

        // GPS 상태
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (state.gpsReady) Accent
                                else Accent.copy(alpha = blinkAlpha),
                        shape = CircleShape,
                    ),
            )
            Text(
                text = if (state.gpsReady) "GPS 준비됨" else "GPS 탐색 중...",
                color = if (state.gpsReady) Accent else Muted,
                fontSize = 13.sp,
            )
        }

        if (state.gpsReady) {
            Text("✓ 준비 완료", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        } else {
            Text("GPS 신호를 기다리는 중입니다", color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
        }

        Button(
            onClick = onConfirm,
            enabled = state.gpsReady,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.Black,
                disabledContainerColor = SurfaceColor,
                disabledContentColor = Muted,
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("시작하기", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        SecondaryAction(
            label = "취소",
            icon = Icons.Filled.Close,
            onClick = onCancel,
        )
    }
}

@Composable
private fun CountdownScreen(
    onFinished: () -> Unit,
    onCancel: () -> Unit,
) {
    var count by remember { mutableIntStateOf(3) }
    val arcProgress = remember { Animatable(0f) }

    LaunchedEffect(count) {
        arcProgress.snapTo(0f)
        if (count > 0) arcProgress.animateTo(1f, tween(900, easing = LinearEasing))
    }

    LaunchedEffect(Unit) {
        delay(1_000); count = 2
        delay(1_000); count = 1
        delay(1_000); count = 0
        delay(400); onFinished()
    }

    BackHandler(onBack = onCancel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(Unit) { detectTapGestures { onFinished() } },
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                drawArc(Accent.copy(alpha = 0.15f), -90f, 360f, false, style = stroke)
                drawArc(Accent, -90f, 360f * arcProgress.value, false, style = stroke)
            }
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (scaleIn(initialScale = 1.4f, animationSpec = tween(220)) +
                        fadeIn(tween(160))) togetherWith
                        (scaleOut(targetScale = 0.7f, animationSpec = tween(180)) +
                            fadeOut(tween(140)))
                },
                label = "countdown",
            ) { c ->
                Text(
                    text = if (c == 0) "GO!" else c.toString(),
                    fontSize = if (c == 0) 42.sp else 72.sp,
                    fontWeight = FontWeight.Black,
                    color = if (c == 0) Accent else Color.White,
                )
            }
        }
        Text(
            "화면을 탭하면 바로 시작",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            color = Muted,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ConfirmDialog(
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text(message, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor, contentColor = Color.White),
                ) { Text("아니요", fontSize = 11.sp) }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = confirmColor, contentColor = Color.Black),
                ) { Text(confirmLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun AmbientTrackingScreen(state: WatchRunState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = formatDuration(state.elapsedSeconds),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "%.2f km".format(state.distanceMeters / 1000.0),
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = formatPace(state.paceMinPerKm) + "/km",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
        if (state.isPaused) {
            Text(
                text = "일시정지",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}
