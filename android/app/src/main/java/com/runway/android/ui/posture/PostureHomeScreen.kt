package com.runway.android.ui.posture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.posture.local.PostureAnalysisEntity
import com.runway.android.ui.theme.RunwayTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostureHomeScreen(
    onStartCapture: () -> Unit,
    onOpenResult: (String) -> Unit,
    viewModel: PostureAnalysisViewModel = hiltViewModel(),
) {
    val history by viewModel.analysisHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("자세 분석") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartCapture) {
                Icon(Icons.Filled.Add, contentDescription = "새 분석 시작")
            }
        },
    ) { padding ->
        if (history.isEmpty()) {
            PostureEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onStartCapture = onStartCapture,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(history) { entity ->
                    PostureHistoryCard(entity = entity, onClick = { onOpenResult(entity.id) })
                }
            }
        }
    }
}

@Composable
private fun PostureEmptyState(
    modifier: Modifier = Modifier,
    onStartCapture: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "아직 분석 기록이 없어요",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "+ 버튼을 눌러 첫 자세 분석을 시작해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostureHistoryCard(
    entity: PostureAnalysisEntity,
    onClick: () -> Unit,
) {
    val dateStr = SimpleDateFormat("M월 d일 HH:mm", Locale.KOREAN).format(Date(entity.createdAt))

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(entity.overallFeedback, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entity.grade,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = gradeColor(entity.grade),
                )
                Text(
                    "${entity.overallScore}점",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun gradeColor(grade: String) = when (grade) {
    "S" -> MaterialTheme.colorScheme.primary
    "A" -> androidx.compose.ui.graphics.Color(0xFF22C55E)
    "B" -> androidx.compose.ui.graphics.Color(0xFF3B82F6)
    "C" -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
    else -> MaterialTheme.colorScheme.error
}

@Preview(showBackground = true)
@Composable
private fun PostureEmptyStatePreview() {
    RunwayTheme {
        PostureEmptyState(onStartCapture = {})
    }
}
