package com.runway.android.core.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.runway.android.BuildConfig
import com.runway.android.data.running.model.RunDetailResponse
import com.runway.android.data.running.model.RunPointResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ImageCaptureUtil {

    private const val SIZE = 1080

    fun draw(detail: RunDetailResponse, template: ShareTemplate, preset: MetricPreset = MetricPreset.FULL_STATS): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCard(canvas, detail, template, preset)
        return bitmap
    }

    private fun drawCard(canvas: Canvas, detail: RunDetailResponse, template: ShareTemplate, preset: MetricPreset) {
        val W = SIZE.toFloat()
        val H = SIZE.toFloat()

        // Background
        canvas.drawColor(template.bgColor.toInt())

        // Top accent bar
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = template.accentColor.toInt() }
        canvas.drawRect(0f, 0f, W, 10f, accentPaint)

        // App wordmark
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.textPrimary.toInt()
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.25f
        }
        val wordmark = BuildConfig.APP_NAME.uppercase()
        canvas.drawText(wordmark, 80f, 120f, brandPaint)

        // Accent dot after wordmark
        canvas.drawCircle(80f + brandPaint.measureText(wordmark) + 18f, 107f, 8f, accentPaint)

        // Date
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.textSecondary.toInt()
            textSize = 38f
        }
        canvas.drawText(formatDate(detail.startedAt), 80f, 176f, datePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = template.accentColor.toInt()
            strokeWidth = 2f
            alpha = 100
        }
        canvas.drawLine(80f, 210f, W - 80f, 210f, dividerPaint)

        // Distance (hero number)
        val distKm = (detail.distanceMeters ?: 0.0) / 1000.0
        val distPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.textPrimary.toInt()
            textSize = 210f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("%.2f".format(distKm), 70f, 460f, distPaint)

        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.textSecondary.toInt()
            textSize = 56f
        }
        canvas.drawText("km", 80f, 530f, unitPaint)

        // Metrics row (preset-dependent)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.textSecondary.toInt()
            textSize = 34f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.textPrimary.toInt()
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val col1 = 80f
        val col2 = W / 2 - 80f
        val col3 = W - 280f

        when (preset) {
            MetricPreset.FULL_STATS -> {
                canvas.drawText("시간", col1, 640f, labelPaint)
                canvas.drawText(formatDuration(detail.durationSeconds), col1, 700f, valuePaint)
                canvas.drawText("평균 페이스", col2, 640f, labelPaint)
                canvas.drawText(formatPace(detail.avgPaceSecondsPerKm), col2, 700f, valuePaint)
                if (detail.caloriesBurned != null) {
                    canvas.drawText("칼로리", col3, 640f, labelPaint)
                    canvas.drawText("${detail.caloriesBurned}", col3, 700f, valuePaint)
                    canvas.drawText("kcal", col3, 740f, labelPaint)
                }
            }
            MetricPreset.DISTANCE_FOCUS -> {
                canvas.drawText("소요 시간", col1, 640f, labelPaint)
                canvas.drawText(formatDuration(detail.durationSeconds), col1, 700f, valuePaint)
            }
            MetricPreset.PACE_FOCUS -> {
                val paceBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = template.accentColor.toInt()
                    textSize = 100f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("평균 페이스", col1, 600f, labelPaint)
                canvas.drawText(formatPace(detail.avgPaceSecondsPerKm), col1, 710f, paceBigPaint)
                canvas.drawText("소요 시간", col1, 780f, labelPaint)
                canvas.drawText(formatDuration(detail.durationSeconds), col1, 840f, valuePaint)
            }
        }

        // Second divider
        val dividerY = if (preset == MetricPreset.PACE_FOCUS) 880f else 760f
        canvas.drawLine(80f, dividerY, W - 80f, dividerY, dividerPaint)

        // Route preview (if enough points)
        val hasRoute = detail.points.size >= 2
        val routeTop = if (preset == MetricPreset.PACE_FOCUS) 900f else 785f
        val routeBounds = if (hasRoute) RectF(80f, routeTop, W - 80f, H - 80f) else null
        if (routeBounds != null) {
            drawRoute(canvas, detail.points, template, routeBounds)
        }

        // Motivational phrase
        val phrasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.accentColor.toInt()
            textSize = 34f
            letterSpacing = 0.12f
        }
        val phrase = if (routeBounds == null) "Run your way." else ""
        if (phrase.isNotEmpty()) {
            canvas.drawText(phrase, 80f, H - 40f, phrasePaint)
        }
    }

    private fun drawRoute(
        canvas: Canvas,
        points: List<RunPointResponse>,
        template: ShareTemplate,
        bounds: RectF,
    ) {
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)

        val margin = 20f
        val innerW = bounds.width() - margin * 2
        val innerH = bounds.height() - margin * 2

        fun toX(lon: Double) = bounds.left + margin + ((lon - minLon) / lonRange * innerW).toFloat()
        fun toY(lat: Double) = bounds.bottom - margin - ((lat - minLat) / latRange * innerH).toFloat()

        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.accentColor.toInt()
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        val path = Path()
        path.moveTo(toX(points[0].longitude), toY(points[0].latitude))
        points.drop(1).forEach { p -> path.lineTo(toX(p.longitude), toY(p.latitude)) }
        canvas.drawPath(path, routePaint)

        // Start and end dots
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = template.accentColor.toInt() }
        canvas.drawCircle(toX(points.first().longitude), toY(points.first().latitude), 14f, dotPaint)
        canvas.drawCircle(toX(points.last().longitude), toY(points.last().latitude), 14f, dotPaint)
    }

    private fun formatDate(isoDate: String?): String {
        if (isoDate == null) return ""
        return runCatching {
            val instant = Instant.parse(isoDate)
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        }.getOrDefault("")
    }

    private fun formatDuration(seconds: Int?): String {
        if (seconds == null) return "--:--"
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun formatPace(secsPerKm: Int?): String {
        if (secsPerKm == null || secsPerKm <= 0) return "--'--\""
        return "%d'%02d\"".format(secsPerKm / 60, secsPerKm % 60)
    }
}
