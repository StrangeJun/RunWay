package com.runway.android.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.RunwayBottomNav
import com.runway.android.ui.discover.DiscoverScreen
import com.runway.android.ui.home.HomeScreen
import com.runway.android.ui.home.HomeViewModel
import com.runway.android.ui.course.library.CoursesLibraryScreen
import com.runway.android.ui.posture.PostureAnalysisState
import com.runway.android.ui.posture.PostureAnalysisViewModel
import com.runway.android.ui.posture.PostureAnalyzingScreen
import com.runway.android.ui.posture.PostureHomeScreen
import com.runway.android.ui.posture.PostureResultScreen
import com.runway.android.ui.profile.ProfileScreen
import com.runway.android.ui.tracking.TrackingRecoveryDialog
import com.runway.android.ui.tracking.TrackingRecoveryViewModel

@Composable
fun MainScaffold(
    onStartRun: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToCourseDetail: (String) -> Unit = {},
    onNavigateToMyRuns: () -> Unit = {},
    onNavigateToCourses: () -> Unit = {},
    onNavigateToRunDetail: (String) -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToReminder: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val recoveryViewModel: TrackingRecoveryViewModel = hiltViewModel()
    val postureViewModel: PostureAnalysisViewModel = hiltViewModel()

    var currentTabOrdinal by rememberSaveable { mutableIntStateOf(MainTab.HOME.ordinal) }
    val currentTab = MainTab.entries[currentTabOrdinal]
    var showPostureCapture by remember { mutableStateOf(false) }
    var postureResultId by remember { mutableStateOf<String?>(null) }
    val postureState by postureViewModel.analysisState.collectAsState()

    // Handle analysis errors without mutating state during composition
    LaunchedEffect(postureState) {
        if (postureState is PostureAnalysisState.Error) {
            val msg = (postureState as PostureAnalysisState.Error).message
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            postureViewModel.resetState()
        }
    }

    BackHandler(enabled = currentTab != MainTab.HOME) {
        currentTabOrdinal = MainTab.HOME.ordinal
    }

    Scaffold(
        bottomBar = {
            RunwayBottomNav(
                currentTab = currentTab,
                onTabSelected = { currentTabOrdinal = it.ordinal },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (currentTab) {
                MainTab.HOME -> HomeScreen(
                    onStartRun = onStartRun,
                    onSeeAllRuns = onNavigateToMyRuns,
                    onNavigateToCourseDetail = onNavigateToCourseDetail,
                    onNavigateToDiscover = { currentTabOrdinal = MainTab.DISCOVER.ordinal },
                    onNavigateToRunDetail = onNavigateToRunDetail,
                    viewModel = homeViewModel,
                )
                MainTab.DISCOVER -> DiscoverScreen(
                    onNavigateToCourseDetail = onNavigateToCourseDetail,
                )
                MainTab.COURSES -> CoursesLibraryScreen(
                    onNavigateToCourseDetail = onNavigateToCourseDetail,
                )
                MainTab.PROFILE -> ProfileScreen(
                    onLogout = onLogout,
                    onNavigateToMyRuns = onNavigateToMyRuns,
                    onNavigateToCourses = onNavigateToCourses,
                    onNavigateToRunDetail = onNavigateToRunDetail,
                    onNavigateToStats = onNavigateToStats,
                    onNavigateToAchievements = onNavigateToAchievements,
                    onNavigateToReminder = onNavigateToReminder,
                    onNavigateToSettings = onNavigateToSettings,
                )
                MainTab.POSTURE -> when {
                    showPostureCapture -> {
                        com.runway.android.ui.posture.PostureCaptureScreen(
                            onVideoReady = { uri ->
                                showPostureCapture = false
                                postureViewModel.analyze(uri)
                            },
                            onBack = { showPostureCapture = false },
                        )
                    }
                    postureState is PostureAnalysisState.Analyzing -> {
                        PostureAnalyzingScreen()
                    }
                    postureState is PostureAnalysisState.Success -> {
                        val success = postureState as PostureAnalysisState.Success
                        PostureResultScreen(
                            analysisId = success.id,
                            result = success.result,
                            onBack = { postureViewModel.resetState() },
                            onRetake = {
                                postureViewModel.resetState()
                                showPostureCapture = true
                            },
                            viewModel = postureViewModel,
                        )
                    }
                    postureResultId != null -> {
                        PostureResultScreen(
                            analysisId = postureResultId,
                            result = null,
                            onBack = { postureResultId = null },
                            onRetake = {
                                postureResultId = null
                                showPostureCapture = true
                            },
                            viewModel = postureViewModel,
                        )
                    }
                    else -> {
                        PostureHomeScreen(
                            onStartCapture = { showPostureCapture = true },
                            onOpenResult = { postureResultId = it },
                            viewModel = postureViewModel,
                        )
                    }
                }
            }

            if (recoveryViewModel.isVisible) {
                TrackingRecoveryDialog(viewModel = recoveryViewModel)
            }
        }
    }
}
