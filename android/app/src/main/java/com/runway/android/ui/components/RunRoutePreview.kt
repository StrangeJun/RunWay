package com.runway.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.runway.android.data.running.model.RunPointResponse

/**
 * 실제 GPS 포인트를 Canvas에 렌더링하는 경로 미리보기.
 * 포인트가 2개 미만이면 플레이스홀더를 표시한다.
 */
@Composable
fun RunRoutePreview(
    points: List<RunPointResponse>,
    modifier: Modifier = Modifier,
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    if (points.size < 2) {
        Box(
            modifier = modifier.background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "경로 데이터 없음",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val lats = points.map { it.latitude }
    val lngs = points.map { it.longitude }
    val minLat = lats.min()
    val maxLat = lats.max()
    val minLng = lngs.min()
    val maxLng = lngs.max()

    Box(modifier = modifier.background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = 28.dp.toPx()
            val drawW = size.width - pad * 2
            val drawH = size.height - pad * 2

            val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
            val lngRange = (maxLng - minLng).coerceAtLeast(0.0001)

            fun norm(lat: Double, lng: Double): Offset {
                val x = ((lng - minLng) / lngRange * drawW + pad).toFloat()
                val y = (((maxLat - lat) / latRange * drawH) + pad).toFloat()
                return Offset(x, y)
            }

            // 경로 선
            val path = Path().apply {
                val first = norm(points[0].latitude, points[0].longitude)
                moveTo(first.x, first.y)
                for (i in 1 until points.size) {
                    val pt = norm(points[i].latitude, points[i].longitude)
                    lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // 시작점 (primary 색상 점)
            val startPt = norm(points.first().latitude, points.first().longitude)
            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = startPt)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = startPt)

            // 종료점 (error 색상 점)
            val endPt = norm(points.last().latitude, points.last().longitude)
            drawCircle(color = errorColor, radius = 6.dp.toPx(), center = endPt)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = endPt)
        }
    }
}
