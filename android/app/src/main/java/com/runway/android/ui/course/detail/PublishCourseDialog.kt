package com.runway.android.ui.course.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runway.android.data.course.model.PublishCourseRequest

private val DIFFICULTY_OPTIONS = listOf("EASY" to "쉬움", "NORMAL" to "보통", "HARD" to "어려움")
private val SLOPE_OPTIONS = listOf("FLAT" to "평탄", "MODERATE" to "완만", "STEEP" to "가파름")
private val RISK_OPTIONS = listOf("LOW" to "낮음", "MEDIUM" to "보통", "HIGH" to "높음")
private val SURFACE_OPTIONS = listOf("ROAD" to "도로", "PARK" to "공원", "TRAIL" to "트레일", "MIXED" to "혼합")
private val TIME_OPTIONS = listOf("MORNING" to "아침", "DAY" to "낮", "NIGHT" to "저녁", "ANY" to "무관")

@Composable
fun PublishCourseDialog(
    isPublishing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPublish: (PublishCourseRequest) -> Unit,
) {
    var difficulty by remember { mutableStateOf("") }
    var slopeLevel by remember { mutableStateOf("") }
    var riskLevel by remember { mutableStateOf("") }
    var surfaceType by remember { mutableStateOf("") }
    var recommendedTime by remember { mutableStateOf("") }
    var warnings by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val allSelected = difficulty.isNotEmpty() && slopeLevel.isNotEmpty() &&
            riskLevel.isNotEmpty() && surfaceType.isNotEmpty() && recommendedTime.isNotEmpty()

    AlertDialog(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        title = { Text("코스 공개", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "공개 코스는 다른 러너가 따라 달릴 수 있으므로 난이도, 위험도, 주의사항을 자세히 작성해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ChipGroup("난이도 *", DIFFICULTY_OPTIONS, difficulty) { difficulty = it }
                ChipGroup("경사도 *", SLOPE_OPTIONS, slopeLevel) { slopeLevel = it }
                ChipGroup("위험도 *", RISK_OPTIONS, riskLevel) { riskLevel = it }
                ChipGroup("노면 유형 *", SURFACE_OPTIONS, surfaceType) { surfaceType = it }
                ChipGroup("추천 시간대 *", TIME_OPTIONS, recommendedTime) { recommendedTime = it }

                OutlinedTextField(
                    value = warnings,
                    onValueChange = { warnings = it },
                    label = { Text("주의사항") },
                    placeholder = { Text("야간에는 조명이 부족합니다.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("코스 설명") },
                    placeholder = { Text("강변을 따라 달리는 평탄한 코스입니다.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )

                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (allSelected && !isPublishing) {
                        onPublish(
                            PublishCourseRequest(
                                difficulty = difficulty,
                                slopeLevel = slopeLevel,
                                riskLevel = riskLevel,
                                surfaceType = surfaceType,
                                recommendedTime = recommendedTime,
                                warnings = warnings.trim().ifBlank { null },
                                description = description.trim().ifBlank { null },
                            )
                        )
                    }
                },
                enabled = allSelected && !isPublishing,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("공개하기")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isPublishing) onDismiss() }) {
                Text("취소")
            }
        },
    )
}

@Composable
private fun ChipGroup(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, display) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(display, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}
