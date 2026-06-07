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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import java.util.Locale
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
    var loaded by remember { mutableStateOf(result != null && analysisId == null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Always load entity from DB when we have an ID — entity carries the video path + frames
    LaunchedEffect(analysisId) {
        if (analysisId != null) {
            loaded = false
            entity = viewModel.loadById(analysisId)
            loaded = true
        }
    }

    val displayResult: PostureResult? = entity?.toPostureResult() ?: result
    val canDelete = analysisId != null || entity != null

    val videoFrames = remember(entity?.videoFramesJson) {
        viewModel.parseVideoFrames(entity?.videoFramesJson)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("분석 이력 삭제") },
            text = { Text("이 자세 분석 피드백을 삭제할까요? 삭제한 이력은 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        (analysisId ?: entity?.id)?.let(viewModel::deleteAnalysis)
                        showDeleteDialog = false
                        onBack()
                    },
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("자세 분석 결과") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (canDelete) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "분석 이력 삭제",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                videoPath = entity?.videoPath,
                videoFrames = videoFrames,
                videoWidth = entity?.videoWidth ?: 0,
                videoHeight = entity?.videoHeight ?: 0,
                modifier = Modifier.padding(padding),
                onRetake = onRetake,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun PostureResultContent(
    result: PostureResult,
    videoPath: String?,
    videoFrames: List<com.runway.android.core.posture.PostureVideoFrame>,
    videoWidth: Int,
    videoHeight: Int,
    modifier: Modifier = Modifier,
    onRetake: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Video replay section (only when video file exists)
        if (videoPath != null && videoFrames.isNotEmpty()) {
            PostureVideoPlayerCard(
                videoPath = videoPath,
                videoFrames = videoFrames,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
            )
            Spacer(Modifier.height(18.dp))
        }

        // Overall score card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
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
            }
        }
        Spacer(Modifier.height(18.dp))

        // Scored category cards
        val scoredCategories = listOf(
            "무릎 굴곡" to result.knee,
            "상체 기울기" to result.trunk,
            "팔꿈치 각도" to result.elbow,
            "고관절 스윙" to result.hip,
        )
        scoredCategories.forEach { (label, cat) ->
            PostureCategoryCard(label = label, category = cat)
            Spacer(Modifier.height(10.dp))
        }

        // Reference metrics (bilateral signals — not included in overall score)
        val hasReferenceData = result.cadence.measuredValue > 0f ||
                result.verticalOscillation.measuredValue > 0f
        if (hasReferenceData) {
            Spacer(Modifier.height(6.dp))
            Text(
                "참고 지표",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "양쪽 다리 움직임으로 측정 — 점수에 포함되지 않습니다",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (result.cadence.measuredValue > 0f) {
                PostureCategoryCard(label = "케이던스", category = result.cadence, isReference = true)
                Spacer(Modifier.height(10.dp))
            }
            if (result.verticalOscillation.measuredValue > 0f) {
                PostureCategoryCard(label = "수직진폭", category = result.verticalOscillation, isReference = true)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("다시 분석하기")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("이력 보기")
        }

        Spacer(Modifier.height(20.dp))
        PostureDisclaimerCard()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PostureCategoryCard(
    label: String,
    category: PostureCategoryResult,
    isReference: Boolean = false,
) {
    val borderColor = if (isReference)
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isReference) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            else MaterialTheme.colorScheme.onSurface,
                )
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

            val displayValue = when {
                category.unit == "spm" ->
                    "측정값: ${category.measuredValue.roundToInt()} spm  " +
                            "이상범위: ${category.idealMin.roundToInt()}~${category.idealMax.roundToInt()} spm"
                else ->
                    "측정값: ${String.format(Locale.US, "%.1f", category.measuredValue)}${category.unit}  " +
                            "이상범위: ${String.format(Locale.US, "%.0f", category.idealMin)}~${String.format(Locale.US, "%.0f", category.idealMax)}${category.unit}"
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
    knee = PostureCategoryResult(kneeScore, kneeMeasuredAngle, 135f, 180f, "°", kneeFeedback, kneeTip),
    trunk = PostureCategoryResult(trunkScore, trunkMeasuredAngle, 5f, 15f, "°", trunkFeedback, trunkTip),
    elbow = PostureCategoryResult(elbowScore, elbowMeasuredAngle, 60f, 90f, "°", elbowFeedback, elbowTip),
    hip = PostureCategoryResult(hipScore, hipMeasuredAngle, 29f, 41f, "°", hipFeedback, hipTip),
    overstride = PostureCategoryResult(overstrideScore, overstrideRatio, 0f, 15f, "°", overstrideFeedback, overstrideTip),
    cadence = PostureCategoryResult(cadenceScore, cadenceSpm, 170f, 180f, "spm", cadenceFeedback, cadenceTip),
    verticalOscillation = PostureCategoryResult(verticalOscScore, verticalOscPercent, 0f, 6.5f, "%", verticalOscFeedback, verticalOscTip),
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
                knee = PostureCategoryResult(82, 148f, 135f, 165f, "°", "착지 시 무릎 각도가 이상적입니다.", ""),
                trunk = PostureCategoryResult(55, 2.3f, 5f, 10f, "°", "상체를 약 5도 앞으로 기울여 보세요.", "전방 기울기는 추진력과 효율을 높여줍니다."),
                elbow = PostureCategoryResult(90, 91f, 85f, 95f, "°", "팔꿈치 각도가 이상적입니다.", ""),
                hip = PostureCategoryResult(78, 35f, 29f, 41f, "°", "고관절 스윙 범위가 좋습니다.", ""),
                overstride = PostureCategoryResult(70, 17f, 0f, 15f, "°", "착지 위치가 약간 앞쪽입니다.", "발이 엉덩이 아래에 가깝게 착지하면 제동력을 줄일 수 있습니다."),
                cadence = PostureCategoryResult(85, 174f, 170f, 180f, "spm", "케이던스 174spm으로 이상적입니다.", ""),
                verticalOscillation = PostureCategoryResult(72, 6.8f, 4f, 8f, "%", "수직진폭이 이상적입니다.", ""),
            ),
            videoPath = null,
            videoFrames = emptyList(),
            videoWidth = 0,
            videoHeight = 0,
            onRetake = {},
            onBack = {},
        )
    }
}

@Composable
private fun PostureDisclaimerCard() {
    val warningColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = warningColor,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 1.dp),
            )
            Text(
                text = "본 분석은 러닝 자세를 이해하기 위한 참고 자료입니다. " +
                    "촬영 환경, 카메라 각도, 의류, 조명 등에 따라 정확도가 달라질 수 있으며, " +
                    "의료적 판단의 근거로 사용해서는 안 됩니다.",
                style = MaterialTheme.typography.labelSmall,
                color = warningColor,
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight,
            )
        }
    }
}
