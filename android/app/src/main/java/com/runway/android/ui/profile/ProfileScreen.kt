package com.runway.android.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.runway.android.ui.components.PersonalRecordsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToMyRuns: () -> Unit = {},
    onNavigateToCourses: () -> Unit = {},
    onNavigateToRunDetail: (String) -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToReminder: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onImageSelected(uri) }

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "프로필",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start),
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else if (viewModel.profileError) {
            Text(
                text = "프로필을 불러오지 못했습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // ─── 아바타 ───
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                val avatarModifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .then(
                        if (viewModel.isEditing)
                            Modifier.clickable {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        else Modifier
                    )

                val localUri = viewModel.selectedImageUri
                val remoteUrl = viewModel.profileImageUrl

                if (localUri != null) {
                    AsyncImage(
                        model = localUri,
                        contentDescription = "프로필 이미지",
                        contentScale = ContentScale.Crop,
                        modifier = avatarModifier,
                    )
                } else if (remoteUrl != null) {
                    AsyncImage(
                        model = remoteUrl,
                        contentDescription = "프로필 이미지",
                        contentScale = ContentScale.Crop,
                        modifier = avatarModifier,
                    )
                } else {
                    Box(
                        modifier = avatarModifier.background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = viewModel.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                if (viewModel.isEditing) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "사진 변경",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = viewModel.nickname,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = viewModel.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (viewModel.isEditing) {
                // ─── 편집 모드 ───
                OutlinedTextField(
                    value = viewModel.editNickname,
                    onValueChange = viewModel::updateEditNickname,
                    label = { Text("닉네임") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.editBio,
                    onValueChange = viewModel::updateEditBio,
                    label = { Text("소개") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (viewModel.saveError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.saveError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = viewModel::cancelEditing,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("취소")
                    }
                    Button(
                        onClick = viewModel::saveProfile,
                        enabled = !viewModel.isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (viewModel.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("저장")
                        }
                    }
                }
            } else {
                // ─── 표시 모드 ───
                if (viewModel.bio.isNotEmpty()) {
                    Text(
                        text = viewModel.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TextButton(onClick = viewModel::startEditing) {
                    Text("편집", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            viewModel.stats?.let { s ->
                var statsTriggered by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { statsTriggered = true }

                val animatedRuns by animateIntAsState(
                    targetValue = if (statsTriggered) s.totalRuns.toInt() else 0,
                    animationSpec = tween(1200), label = "runs",
                )
                val distanceTarget = s.totalDistanceKm.toFloatOrNull() ?: 0f
                val animatedDistance by animateFloatAsState(
                    targetValue = if (statsTriggered) distanceTarget else 0f,
                    animationSpec = tween(1200), label = "dist",
                )
                val animatedStreak by animateIntAsState(
                    targetValue = if (statsTriggered) s.currentStreakDays else 0,
                    animationSpec = tween(1200), label = "streak",
                )
                val flameTransition = rememberInfiniteTransition(label = "flame")
                val flameScale by flameTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "flameScale",
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "STATS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "총 러닝",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${animatedRuns}회",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Column {
                                Text(
                                    text = "총 거리",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${"%.2f".format(animatedDistance)} km",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            if (s.currentStreakDays > 0) {
                                Column {
                                    Text(
                                        text = "현재 streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Whatshot,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp).scale(flameScale),
                                        )
                                        Text(
                                            text = "${animatedStreak}일 연속",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                PersonalRecordsSection(
                    records = viewModel.personalRecords,
                    onRecordClick = onNavigateToRunDetail,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── Stats 메뉴 ───
        ProfileMenuItem(
            icon = Icons.Filled.BarChart,
            label = "통계",
            onClick = onNavigateToStats,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Achievements 메뉴 ───
        ProfileMenuItem(
            icon = Icons.Filled.EmojiEvents,
            label = "업적",
            onClick = onNavigateToAchievements,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ─── My Runs 메뉴 ───
        ProfileMenuItem(
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            label = "내 러닝",
            onClick = onNavigateToMyRuns,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ─── My Courses 메뉴 ───
        ProfileMenuItem(
            icon = Icons.Filled.Route,
            label = "내 코스",
            onClick = onNavigateToCourses,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Reminder 메뉴 ───
        ProfileMenuItem(
            icon = Icons.Filled.NotificationsActive,
            label = "러닝 리마인더",
            onClick = onNavigateToReminder,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Settings 메뉴 ───
        ProfileMenuItem(
            icon = Icons.Filled.Settings,
            label = "설정",
            onClick = onNavigateToSettings,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ─── 로그아웃 ───
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
            onClick = { viewModel.logout(onLogout) },
        ) {
            Text(
                text = "로그아웃",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    } // PullToRefreshBox
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
