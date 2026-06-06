package com.runway.android.ui.discover

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.runway.android.ui.components.DiscoverCourseCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToCourseDetail: (String) -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showFilterSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.onLocationPermissionGranted() else viewModel.onLocationPermissionDenied()
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) viewModel.onLocationPermissionGranted()
        else permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    val onRefresh: () -> Unit = if (
        viewModel.viewMode == DiscoverViewMode.LIST && viewModel.isLocationRequired
    ) {
        { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    } else viewModel::refresh

    val radiusLabel = when (viewModel.radiusMeters) {
        1000 -> "1km"; 5000 -> "5km"; else -> "3km"
    }

    // Keep the map outside LazyColumn so map gestures never scroll the whole screen.
    if (viewModel.viewMode == DiscoverViewMode.MAP) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "코스 탐색",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "전국 공개 코스를 지도에서 확인하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, "새로고침", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BadgedBox(
                    badge = {
                        if (viewModel.activeFilterCount > 0) Badge { Text("${viewModel.activeFilterCount}") }
                    }
                ) {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "필터",
                            tint = if (viewModel.activeFilterCount > 0)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                TextField(
                    value = viewModel.keyword,
                    onValueChange = viewModel::onKeywordChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("코스 이름으로 검색", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = if (viewModel.keyword.isNotEmpty()) {
                        { IconButton(onClick = viewModel::clearKeyword) {
                            Icon(Icons.Filled.Close, "검색어 지우기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }}
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide(); viewModel.onSearch() }),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }

            DiscoverViewModeSelector(
                selectedMode = viewModel.viewMode,
                onModeSelected = viewModel::onViewModeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp),
            )

            Spacer(Modifier.height(12.dp))

            DiscoverCourseMap(
                courses = viewModel.mapCourses,
                isLoading = viewModel.isMapLoading,
                errorMessage = viewModel.mapErrorMessage,
                currentLocation = viewModel.currentLocation,
                onCourseClick = onNavigateToCourseDetail,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
        if (showFilterSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                FilterSheetContent(viewModel = viewModel, onDismiss = { showFilterSheet = false })
            }
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // ─── 헤더 ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "코스 탐색",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = if (viewModel.isLoading) "불러오는 중…"
                            else "${viewModel.courses.size}개 코스 · 반경 $radiusLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, "새로고침", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BadgedBox(
                        badge = {
                            if (viewModel.activeFilterCount > 0) {
                                Badge { Text("${viewModel.activeFilterCount}") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = "필터",
                                tint = if (viewModel.activeFilterCount > 0)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ─── 검색바 ───
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    TextField(
                        value = viewModel.keyword,
                        onValueChange = viewModel::onKeywordChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("코스 이름으로 검색", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = if (viewModel.keyword.isNotEmpty()) {
                            {
                                IconButton(onClick = viewModel::clearKeyword) {
                                    Icon(Icons.Filled.Close, "검색어 지우기",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            viewModel.onSearch()
                        }),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }
            }

            item {
                DiscoverViewModeSelector(
                    selectedMode = viewModel.viewMode,
                    onModeSelected = viewModel::onViewModeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp),
                )
            }

            // ─── 활성 필터 요약 칩 ───
            if (viewModel.activeFilterCount > 0) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (viewModel.radiusMeters != 3000) {
                            item {
                                ActiveChip(label = "반경 $radiusLabel") {
                                    viewModel.onRadiusChange(3000)
                                }
                            }
                        }
                        viewModel.isLoopFilter?.let {
                            item {
                                ActiveChip(label = "루프코스") {
                                    viewModel.onIsLoopFilterChange(null)
                                }
                            }
                        }
                        if (viewModel.distanceFilter != DistanceFilterOption.ALL) {
                            item {
                                ActiveChip(label = viewModel.distanceFilter.label) {
                                    viewModel.onDistanceFilterChange(DistanceFilterOption.ALL)
                                }
                            }
                        }
                        if (viewModel.sortOption != CourseSortOption.NEAREST) {
                            item {
                                ActiveChip(label = viewModel.sortOption.label) {
                                    viewModel.onSortChange(CourseSortOption.NEAREST)
                                }
                            }
                        }
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = Color.Transparent,
                                onClick = viewModel::resetFilters,
                            ) {
                                Text(
                                    text = "초기화",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ─── 코스 목록 ───
            when {
                viewModel.isLoading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                viewModel.errorMessage != null -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("코스를 불러오지 못했습니다", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                        Text(viewModel.errorMessage!!, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Surface(onClick = onRefresh, shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = if (viewModel.isLocationRequired) "위치 권한 허용" else "다시 시도",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                viewModel.courses.isEmpty() -> item {
                    val hasActiveFilter = viewModel.activeFilterCount > 0
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (viewModel.keyword.isNotBlank()) "검색 결과가 없습니다" else "주변에 코스가 없습니다",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = when {
                                viewModel.keyword.isNotBlank() -> "다른 검색어나 더 넓은 반경으로 시도해 보세요."
                                hasActiveFilter -> "필터를 해제하거나 반경을 늘려 보세요."
                                else -> "반경을 늘리거나 직접 코스를 만들어 보세요."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> itemsIndexed(viewModel.courses, key = { _, c -> c.courseId }) { index, course ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(course.courseId) {
                        delay(index.coerceAtMost(8) * 50L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 3 },
                    ) {
                        DiscoverCourseCard(
                            course = course,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 12.dp),
                            onClick = { onNavigateToCourseDetail(course.courseId) },
                        )
                    }
                }
            }
        }
    }

    // ─── 필터 바텀시트 ───
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            FilterSheetContent(
                viewModel = viewModel,
                onDismiss = { showFilterSheet = false },
            )
        }
    }
}

@Composable
private fun DiscoverViewModeSelector(
    selectedMode: DiscoverViewMode,
    onModeSelected: (DiscoverViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            listOf(
                Triple(DiscoverViewMode.LIST, Icons.AutoMirrored.Filled.List, "목록"),
                Triple(DiscoverViewMode.MAP, Icons.Filled.Map, "지도"),
            ).forEach { (mode, icon, label) ->
                val selected = selectedMode == mode
                Surface(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    shadowElevation = if (selected) 1.dp else 0.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─── 필터 바텀시트 내용 ───────────────────────────────────────────────────────

@Composable
private fun FilterSheetContent(
    viewModel: DiscoverViewModel,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "필터 & 정렬",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                onClick = {
                    viewModel.resetFilters()
                    onDismiss()
                },
                color = Color.Transparent,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = "초기화",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(20.dp))

        if (viewModel.viewMode == DiscoverViewMode.LIST) {
            // ─── 반경 ───
            FilterSection(title = "반경") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1000 to "1km", 3000 to "3km", 5000 to "5km").forEach { (meters, label) ->
                        SheetChip(
                            label = label,
                            selected = viewModel.radiusMeters == meters,
                            onClick = { viewModel.onRadiusChange(meters) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ─── 코스 유형 ───
        FilterSection(title = "코스 유형") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetChip(
                    label = "전체",
                    selected = viewModel.isLoopFilter == null,
                    onClick = { viewModel.onIsLoopFilterChange(null) },
                )
                SheetChip(
                    label = "루프코스",
                    selected = viewModel.isLoopFilter == true,
                    onClick = { viewModel.onIsLoopFilterChange(true) },
                )
                SheetChip(
                    label = "일반코스",
                    selected = viewModel.isLoopFilter == false,
                    onClick = { viewModel.onIsLoopFilterChange(false) },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── 코스 거리 ───
        FilterSection(title = "코스 거리") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DistanceFilterOption.entries.forEach { option ->
                    SheetChip(
                        label = option.label,
                        selected = viewModel.distanceFilter == option,
                        onClick = { viewModel.onDistanceFilterChange(option) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── 정렬 ───
        FilterSection(title = "정렬") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CourseSortOption.entries.forEach { option ->
                    SheetChip(
                        label = option.label,
                        selected = viewModel.sortOption == option,
                        onClick = { viewModel.onSortChange(option) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 적용 버튼
        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = "적용",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SheetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

// ─── 활성 필터 요약 칩 ──────────────────────────────────────────────────────

@Composable
private fun ActiveChip(label: String, onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        onClick = onDismiss,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "필터 해제",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
