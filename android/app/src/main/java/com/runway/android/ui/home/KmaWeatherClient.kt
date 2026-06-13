package com.runway.android.ui.home

import com.google.gson.Gson
import com.runway.android.ui.components.WeatherCondition
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

internal class KmaWeatherClient(
    private val httpClient: OkHttpClient,
    private val gson: Gson = Gson(),
) {
    fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        serviceKey: String,
        now: ZonedDateTime = ZonedDateTime.now(KOREA_ZONE),
    ): KmaWeatherReading? {
        if (serviceKey.isBlank()) return null

        val grid = KmaGridConverter.toGrid(latitude, longitude)
        val baseDateTime = resolveBaseDateTime(now)
        val url = FORECAST_URL.toHttpUrl().newBuilder()
            .addQueryParameter("serviceKey", serviceKey)
            .addQueryParameter("pageNo", "1")
            .addQueryParameter("numOfRows", "1000")
            .addQueryParameter("dataType", "JSON")
            .addQueryParameter("base_date", baseDateTime.format(DATE_FORMATTER))
            .addQueryParameter("base_time", baseDateTime.format(TIME_FORMATTER))
            .addQueryParameter("nx", grid.x.toString())
            .addQueryParameter("ny", grid.y.toString())
            .build()

        val request = Request.Builder().url(url).get().build()
        val items = runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.charStream() ?: return null
                gson.fromJson(body, ForecastResponse::class.java)
                    ?.response
                    ?.takeIf { it.header?.resultCode == "00" }
                    ?.body
                    ?.items
                    ?.item
                    .orEmpty()
            }
        }.getOrNull().orEmpty()
        if (items.isEmpty()) return null

        val targetTime = items.mapNotNull { it.toForecastDateTime() }
            .distinct()
            .minByOrNull { forecastTime ->
                val seconds = Duration.between(now.toLocalDateTime(), forecastTime).seconds
                if (seconds >= 0) seconds else Long.MAX_VALUE / 2 + -seconds
            } ?: return null
        val values = items.asSequence()
            .filter { it.toForecastDateTime() == targetTime }
            .associate { it.category to it.fcstValue }

        val temperature = values["T1H"]?.toDoubleOrNull()?.toInt() ?: return null
        val humidity = values["REH"]?.toDoubleOrNull()?.toInt() ?: return null
        val precipitationType = values["PTY"]?.toIntOrNull() ?: 0
        val sky = values["SKY"]?.toIntOrNull() ?: 1

        return KmaWeatherReading(
            temperatureCelsius = temperature,
            humidity = humidity,
            condition = resolveCondition(precipitationType, sky),
        )
    }

    private fun resolveBaseDateTime(now: ZonedDateTime): ZonedDateTime {
        val availableTime = now.minusMinutes(45)
        val baseMinute = if (availableTime.minute >= 30) 30 else 0
        return availableTime.withMinute(baseMinute).withSecond(0).withNano(0)
    }

    private fun resolveCondition(
        precipitationType: Int,
        sky: Int,
    ): WeatherCondition = when (precipitationType) {
        1 -> WeatherCondition.RAIN
        2 -> WeatherCondition.SLEET
        3 -> WeatherCondition.SNOW
        4 -> WeatherCondition.SHOWER
        5 -> WeatherCondition.DRIZZLE
        6 -> WeatherCondition.DRIZZLE_AND_FLURRY
        7 -> WeatherCondition.FLURRY
        else -> when (sky) {
            1 -> WeatherCondition.CLEAR
            3 -> WeatherCondition.MOSTLY_CLOUDY
            4 -> WeatherCondition.CLOUDY
            else -> WeatherCondition.CLEAR
        }
    }

    private fun ForecastItem.toForecastDateTime(): LocalDateTime? = runCatching {
        LocalDateTime.parse(fcstDate + fcstTime, FORECAST_DATE_TIME_FORMATTER)
    }.getOrNull()

    private data class ForecastResponse(val response: Response?)
    private data class Response(val header: Header?, val body: Body?)
    private data class Header(val resultCode: String?)
    private data class Body(val items: Items?)
    private data class Items(val item: List<ForecastItem> = emptyList())
    private data class ForecastItem(
        val category: String = "",
        val fcstDate: String = "",
        val fcstTime: String = "",
        val fcstValue: String = "",
    )

    private companion object {
        const val FORECAST_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst"
        val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
        val FORECAST_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    }
}

internal data class KmaWeatherReading(
    val temperatureCelsius: Int,
    val humidity: Int,
    val condition: WeatherCondition,
)

internal data class KmaGrid(val x: Int, val y: Int)

internal object KmaGridConverter {
    private const val EARTH_RADIUS_KM = 6371.00877
    private const val GRID_SIZE_KM = 5.0
    private const val STANDARD_LATITUDE_1 = 30.0
    private const val STANDARD_LATITUDE_2 = 60.0
    private const val ORIGIN_LONGITUDE = 126.0
    private const val ORIGIN_LATITUDE = 38.0
    private const val ORIGIN_X = 43.0
    private const val ORIGIN_Y = 136.0

    fun toGrid(latitude: Double, longitude: Double): KmaGrid {
        val radius = EARTH_RADIUS_KM / GRID_SIZE_KM
        val slat1 = STANDARD_LATITUDE_1.toRadians()
        val slat2 = STANDARD_LATITUDE_2.toRadians()
        val originLongitude = ORIGIN_LONGITUDE.toRadians()
        val originLatitude = ORIGIN_LATITUDE.toRadians()

        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        var sf = tan(PI * 0.25 + slat1 * 0.5).pow(sn) * cos(slat1) / sn
        var ro = tan(PI * 0.25 + originLatitude * 0.5)
        ro = radius * sf / ro.pow(sn)

        var ra = tan(PI * 0.25 + latitude.toRadians() * 0.5)
        ra = radius * sf / ra.pow(sn)
        var theta = longitude.toRadians() - originLongitude
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn

        return KmaGrid(
            x = floor(ra * sin(theta) + ORIGIN_X + 0.5).toInt(),
            y = floor(ro - ra * cos(theta) + ORIGIN_Y + 0.5).toInt(),
        )
    }

    private fun Double.toRadians(): Double = this * PI / 180.0
}
