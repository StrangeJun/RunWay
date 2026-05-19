package com.runway.android.core.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

fun List<MapPoint>.toLatLngBounds(paddingFraction: Double = 0.10): LatLngBounds {
    val minLat = minOf { it.latitude }
    val maxLat = maxOf { it.latitude }
    val minLng = minOf { it.longitude }
    val maxLng = maxOf { it.longitude }

    val latPad = (maxLat - minLat).coerceAtLeast(0.001) * paddingFraction
    val lngPad = (maxLng - minLng).coerceAtLeast(0.001) * paddingFraction

    return LatLngBounds(
        LatLng(minLat - latPad, minLng - lngPad),
        LatLng(maxLat + latPad, maxLng + lngPad),
    )
}
