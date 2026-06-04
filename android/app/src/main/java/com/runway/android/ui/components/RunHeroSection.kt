package com.runway.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runway.android.core.map.MapPoint
import com.runway.android.ui.home.RunGoal

private val HeroScrim = Color(0xFF0A0B10)
private val LimeGreen = Color(0xFFA4E168)
private val PillBg = Color(0x1EFFFFFF)

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
    Box(modifier = modifier) {

        // ── Real Google Map background ────────────────────────────────────
        HomeMapView(
            currentLocation = currentLocation,
            hasLocationPermission = hasLocationPermission,
            modifier = Modifier.fillMaxSize(),
        )

        // ── Bottom scrim ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.35f to HeroScrim.copy(alpha = 0.70f),
                        1.00f to HeroScrim,
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
                text = "YOUR RUNWAY",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.5.sp,
                color = LimeGreen,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Run your way.",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = Color.White,
            )
        }

        // ── GPS + Weather pill ────────────────────────────────────────────
        GpsWeatherPill(
            weatherInfo = weatherInfo,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
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
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = onStartRun,
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = LimeGreen,
                shadowElevation = 14.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "시작",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0A0B10),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                onClick = onSetGoal,
                shape = RoundedCornerShape(50),
                color = if (selectedGoal != null) LimeGreen.copy(alpha = 0.18f) else PillBg,
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
                    color = if (selectedGoal != null) LimeGreen else Color.White.copy(alpha = 0.65f),
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
private fun GpsWeatherPill(
    weatherInfo: WeatherInfo?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PillBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Row 1: GPS status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(LimeGreen, CircleShape),
            )
            Text(
                text = "GPS 준비 완료",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.90f),
            )
        }

        // Row 2: Weather — only shown when data is available
        if (weatherInfo != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WeatherChip(text = "${weatherInfo.tempCelsius}°C")
                WeatherChip(text = "습도 ${weatherInfo.humidity}%")

                if (weatherInfo.pm10 > 0) {
                    DustChip(
                        label = "PM10",
                        quality = pm10Quality(weatherInfo.pm10),
                        color = pm10Color(weatherInfo.pm10),
                    )
                }
                if (weatherInfo.pm25 > 0) {
                    DustChip(
                        label = "PM2.5",
                        quality = pm25Quality(weatherInfo.pm25),
                        color = pm25Color(weatherInfo.pm25),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherChip(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = Color.White.copy(alpha = 0.75f),
    )
}

@Composable
private fun DustChip(label: String, quality: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.55f),
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Text(
            text = quality,
            fontSize = 11.sp,
            color = color,
        )
    }
}
