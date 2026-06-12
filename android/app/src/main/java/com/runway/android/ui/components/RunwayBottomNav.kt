package com.runway.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runway.android.ui.navigation.MainTab
import com.runway.android.ui.theme.LocalIsDarkTheme
import com.runway.android.ui.theme.OutlineVariantDark
import com.runway.android.ui.theme.SurfaceContainerLowestDark

@Composable
fun RunwayBottomNav(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val topBorderColor = if (isDark) OutlineVariantDark else Color.Black.copy(alpha = 0.08f)

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .drawBehind {
                drawLine(
                    color = topBorderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        containerColor = if (isDark) SurfaceContainerLowestDark.copy(alpha = 0.96f)
                         else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        MainTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(20.dp),
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
