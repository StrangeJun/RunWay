package com.runway.android.ui.navigation

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
import com.runway.android.ui.leaderboard.LeaderboardScreen
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
) {
    val recoveryViewModel: TrackingRecoveryViewModel = hiltViewModel()

    var currentTabOrdinal by rememberSaveable { mutableIntStateOf(MainTab.HOME.ordinal) }
    val currentTab = MainTab.entries[currentTabOrdinal]

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
                )
                MainTab.DISCOVER -> DiscoverScreen(
                    onNavigateToCourseDetail = onNavigateToCourseDetail,
                )
                MainTab.LEADERBOARD -> LeaderboardScreen()
                MainTab.PROFILE -> ProfileScreen(
                    onLogout = onLogout,
                    onNavigateToMyRuns = onNavigateToMyRuns,
                    onNavigateToCourses = onNavigateToCourses,
                )
            }

            if (recoveryViewModel.isVisible) {
                TrackingRecoveryDialog(viewModel = recoveryViewModel)
            }
        }
    }
}
