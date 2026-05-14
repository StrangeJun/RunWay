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
    HOME(Icons.Filled.Home, "Home"),
    DISCOVER(Icons.Filled.Explore, "Discover"),
    LEADERBOARD(Icons.Filled.EmojiEvents, "Ranks"),
    PROFILE(Icons.Filled.Person, "Me"),
}
