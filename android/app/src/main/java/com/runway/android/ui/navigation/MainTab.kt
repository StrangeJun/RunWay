package com.runway.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val icon: ImageVector,
    val label: String,
) {
    HOME(Icons.Filled.Home, "홈"),
    DISCOVER(Icons.Filled.Explore, "탐색"),
    LEADERBOARD(Icons.Filled.EmojiEvents, "랭킹"),
    PROFILE(Icons.Filled.Person, "내 정보"),
}
