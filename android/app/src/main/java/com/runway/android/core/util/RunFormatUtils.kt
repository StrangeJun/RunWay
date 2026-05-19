package com.runway.android.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

fun formatDistance(meters: Double?): String {
    if (meters == null) return "--"
    return if (meters < 1000.0) "${meters.toInt()} m"
    else "%.2f km".format(meters / 1000.0)
}

fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds < 0) return "--:--"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

fun formatPace(secsPerKm: Int?): String {
    if (secsPerKm == null || secsPerKm <= 0) return "--'--\" /km"
    return "%d'%02d\" /km".format(secsPerKm / 60, secsPerKm % 60)
}

fun formatRunDateShort(isoString: String?): String {
    if (isoString == null) return ""
    return runCatching {
        val dt = Instant.parse(isoString).atZone(ZoneId.systemDefault())
        val month = dt.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        "$month ${dt.dayOfMonth}, ${dt.year}"
    }.getOrDefault("")
}

fun formatRunDateFull(isoString: String?): String {
    if (isoString == null) return ""
    return runCatching {
        val dt = Instant.parse(isoString).atZone(ZoneId.systemDefault())
        val month = dt.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        "$month ${dt.dayOfMonth}, ${dt.year}"
    }.getOrDefault("")
}

fun formatTime(isoString: String?): String {
    if (isoString == null) return ""
    return runCatching {
        val dt = Instant.parse(isoString).atZone(ZoneId.systemDefault())
        "%02d:%02d".format(dt.hour, dt.minute)
    }.getOrDefault("")
}
