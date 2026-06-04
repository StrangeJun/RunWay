package com.runway.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val icon: ImageVector,
    val label: String,
) {
    HOME(Icons.Filled.Home, "홈"),
    DISCOVER(Icons.Filled.Explore, "탐색"),
    COURSES(Icons.Filled.Route, "코스"),
    POSTURE(Icons.Filled.SelfImprovement, "자세"),
    PROFILE(Icons.Filled.Person, "내 정보"),
}
