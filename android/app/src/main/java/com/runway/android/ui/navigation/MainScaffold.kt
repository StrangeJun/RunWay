package com.runway.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.runway.android.ui.components.RunwayBottomNav
import com.runway.android.ui.discover.DiscoverScreen
import com.runway.android.ui.home.HomeScreen
import com.runway.android.ui.leaderboard.LeaderboardScreen
import com.runway.android.ui.profile.ProfileScreen

@Composable
fun MainScaffold(
    onStartRun: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var currentTab by remember { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            RunwayBottomNav(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (currentTab) {
                MainTab.HOME -> HomeScreen(onStartRun = onStartRun)
                MainTab.DISCOVER -> DiscoverScreen()
                MainTab.LEADERBOARD -> LeaderboardScreen()
                MainTab.PROFILE -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}
