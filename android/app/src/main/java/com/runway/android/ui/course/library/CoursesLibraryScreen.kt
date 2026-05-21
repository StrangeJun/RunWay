package com.runway.android.ui.course.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CoursesLibraryScreen(
    onNavigateToCourseDetail: (String) -> Unit = {},
) {
    val tabs = listOf("만든 코스", "즐겨찾기", "참여한 코스")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Text(
            text = "내 코스",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (selectedTabIndex) {
                0 -> CreatedCoursesTab()
                1 -> FavoriteCoursesTab()
                2 -> ParticipatedCoursesTab()
            }
        }
    }
}

@Composable
private fun CreatedCoursesTab() {
    PlaceholderMessage("내가 만든 코스")
}

@Composable
private fun FavoriteCoursesTab() {
    PlaceholderMessage("즐겨찾는 코스")
}

@Composable
private fun ParticipatedCoursesTab() {
    PlaceholderMessage("참여한 코스")
}

@Composable
private fun PlaceholderMessage(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "준비 중입니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "($label 기능은 B-30에서 추가됩니다)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
