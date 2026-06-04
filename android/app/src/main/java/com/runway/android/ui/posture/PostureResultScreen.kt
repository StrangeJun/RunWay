package com.runway.android.ui.posture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.core.posture.PostureCategoryResult
import com.runway.android.core.posture.PostureResult
import com.runway.android.core.posture.local.PostureAnalysisEntity
import com.runway.android.ui.theme.RunwayTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostureResultScreen(
    analysisId: String?,
    result: PostureResult?,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    viewModel: PostureAnalysisViewModel = hiltViewModel(),
) {
    var entity by remember { mutableStateOf<PostureAnalysisEntity?>(null) }
    var loaded by remember { mutableStateOf(result != null) }

    LaunchedEffect(analysisId) {
        if (analysisId != null && result == null) {
            entity = viewModel.loadById(analysisId)
            loaded = true
        }
    }

    val displayResult: PostureResult? = result ?: entity?.toPostureResult()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("자세 분석 결과") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        when {
            !loaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            displayResult == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("결과를 불러올 수 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> PostureResultContent(
                result = displayResult,
                modifier = Modifier.padding(padding),
                onRetake = onRetake,
            )
        }
    }
}

@Composable
private fun PostureResultContent(
    result: PostureResult,
    modifier: Modifier = Modifier,
    onRetake: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Score header
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(gradeColor(result.grade).copy(alpha = 0.12f), MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    result.grade,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = gradeColor(result.grade),
                )
                Text(
                    "${result.overallScore}점",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            result.overallFeedback,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        // Category cards
        val categories = listOf(
            "무릎 굴곡" to result.knee,
            "상체 기울기" to result.trunk,
            "오버스트라이드" to result.overstride,
            "팔꿈치 각도" to result.elbow,
            "고관절 신전" to result.hip,
        )
        categories.forEach { (label, cat) ->
            PostureCategoryCard(label = label, category = cat)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("다시 분석하기")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PostureCategoryCard(label: String, category: PostureCategoryResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${category.score}점",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scoreColor(category.score),
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { category.score / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = scoreColor(category.score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            val displayValue = if (category.unit == "%") {
                "착지 위치: 엉덩이 기준 ${(category.measuredValue * 100).roundToInt()}% 앞"
            } else {
                "측정값: ${"%.1f".format(category.measuredValue)}${category.unit}  " +
                        "이상범위: ${"%.0f".format(category.idealMin)}~${"%.0f".format(category.idealMax)}${category.unit}"
            }
            Text(displayValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(category.feedback, style = MaterialTheme.typography.bodySmall)
            if (category.tip.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "💡 ${category.tip}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun scoreColor(score: Int) = when {
    score >= 80 -> Color(0xFF22C55E)
    score >= 60 -> Color(0xFF3B82F6)
    score >= 45 -> Color(0xFFF59E0B)
    else -> MaterialTheme.colorScheme.error
}

private fun PostureAnalysisEntity.toPostureResult(): PostureResult = PostureResult(
    overallScore = overallScore,
    grade = grade,
    overallFeedback = overallFeedback,
    knee = PostureCategoryResult(kneeScore, kneeMeasuredAngle, 155f, 170f, "°", kneeFeedback, kneeTip),
    trunk = PostureCategoryResult(trunkScore, trunkMeasuredAngle, 5f, 10f, "°", trunkFeedback, trunkTip),
    elbow = PostureCategoryResult(elbowScore, elbowMeasuredAngle, 85f, 95f, "°", elbowFeedback, elbowTip),
    hip = PostureCategoryResult(hipScore, hipMeasuredAngle, 160f, 180f, "°", hipFeedback, hipTip),
    overstride = PostureCategoryResult(overstrideScore, overstrideRatio, 0f, 0.10f, "%", overstrideFeedback, overstrideTip),
)

@Preview(showBackground = true)
@Composable
private fun PostureResultPreview() {
    RunwayTheme {
        PostureResultContent(
            result = PostureResult(
                overallScore = 74,
                grade = "B",
                overallFeedback = "전반적으로 좋은 자세입니다. 상체 기울기 부분을 보완하면 더 좋아집니다.",
                knee = PostureCategoryResult(82, 162f, 155f, 170f, "°", "착지 시 무릎 각도가 이상적입니다.", ""),
                trunk = PostureCategoryResult(55, 2.3f, 5f, 10f, "°", "상체를 약 5도 앞으로 기울여 보세요.", "전방 기울기는 추진력과 효율을 높여줍니다."),
                elbow = PostureCategoryResult(90, 91f, 85f, 95f, "°", "팔꿈치 각도가 이상적입니다.", ""),
                hip = PostureCategoryResult(78, 172f, 160f, 180f, "°", "고관절 신전이 적절합니다.", ""),
                overstride = PostureCategoryResult(70, 0.18f, 0f, 0.10f, "%", "착지 위치가 약간 앞쪽입니다.", "발이 엉덩이 아래에 가깝게 착지하면 제동력을 줄일 수 있습니다."),
            ),
            onRetake = {},
        )
    }
}
