package com.runway.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HomeWeatherCard(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    val condition = weather.toWeatherPresentation()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = condition.color.copy(alpha = 0.16f),
                ) {
                    Icon(
                        imageVector = condition.icon,
                        contentDescription = condition.title,
                        tint = condition.color,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "오늘의 날씨",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = condition.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = condition.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = "${weather.tempCelsius}°",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WeatherMetric(label = "습도", value = "${weather.humidity}%")
                WeatherMetric(
                    label = "미세먼지",
                    value = if (weather.pm10 > 0) pm10Quality(weather.pm10) else "-",
                    valueColor = if (weather.pm10 > 0) pm10Color(weather.pm10) else null,
                )
                WeatherMetric(
                    label = "초미세먼지",
                    value = if (weather.pm25 > 0) pm25Quality(weather.pm25) else "-",
                    valueColor = if (weather.pm25 > 0) pm25Color(weather.pm25) else null,
                )
            }
        }
    }
}

@Composable
private fun WeatherMetric(
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class WeatherPresentation(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)

private fun WeatherInfo.toWeatherPresentation(): WeatherPresentation {
    val fallbackDescription = description.takeIf { it.isNotBlank() }

    return when {
        conditionId in 200..232 -> WeatherPresentation(
            title = "천둥번개",
            description = fallbackDescription ?: "강한 비와 낙뢰에 주의하세요.",
            icon = Icons.Filled.Thunderstorm,
            color = Color(0xFF8B82D8),
        )
        conditionId in 300..321 -> WeatherPresentation(
            title = "이슬비",
            description = fallbackDescription ?: "가벼운 비가 내리고 있어요.",
            icon = Icons.Filled.Grain,
            color = Color(0xFF62A7D8),
        )
        conditionId in 500..531 -> WeatherPresentation(
            title = "비",
            description = fallbackDescription ?: "우산을 챙겨 외출하세요.",
            icon = Icons.Filled.Umbrella,
            color = Color(0xFF4D91C6),
        )
        conditionId in 600..622 -> WeatherPresentation(
            title = "눈",
            description = fallbackDescription ?: "노면이 미끄러울 수 있어요.",
            icon = Icons.Filled.AcUnit,
            color = Color(0xFF83B6D9),
        )
        conditionId in 700..781 -> WeatherPresentation(
            title = "안개",
            description = fallbackDescription ?: "가시거리가 짧으니 주의하세요.",
            icon = Icons.Filled.Air,
            color = Color(0xFF8E969D),
        )
        conditionId == 800 -> WeatherPresentation(
            title = "맑음",
            description = fallbackDescription ?: "야외 러닝하기 좋은 날씨예요.",
            icon = Icons.Filled.WbSunny,
            color = Color(0xFFF0B83F),
        )
        conditionId in 801..804 -> WeatherPresentation(
            title = if (conditionId <= 802) "구름 조금" else "흐림",
            description = fallbackDescription ?: "구름이 낀 선선한 날씨예요.",
            icon = Icons.Filled.Cloud,
            color = Color(0xFF83909A),
        )
        condition.equals("rain", ignoreCase = true) -> WeatherPresentation(
            title = "비",
            description = fallbackDescription ?: "우산을 챙겨 외출하세요.",
            icon = Icons.Filled.WaterDrop,
            color = Color(0xFF4D91C6),
        )
        else -> WeatherPresentation(
            title = condition.ifBlank { "현재 날씨" },
            description = fallbackDescription ?: "현재 기상 정보를 확인하세요.",
            icon = Icons.Filled.Cloud,
            color = Color(0xFF83909A),
        )
    }
}
