package com.runway.android.ui.home

import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import kotlin.math.roundToInt

internal class AirKoreaClient(
    private val httpClient: OkHttpClient,
    private val gson: Gson = Gson(),
) {
    fun getAirQuality(
        latitude: Double,
        longitude: Double,
        serviceKey: String,
        stationCandidates: List<String> = emptyList(),
    ): AirKoreaReading? {
        if (serviceKey.isBlank()) return null

        return runCatching {
            val tmCoordinate = AirKoreaCoordinateConverter.toTm(latitude, longitude)
            val nearestStation = findNearestStation(tmCoordinate, serviceKey)
            buildList {
                nearestStation?.let(::add)
                addAll(stationCandidates)
            }.distinct().firstNotNullOfOrNull { stationName ->
                getLatestReading(stationName, serviceKey)
            }
        }.getOrNull()
    }

    private fun findNearestStation(
        coordinate: TmCoordinate,
        serviceKey: String,
    ): String? {
        val url = NEARBY_STATION_URL.toHttpUrl().newBuilder()
            .addQueryParameter("serviceKey", serviceKey)
            .addQueryParameter("returnType", "json")
            .addQueryParameter("tmX", coordinate.x.toString())
            .addQueryParameter("tmY", coordinate.y.toString())
            .addQueryParameter("ver", "1.1")
            .build()

        val response = execute(url.toString(), NearbyStationResponse::class.java)
        return response?.response?.body?.items
            ?.minByOrNull { it.tm?.toDoubleOrNull() ?: Double.MAX_VALUE }
            ?.stationName
            ?.takeIf(String::isNotBlank)
    }

    private fun getLatestReading(
        stationName: String,
        serviceKey: String,
    ): AirKoreaReading? {
        val url = STATION_READING_URL.toHttpUrl().newBuilder()
            .addQueryParameter("serviceKey", serviceKey)
            .addQueryParameter("returnType", "json")
            .addQueryParameter("numOfRows", "1")
            .addQueryParameter("pageNo", "1")
            .addQueryParameter("stationName", stationName)
            .addQueryParameter("dataTerm", "DAILY")
            .addQueryParameter("ver", "1.3")
            .build()

        val item = execute(url.toString(), StationReadingResponse::class.java)
            ?.response?.body?.items?.firstOrNull()
            ?: return null
        val pm10 = item.pm10Value.toMeasurement()
        val pm25 = item.pm25Value.toMeasurement()
        if (pm10 == null && pm25 == null) return null

        return AirKoreaReading(
            pm10 = pm10,
            pm25 = pm25,
            stationName = stationName,
            measuredAt = item.dataTime.orEmpty(),
        )
    }

    private fun <T> execute(url: String, responseType: Class<T>): T? {
        val request = Request.Builder().url(url).get().build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.charStream()?.use { gson.fromJson(it, responseType) }
        }
    }

    private fun String?.toMeasurement(): Int? {
        return this?.takeUnless { it == "-" }?.toDoubleOrNull()?.roundToInt()
    }

    private data class NearbyStationResponse(val response: NearbyResponse?)
    private data class NearbyResponse(val body: NearbyBody?)
    private data class NearbyBody(val items: List<NearbyStation> = emptyList())
    private data class NearbyStation(
        val stationName: String = "",
        val tm: String? = null,
    )

    private data class StationReadingResponse(val response: ReadingResponse?)
    private data class ReadingResponse(val body: ReadingBody?)
    private data class ReadingBody(val items: List<ReadingItem> = emptyList())
    private data class ReadingItem(
        val pm10Value: String? = null,
        val pm25Value: String? = null,
        val dataTime: String? = null,
    )

    private companion object {
        const val NEARBY_STATION_URL =
            "https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList"
        const val STATION_READING_URL =
            "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty"
    }
}

internal data class AirKoreaReading(
    val pm10: Int?,
    val pm25: Int?,
    val stationName: String,
    val measuredAt: String,
)

internal data class TmCoordinate(val x: Double, val y: Double)

internal object AirKoreaCoordinateConverter {
    private val transform = CoordinateTransformFactory().createTransform(
        CRSFactory().createFromParameters(
            "WGS84",
            "+proj=longlat +datum=WGS84 +no_defs",
        ),
        CRSFactory().createFromParameters(
            "AIR_KOREA_TM",
            "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 " +
                "+x_0=200000 +y_0=500000 +ellps=bessel " +
                "+towgs84=-146.43,507.89,681.46 +units=m +no_defs",
        ),
    )

    fun toTm(latitude: Double, longitude: Double): TmCoordinate {
        val output = ProjCoordinate()
        transform.transform(ProjCoordinate(longitude, latitude), output)
        return TmCoordinate(output.x, output.y)
    }
}
