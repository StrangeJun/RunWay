package com.runway.android.core.share

import android.content.Context
import android.content.Intent

object ShareUtils {
    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    fun shareCourse(
        context: Context,
        name: String,
        distanceKm: String,
        completionCount: Int,
        isLoop: Boolean,
    ) {
        val loopTag = if (isLoop) " (루프 코스)" else ""
        val text = buildString {
            appendLine("RunWay 코스 공유 🏃")
            appendLine()
            appendLine("📍 $name$loopTag")
            appendLine("📏 거리: $distanceKm")
            appendLine("🏅 완주 횟수: ${completionCount}회")
            appendLine()
            append("RunWay 앱에서 이 코스에 도전해보세요!")
        }
        shareText(context, "코스 공유 — $name", text)
    }

    fun shareRunSummary(
        context: Context,
        distanceFormatted: String,
        duration: String,
        pace: String,
        date: String,
    ) {
        val text = buildString {
            appendLine("RunWay 러닝 기록 🏃")
            appendLine()
            appendLine("📅 $date")
            appendLine("📏 거리: $distanceFormatted")
            appendLine("⏱ 시간: $duration")
            appendLine("🚀 페이스: $pace")
            appendLine()
            append("RunWay에서 기록하세요!")
        }
        shareText(context, "러닝 기록 공유", text)
    }
}
