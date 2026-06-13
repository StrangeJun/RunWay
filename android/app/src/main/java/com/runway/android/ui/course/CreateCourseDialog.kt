package com.runway.android.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.runway.android.ui.theme.DisplayFontFamily

private val DialogBg    = Color(0xFF0E1612)
private val DialogAccent = Color(0xFFB8FF00)
private val DialogDark  = Color(0xFF080C0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCourseDialog(
    courseName: String,
    onCourseNameChange: (String) -> Unit,
    isLoop: Boolean,
    onIsLoopChange: (Boolean) -> Unit,
    isCreating: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(DialogAccent.copy(0.70f), DialogAccent.copy(0.20f), DialogAccent.copy(0.07f)),
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                ),
            shape = MaterialTheme.shapes.extraLarge,
            color = DialogBg,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // ── 헤더 ────────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DialogAccent.copy(0.12f), MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.AddCircleOutline,
                            null,
                            Modifier.size(20.dp),
                            tint = DialogAccent,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "SAVE COURSE",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            "내 코스로 저장",
                            style = MaterialTheme.typography.labelSmall,
                            color = DialogAccent.copy(0.60f),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .width(36.dp)
                        .height(2.dp)
                        .background(DialogAccent, RoundedCornerShape(1.dp)),
                )
                Spacer(Modifier.height(14.dp))

                Text(
                    "10회 완주 후 공개 코스로 등록할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.38f),
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = DialogAccent.copy(0.10f))
                Spacer(Modifier.height(20.dp))

                // ── 코스 이름 입력 ───────────────────────────────────────────
                Text(
                    "COURSE NAME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DialogAccent.copy(0.65f),
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(6.dp))

                val fieldHasError = errorMessage != null && courseName.isBlank()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = if (fieldHasError)
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.error.copy(0.80f), MaterialTheme.colorScheme.error.copy(0.30f)))
                            else
                                Brush.linearGradient(listOf(DialogAccent.copy(0.50f), DialogAccent.copy(0.15f), DialogAccent.copy(0.05f))),
                            shape = MaterialTheme.shapes.large,
                        ),
                    shape = MaterialTheme.shapes.large,
                    color = Color.White.copy(0.04f),
                ) {
                    TextField(
                        value = courseName,
                        onValueChange = onCourseNameChange,
                        placeholder = {
                            Text(
                                "아침 러닝 코스",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(0.25f),
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor     = Color.Transparent,
                            cursorColor = DialogAccent,
                        ),
                    )
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── 루프 토글 ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (isLoop) DialogAccent.copy(0.15f) else Color.White.copy(0.06f),
                                    MaterialTheme.shapes.small,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Loop,
                                null,
                                Modifier.size(16.dp),
                                tint = if (isLoop) DialogAccent else Color.White.copy(0.35f),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "루프 코스",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLoop) DialogAccent else Color.White.copy(0.75f),
                            )
                            Text(
                                "시작점 = 종료점",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.28f),
                            )
                        }
                    }
                    Switch(
                        checked = isLoop,
                        onCheckedChange = onIsLoopChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor    = DialogDark,
                            checkedTrackColor    = DialogAccent,
                            uncheckedThumbColor  = Color.White.copy(0.40f),
                            uncheckedTrackColor  = Color.White.copy(0.10f),
                            uncheckedBorderColor = Color.White.copy(0.15f),
                        ),
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── 버튼 ─────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isCreating,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(
                            "취소",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(0.38f),
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = courseName.isNotBlank() && !isCreating,
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor         = DialogAccent,
                            contentColor           = DialogDark,
                            disabledContainerColor = DialogAccent.copy(0.30f),
                            disabledContentColor   = DialogDark,
                        ),
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = DialogDark,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                "만들기",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
