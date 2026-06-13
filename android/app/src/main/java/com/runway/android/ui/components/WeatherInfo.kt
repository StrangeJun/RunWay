package com.runway.android.ui.components

import androidx.compose.ui.graphics.Color

data class WeatherInfo(
    val tempCelsius: Int,
    val humidity: Int,
    val pm10: Int,
    val pm25: Int,
    val condition: WeatherCondition,
)

enum class WeatherCondition {
    CLEAR,
    MOSTLY_CLOUDY,
    CLOUDY,
    RAIN,
    SLEET,
    SNOW,
    SHOWER,
    DRIZZLE,
    DRIZZLE_AND_FLURRY,
    FLURRY,
}

fun pm10Quality(pm10: Int): String = when {
    pm10 <= 30 -> "좋음"
    pm10 <= 80 -> "보통"
    pm10 <= 150 -> "나쁨"
    else -> "매우나쁨"
}

fun pm25Quality(pm25: Int): String = when {
    pm25 <= 15 -> "좋음"
    pm25 <= 35 -> "보통"
    pm25 <= 75 -> "나쁨"
    else -> "매우나쁨"
}

fun pm10Color(pm10: Int): Color = when {
    pm10 <= 30 -> Color(0xFF7EC850)
    pm10 <= 80 -> Color(0xFFE8C44B)
    pm10 <= 150 -> Color(0xFFE8904B)
    else -> Color(0xFFE84B4B)
}

fun pm25Color(pm25: Int): Color = when {
    pm25 <= 15 -> Color(0xFF7EC850)
    pm25 <= 35 -> Color(0xFFE8C44B)
    pm25 <= 75 -> Color(0xFFE8904B)
    else -> Color(0xFFE84B4B)
}
