# Android 앱 전체 디자인 리디자인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stitch 신규 디자인 컨셉(순수 블랙, 글래스모피즘 카드, full-pill 버튼, 악센트 색상 추가)을 Android 앱 전체 화면에 적용한다. 스플래시 화면은 제외.

**Architecture:** 디자인 토큰(Color/Shape/Theme) 변경이 MaterialTheme을 통해 대부분의 화면에 자동 전파된다. 토큰 변경 후 공통 컴포넌트(카드/버튼/네비)를 글래스모피즘으로 교체하면 스크린 레벨 수정은 최소화된다.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose Material3, android/app 모듈

---

### Task 1: Color.kt — 순수 블랙 배경 + 글래스 토큰 + 새 악센트 값

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/theme/Color.kt`

- [ ] **Step 1: Color.kt 전체 교체**

```kotlin
package com.runway.android.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Dark theme backgrounds (순수 블랙 계열) ───
val BackgroundDark = Color(0xFF0A0A0A)
val SurfaceDark = Color(0xFF141414)
val MutedDark = Color(0xFF1E1E1E)
val SecondaryContainerDark = Color(0xFF242424)

// ─── Light theme backgrounds ───
val BackgroundLight = Color(0xFFF5F5FA)
val SurfaceLight = Color(0xFFFFFFFF)
val MutedLight = Color(0xFFEEEEF4)
val SecondaryContainerLight = Color(0xFFE8E8F0)

// ─── Text (dark theme) ───
val OnSurfaceWhite = Color(0xFFF8F8FC)
val OnSurfaceMuted = Color(0xFF888AA8)

// ─── Text (light theme) ───
val OnSurfaceDark = Color(0xFF111119)
val OnSurfaceMutedLight = Color(0xFF666680)

// ─── Border / Outline ───
val BorderColorDark = Color(0xFF2A2A2A)
val BorderColorLight = Color(0xFFD8D8E8)

// ─── Glass card tokens (dark) ───
val GlassSurfaceDark = Color(0x0DFFFFFF)   // white 5%
val GlassBorderDark = Color(0x1AFFFFFF)    // white 10%

// ─── Primary — Neon lime green ───
val RunwayGreen = Color(0xFFA4E168)
val OnRunwayGreen = Color(0xFF111119)

// ─── Accent colors ───
val RunwayWhite = Color(0xFFFFFFFF)
val OnRunwayWhite = Color(0xFF0A0A0A)
val RunwayEnergyOrange = Color(0xFFFF6B35)
val OnRunwayOrange = Color(0xFF0A0A0A)
val RunwayElectricBlue = Color(0xFF4D9EFF)
val OnRunwayBlue = Color(0xFF0A0A0A)
val RunwayRed = Color(0xFFFF7474)
val RunwayOrange = Color(0xFFFFA45B)
val RunwayYellow = Color(0xFFF3D45C)
val RunwayBlue = Color(0xFF65B7FF)
val RunwayPurple = Color(0xFFC58BFF)

// ─── Semantic states ───
val DestructiveRed = Color(0xFFDC5040)
val SuccessGreen = Color(0xFF7FD98A)
val WarningYellow = Color(0xFFCCBE50)
```

- [ ] **Step 2: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/theme/Color.kt && git commit -m "feat(android): update color tokens to pure-black + glass palette"
```

---

### Task 2: Shape.kt — 코너 반경 증가 + PillShape

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/theme/Shape.kt`

- [ ] **Step 1: Shape.kt 전체 교체**

```kotlin
package com.runway.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RunwayShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),   // 칩, 배지
    small      = RoundedCornerShape(14.dp),   // 아이콘 컨테이너
    medium     = RoundedCornerShape(20.dp),   // 기본 카드, 입력 필드
    large      = RoundedCornerShape(28.dp),   // 대형 카드
    extraLarge = RoundedCornerShape(32.dp),   // 통계 카드, 맵 컨테이너
)

/** 버튼 full-pill — RunwayPrimaryButton / RunwayLoadingButton 전용 */
val PillShape = RoundedCornerShape(999.dp)
```

- [ ] **Step 2: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/theme/Shape.kt && git commit -m "feat(android): increase corner radii and add PillShape"
```

---

### Task 3: Theme.kt — WHITE / ENERGY_ORANGE / ELECTRIC_BLUE 악센트 추가

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/theme/Theme.kt`

- [ ] **Step 1: Theme.kt 전체 교체**

```kotlin
package com.runway.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = compositionLocalOf { true }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class AccentColor(
    val displayName: String,
    val color: Color,
    val onColor: Color,
    val darkContainer: Color,
    val lightContainer: Color,
) {
    GREEN("초록", RunwayGreen, OnRunwayGreen, Color(0xFF1C2E14), Color(0xFFD6F5B0)),
    WHITE("화이트", RunwayWhite, OnRunwayWhite, Color(0xFF1E1E1E), Color(0xFFF0F0F0)),
    ENERGY_ORANGE("오렌지", RunwayEnergyOrange, OnRunwayOrange, Color(0xFF3D1A0A), Color(0xFFFFD5C2)),
    ELECTRIC_BLUE("블루", RunwayElectricBlue, OnRunwayBlue, Color(0xFF0A1E3D), Color(0xFFC2DCFF)),
    RED("빨강", RunwayRed, OnRunwayGreen, Color(0xFF3D1B20), Color(0xFFFFDADA)),
    ORANGE("주황", RunwayOrange, OnRunwayGreen, Color(0xFF3B2414), Color(0xFFFFDCC2)),
    YELLOW("노랑", RunwayYellow, OnRunwayGreen, Color(0xFF342D12), Color(0xFFFFEFA8)),
    BLUE("파랑", RunwayBlue, OnRunwayGreen, Color(0xFF142B3D), Color(0xFFCDE8FF)),
    PURPLE("보라", RunwayPurple, OnRunwayGreen, Color(0xFF2D1D3D), Color(0xFFEBD8FF)),
}

private fun runwayDarkColorScheme(accent: AccentColor) = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = MutedDark,

    primary = accent.color,
    onPrimary = accent.onColor,
    primaryContainer = accent.darkContainer,
    onPrimaryContainer = accent.color,

    secondary = SecondaryContainerDark,
    onSecondary = OnSurfaceWhite,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSurfaceWhite,

    error = DestructiveRed,
    onError = OnSurfaceWhite,
    errorContainer = Color(0xFF3B1410),
    onErrorContainer = DestructiveRed,

    onBackground = OnSurfaceWhite,
    onSurface = OnSurfaceWhite,
    onSurfaceVariant = OnSurfaceMuted,

    outline = BorderColorDark,
    outlineVariant = BorderColorDark,

    inverseSurface = OnSurfaceWhite,
    inverseOnSurface = BackgroundDark,
    inversePrimary = accent.color,
)

private fun runwayLightColorScheme(accent: AccentColor) = lightColorScheme(
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = MutedLight,

    primary = accent.color,
    onPrimary = accent.onColor,
    primaryContainer = accent.lightContainer,
    onPrimaryContainer = OnSurfaceDark,

    secondary = SecondaryContainerLight,
    onSecondary = OnSurfaceDark,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSurfaceDark,

    error = DestructiveRed,
    onError = OnSurfaceWhite,
    errorContainer = Color(0xFFFFEDEB),
    onErrorContainer = DestructiveRed,

    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceMutedLight,

    outline = BorderColorLight,
    outlineVariant = BorderColorLight,

    inverseSurface = OnSurfaceDark,
    inverseOnSurface = BackgroundLight,
    inversePrimary = accent.color,
)

@Composable
fun RunwayTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColor = AccentColor.GREEN,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = if (isDark) runwayDarkColorScheme(accentColor)
                          else runwayLightColorScheme(accentColor),
            typography = RunwayTypography,
            shapes = RunwayShapes,
            content = content,
        )
    }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/theme/Theme.kt && git commit -m "feat(android): add WHITE/ENERGY_ORANGE/ELECTRIC_BLUE accent colors"
```

---

### Task 4: 버튼 — Full-Pill 형태

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/components/RunwayPrimaryButton.kt`
- Modify: `android/app/src/main/java/com/runway/android/ui/components/RunwayLoadingButton.kt`

- [ ] **Step 1: RunwayPrimaryButton.kt 교체**

```kotlin
package com.runway.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.PillShape

@Composable
fun RunwayPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
```

- [ ] **Step 2: RunwayLoadingButton.kt 교체**

```kotlin
package com.runway.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.PillShape

@Composable
fun RunwayLoadingButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
```

- [ ] **Step 3: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/components/RunwayPrimaryButton.kt android/app/src/main/java/com/runway/android/ui/components/RunwayLoadingButton.kt && git commit -m "feat(android): apply full-pill shape to primary buttons"
```

---

### Task 5: RunwayTextField — 더 둥근 입력 필드

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/components/RunwayTextField.kt`

- [ ] **Step 1: RunwayTextField.kt 교체**

```kotlin
package com.runway.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.GlassBorderDark
import com.runway.android.ui.theme.GlassSurfaceDark
import com.runway.android.ui.theme.LocalIsDarkTheme

@Composable
fun RunwayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
) {
    val isDark = LocalIsDarkTheme.current
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            isError = isError,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
                errorContainerColor = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isDark) GlassBorderDark else MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/components/RunwayTextField.kt && git commit -m "feat(android): apply glass style and larger radius to TextField"
```

---

### Task 6: RunwayBottomNav — 글래스 바텀 네비게이션

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/components/RunwayBottomNav.kt`

- [ ] **Step 1: RunwayBottomNav.kt 교체**

```kotlin
package com.runway.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun RunwayBottomNav(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val topBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = topBorderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        containerColor = if (isDark) Color.White.copy(alpha = 0.04f)
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
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
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
```

- [ ] **Step 2: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/components/RunwayBottomNav.kt && git commit -m "feat(android): apply glass effect to bottom navigation bar"
```

---

### Task 7: 글래스 카드 컴포넌트 — RecentRunCard / RunHistoryCard / StatsSummaryCard / StartRunCard

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/components/RecentRunCard.kt`
- Modify: `android/app/src/main/java/com/runway/android/ui/components/RunHistoryCard.kt`
- Modify: `android/app/src/main/java/com/runway/android/ui/components/StatsSummaryCard.kt`
- Modify: `android/app/src/main/java/com/runway/android/ui/components/StartRunCard.kt`

- [ ] **Step 1: RecentRunCard.kt 교체**

```kotlin
package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.GlassBorderDark
import com.runway.android.ui.theme.GlassSurfaceDark
import com.runway.android.ui.theme.LocalIsDarkTheme

data class RecentRun(
    val runId: String,
    val day: String,
    val distanceKm: String,
    val pace: String,
    val duration: String,
)

@Composable
fun RecentRunCard(
    run: RecentRun,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isDark) GlassBorderDark else MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${run.distanceKm} km · ${run.duration}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${run.day} · Pace ${run.pace}/km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
```

- [ ] **Step 2: RunHistoryCard.kt 교체**

```kotlin
package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.GlassBorderDark
import com.runway.android.ui.theme.GlassSurfaceDark
import com.runway.android.ui.theme.LocalIsDarkTheme

data class RunHistoryItem(
    val runId: String,
    val dateLabel: String,
    val distanceFormatted: String,
    val duration: String,
    val pace: String,
    val status: String,
    val calories: Int?,
    val localDate: java.time.LocalDate? = null,
    val rawDistanceMeters: Double? = null,
    val rawDurationSeconds: Int? = null,
)

@Composable
fun RunHistoryCard(
    item: RunHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isDark) GlassBorderDark else MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.distanceFormatted} · ${item.duration}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.pace,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    item.calories?.let { cal ->
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "$cal kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
```

- [ ] **Step 3: StatsSummaryCard.kt 교체**

```kotlin
package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.GlassBorderDark
import com.runway.android.ui.theme.GlassSurfaceDark
import com.runway.android.ui.theme.LocalIsDarkTheme

@Composable
fun StatsSummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isDark) GlassBorderDark else MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
```

- [ ] **Step 4: StartRunCard.kt 교체**

```kotlin
package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runway.android.ui.theme.GlassBorderDark
import com.runway.android.ui.theme.GlassSurfaceDark
import com.runway.android.ui.theme.LocalIsDarkTheme

@Composable
fun StartRunCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isDark) GlassBorderDark else MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "러닝 시작",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "자유 러닝 · GPS 준비",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
```

- [ ] **Step 5: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/components/RecentRunCard.kt android/app/src/main/java/com/runway/android/ui/components/RunHistoryCard.kt android/app/src/main/java/com/runway/android/ui/components/StatsSummaryCard.kt android/app/src/main/java/com/runway/android/ui/components/StartRunCard.kt && git commit -m "feat(android): apply glassmorphism to list card components"
```

---

### Task 8: DiscoverCourseCard — 하드코딩 배경색 업데이트 + 글래스

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/components/DiscoverCourseCard.kt`

- [ ] **Step 1: DiscoverCourseCard.kt 상단 private val 색상과 Surface 교체**

파일 상단의 하드코딩 색상을 순수 블랙 계열로 변경하고, 카드 Surface에 글래스 적용.

`private val DarkBgTop`, `private val DarkBgBottom` 두 줄을:
```kotlin
private val DarkBgTop    = Color(0xFF0A0A0A)
private val DarkBgBottom = Color(0xFF141414)
```

`DiscoverCourseCard` 내 `Surface`의 `color`와 `border` 파라미터를:
```kotlin
// 기존
color = MaterialTheme.colorScheme.surface,
border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),

// 변경 후 (isDark 변수는 이미 함수 안에 선언되어 있음)
color = if (isDark) GlassSurfaceDark else MaterialTheme.colorScheme.surface,
border = BorderStroke(1.dp, if (isDark) GlassBorderDark else MaterialTheme.colorScheme.outline),
```

파일 import에 추가:
```kotlin
import com.runway.android.ui.theme.GlassBorderDark
import com.runway.android.ui.theme.GlassSurfaceDark
```

- [ ] **Step 2: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/components/DiscoverCourseCard.kt && git commit -m "feat(android): update DiscoverCourseCard hardcoded colors and apply glass"
```

---

### Task 9: RunHeroSection — HeroScrim 색상 업데이트

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/components/RunHeroSection.kt`

- [ ] **Step 1: HeroScrimDark 값 업데이트**

`RunHeroSection.kt` 내 상단 `private val` 라인:
```kotlin
// 기존
private val HeroScrimDark = Color(0xFF0A0B10)

// 변경 후
private val HeroScrimDark = Color(0xFF0A0A0A)
```

- [ ] **Step 2: 시작 버튼 onColor 업데이트**

RunHeroSection의 시작 버튼 내 하드코딩 색상:
```kotlin
// 기존
color = Color(0xFF0A0B10),

// 변경 후
color = MaterialTheme.colorScheme.onPrimary,
```

- [ ] **Step 3: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/components/RunHeroSection.kt && git commit -m "feat(android): align RunHeroSection colors with new pure-black theme"
```

---

### Task 10: SettingsScreen — 악센트 색상 선택 UI 업데이트

SettingsScreen에서 `AccentColor` 선택 UI가 있다면 새 색상(WHITE, ENERGY_ORANGE, ELECTRIC_BLUE)이 보이도록 한다. 이미 enum을 iterate하면 자동 반영되지만, 혹시 특정 색상만 열거한 코드가 있다면 전체 `AccentColor.entries`를 사용하도록 수정한다.

**Files:**
- Modify: `android/app/src/main/java/com/runway/android/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: SettingsScreen.kt 에서 AccentColor 관련 코드 확인**

```bash
grep -n "AccentColor\|accentColor\|accent" /Users/jun/Developer/RunWay/android/app/src/main/java/com/runway/android/ui/settings/SettingsScreen.kt | head -20
```

- [ ] **Step 2: AccentColor.entries 사용 확인**

Step 1 결과에서 특정 색상만 나열하는 코드(예: `listOf(AccentColor.GREEN, AccentColor.RED, ...)`)가 있으면 `AccentColor.entries`로 교체한다. 이미 `entries`를 사용 중이면 변경 불필요.

- [ ] **Step 3: 빌드 확인**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 변경이 있을 경우 커밋**

```bash
cd /Users/jun/Developer/RunWay && git add android/app/src/main/java/com/runway/android/ui/settings/SettingsScreen.kt && git commit -m "feat(android): expose new accent colors in settings"
```

---

### Task 11: 최종 빌드 검증 및 커밋

**Files:** 없음 (빌드 검증만)

- [ ] **Step 1: 전체 클린 빌드**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew clean assembleDebug 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: lint 체크 (옵션)**

```bash
cd /Users/jun/Developer/RunWay/android && ./gradlew lintDebug 2>&1 | grep -E "error:|Error" | head -20
```

- [ ] **Step 3: 완료 확인**

아래 체크리스트 확인:
- [ ] BackgroundDark = #0A0A0A 적용됨
- [ ] PillShape 버튼 적용됨
- [ ] Glass 카드(RecentRunCard, RunHistoryCard, StatsSummaryCard, StartRunCard, DiscoverCourseCard) 적용됨
- [ ] Glass BottomNav 적용됨
- [ ] AccentColor enum에 WHITE/ENERGY_ORANGE/ELECTRIC_BLUE 추가됨
- [ ] 스플래시 화면 미변경 확인
