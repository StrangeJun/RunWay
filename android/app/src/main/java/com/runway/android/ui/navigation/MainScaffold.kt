package com.runway.android.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.RunwayBottomNav
import com.runway.android.ui.discover.DiscoverScreen
import com.runway.android.ui.home.HomeScreen
import com.runway.android.ui.home.HomeViewModel
import com.runway.android.ui.course.library.CoursesLibraryScreen
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
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val recoveryViewModel: TrackingRecoveryViewModel = hiltViewModel()

    var currentTabOrdinal by rememberSaveable { mutableIntStateOf(MainTab.HOME.ordinal) }
    val currentTab = MainTab.entries[currentTabOrdinal]

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
                )
            }

            if (recoveryViewModel.isVisible) {
                TrackingRecoveryDialog(viewModel = recoveryViewModel)
            }
        }
    }
}
