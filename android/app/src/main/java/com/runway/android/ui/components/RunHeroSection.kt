package com.runway.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runway.android.BuildConfig
import com.runway.android.core.map.MapPoint
import com.runway.android.ui.home.RunGoal
import com.runway.android.ui.theme.LocalIsDarkTheme

private val HeroScrimDark = Color(0xFF0A0A0A)
private val HeroScrimLight = Color(0xFFF5F5FA)

@Composable
fun RunHeroSection(
    onStartRun: () -> Unit,
    onSetGoal: () -> Unit = {},
    selectedGoal: RunGoal? = null,
    weatherInfo: WeatherInfo? = null,
    currentLocation: MapPoint? = null,
    hasLocationPermission: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val heroScrim = if (isDark) HeroScrimDark else HeroScrimLight
    val onHero = if (isDark) Color.White else Color(0xFF111119)
    val pillBg = if (isDark) Color(0x1EFFFFFF) else Color(0x14000000)

    Box(modifier = modifier) {

        // ── Real Google Map background ────────────────────────────────────
        HomeMapView(
            currentLocation = currentLocation,
            hasLocationPermission = hasLocationPermission,
            modifier = Modifier.fillMaxSize(),
        )

        // ── Bottom scrim — starts at 80% height for a more gradual fade ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.28f to heroScrim.copy(alpha = 0.55f),
                        1.00f to heroScrim,
                    )
                ),
        )

        // ── Header tagline ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(
                text = BuildConfig.APP_NAME.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.8.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Run your way.",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = onHero,
            )
        }

        // ── GPS status + weather summary ──────────────────────────────────
        GpsStatusPill(
            pillBg = pillBg,
            onPill = onHero,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 102.dp)
                .padding(horizontal = 20.dp),
        )
        WeatherSummaryPill(
            weatherInfo = weatherInfo,
            pillBg = pillBg,
            onPill = onHero,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 142.dp)
                .padding(horizontal = 20.dp),
        )

        // ── 스크롤 유도 애니메이션 (좌/우) ──────────────────────────────────
        ScrollHintIndicator(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 88.dp),
        )
        ScrollHintIndicator(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 88.dp),
        )

        // ── 시작 button + 목표 설정 ─────────────────────────────────────────
        val startButtonInteraction = remember { MutableInteractionSource() }
        val isStartPressed by startButtonInteraction.collectIsPressedAsState()
        val startButtonScale by animateFloatAsState(
            targetValue = if (isStartPressed) 0.93f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "startButtonScale",
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = onStartRun,
                modifier = Modifier.size(96.dp).scale(startButtonScale),
                interactionSource = startButtonInteraction,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 14.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "시작",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                onClick = onSetGoal,
                shape = RoundedCornerShape(50),
                color = if (selectedGoal != null) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    pillBg
                },
            ) {
                Text(
                    text = when (selectedGoal) {
                        is RunGoal.TimeGoal -> "목표설정: ${selectedGoal.label()}"
                        is RunGoal.DistanceGoal -> "목표설정: ${selectedGoal.label()}"
                        is RunGoal.IntervalGoal -> "목표설정: ${selectedGoal.label()}"
                        null -> "목표 설정"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedGoal != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        onHero.copy(alpha = 0.65f)
                    },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ScrollHintIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scroll_hint")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Column(
        modifier = modifier.offset(y = offsetY.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.White.copy(alpha = alpha * 0.4f),
            modifier = Modifier
                .size(30.dp)
                .offset(y = (-6).dp),
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.White.copy(alpha = alpha),
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun GpsStatusPill(
    pillBg: Color,
    onPill: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(pillBg)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Text(
            text = "GPS 준비 완료",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = onPill.copy(alpha = 0.90f),
        )
    }
}

@Composable
private fun WeatherSummaryPill(
    weatherInfo: WeatherInfo?,
    pillBg: Color,
    onPill: Color,
    modifier: Modifier = Modifier,
) {
    val shimmerTransition = rememberInfiniteTransition(label = "weather_shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "weatherShimmerX",
    )

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = 340.dp)
            .wrapContentWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pillBg)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        val compact = maxWidth < 330.dp
        Column(
            modifier = Modifier.wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
        if (weatherInfo != null) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = weatherInfo.weatherIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = weatherInfo.weatherLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onPill,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Spacer(Modifier.width(if (compact) 8.dp else 14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp)) {
                    WeatherChip(
                        text = "${weatherInfo.tempCelsius}°C",
                        onPill = onPill,
                        emphasized = true,
                        icon = Icons.Filled.Thermostat,
                    )
                    WeatherChip(
                        text = "습도 ${weatherInfo.humidity}%",
                        onPill = onPill,
                        emphasized = true,
                        icon = Icons.Filled.WaterDrop,
                    )
                }
            }

            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                DustChip(
                    label = if (compact) "미세" else "미세먼지",
                    value = weatherInfo.pm10.takeIf { it > 0 },
                    quality = if (weatherInfo.pm10 > 0) pm10Quality(weatherInfo.pm10) else "-",
                    color = if (weatherInfo.pm10 > 0) pm10Color(weatherInfo.pm10) else onPill.copy(alpha = 0.55f),
                    onPill = onPill,
                    compact = compact,
                )
                Spacer(Modifier.width(if (compact) 8.dp else 14.dp))
                DustChip(
                    label = if (compact) "초미세" else "초미세먼지",
                    value = weatherInfo.pm25.takeIf { it > 0 },
                    quality = if (weatherInfo.pm25 > 0) pm25Quality(weatherInfo.pm25) else "-",
                    color = if (weatherInfo.pm25 > 0) pm25Color(weatherInfo.pm25) else onPill.copy(alpha = 0.55f),
                    onPill = onPill,
                    compact = compact,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(onPill.copy(alpha = 0.07f))
                    .drawWithContent {
                        drawContent()
                        val center = size.width * shimmerOffset
                        val half = size.width * 0.5f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, onPill.copy(0.22f), Color.Transparent),
                                startX = center - half,
                                endX = center + half,
                            ),
                        )
                    },
            )
        }
        }
    }
}

private fun WeatherInfo.weatherLabel(): String = when {
    conditionId in 200..232 -> "천둥번개"
    conditionId in 300..321 -> "이슬비"
    conditionId in 500..531 -> "비"
    conditionId in 600..622 -> "눈"
    conditionId in 700..781 -> "안개"
    conditionId == 800 -> "맑음"
    conditionId in 801..802 -> "구름 조금"
    conditionId in 803..804 -> "흐림"
    else -> description.ifBlank { condition.ifBlank { "현재 날씨" } }
}

private fun WeatherInfo.weatherIcon() = when {
    conditionId in 200..232 -> Icons.Filled.Thunderstorm
    conditionId in 300..321 -> Icons.Filled.Grain
    conditionId in 500..531 -> Icons.Filled.Umbrella
    conditionId in 600..622 -> Icons.Filled.AcUnit
    conditionId in 700..781 -> Icons.Filled.Air
    conditionId == 800 -> Icons.Filled.WbSunny
    else -> Icons.Filled.Cloud
}

@Composable
private fun WeatherChip(
    text: String,
    onPill: Color,
    emphasized: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            style = if (emphasized) {
                MaterialTheme.typography.labelLarge
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasized) onPill else onPill.copy(alpha = 0.75f),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun DustChip(
    label: String,
    value: Int?,
    quality: String,
    color: Color,
    onPill: Color,
    compact: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            fontSize = if (compact) 11.sp else 12.sp,
            color = onPill.copy(alpha = 0.55f),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Text(
            text = value?.let { "$it㎍/㎥ $quality" } ?: quality,
            fontSize = if (compact) 10.sp else 11.sp,
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
    }
}
