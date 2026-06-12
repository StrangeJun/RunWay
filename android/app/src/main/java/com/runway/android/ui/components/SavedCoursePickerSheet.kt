package com.runway.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runway.android.data.course.model.CourseResponse
import com.runway.android.ui.theme.SurfaceContainerDark
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedCoursePickerSheet(
    courses: List<CourseResponse>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onExploreCourses: () -> Unit,
    onCourseSelected: (CourseResponse) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainerDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                        MaterialTheme.shapes.extraLarge,
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            // ── 헤더 ──────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.shapes.large,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        text = "저장한 코스",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "도전할 코스를 선택하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )

            // ── 콘텐츠 ──────────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            onClick = onRetry,
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = "다시 시도",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                courses.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 32.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    MaterialTheme.shapes.extraLarge,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "저장한 코스가 없어요",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "코스 탐색에서 마음에 드는 코스를\n즐겨찾기하면 여기서 빠르게 선택할 수 있어요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .runwayCardFrame(MaterialTheme.shapes.extraLarge),
                            onClick = onExploreCourses,
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Explore,
                                    null,
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "코스 탐색하러 가기",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 480.dp)
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(courses, key = { it.courseId }) { course ->
                            SavedCourseCard(
                                course = course,
                                onClick = { onCourseSelected(course) },
                            )
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedCourseCard(
    course: CourseResponse,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .runwayCardFrame(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.06f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 거리 배지
            val km = course.distanceMeters / 1000.0
            val distLabel = if (km >= 1.0) "%.1f".format(km) else "${course.distanceMeters.toInt()}"
            val unitLabel = if (km >= 1.0) "km" else "m"
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        MaterialTheme.shapes.large,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = distLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = unitLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }
            }

            // 이름 + 메타
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = String.format(Locale.KOREA, "%.1f km", course.distanceMeters / 1000.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (course.isLoop) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Loop,
                                    null,
                                    Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "루프",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // 화살표
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
