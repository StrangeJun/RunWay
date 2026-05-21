package com.runway.android.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val HeroScrim = Color(0xFF0A0B10)
private val LimeGreen = Color(0xFFA4E168)
private val PillBg = Color(0x1EFFFFFF)
private val BtnBg = Color(0x26FFFFFF)

@Composable
fun RunHeroSection(
    onStartRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {

        // ── Map canvas background ─────────────────────────────────────────
        HomeMapBackground(modifier = Modifier.fillMaxSize())

        // ── Bottom scrim so controls stay readable over the map ───────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.40f to HeroScrim.copy(alpha = 0.65f),
                        1.00f to HeroScrim,
                    )
                ),
        )

        // ── Top header overlay ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp),
        ) {
            Text(
                text = "러닝",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroTabChip(text = "바로 시작", selected = true)
                HeroTabChip(text = "러닝 가이드", selected = false)
            }
        }

        // ── GPS status pill ───────────────────────────────────────────────
        GpsStatusPill(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 124.dp),
        )

        // ── Run start controls (bottom of hero) ───────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // [ 음악 ]  [ 시작 ]  [ 유형 ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HeroControlButton(
                    icon = Icons.Filled.MusicNote,
                    contentDescription = "음악",
                )

                Surface(
                    onClick = onStartRun,
                    modifier = Modifier.size(84.dp),
                    shape = CircleShape,
                    color = LimeGreen,
                    shadowElevation = 12.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "시작",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0A0B10),
                        )
                    }
                }

                HeroControlButton(
                    icon = Icons.Filled.DirectionsRun,
                    contentDescription = "러닝 유형",
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                onClick = {},
                shape = RoundedCornerShape(50),
                color = PillBg,
            ) {
                Text(
                    text = "목표 설정",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun GpsStatusPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PillBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(LimeGreen, CircleShape),
        )
        Text(
            text = "GPS 준비 완료 · 러닝하기 좋은 날",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun HeroTabChip(text: String, selected: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) Color(0xFF0A0B10) else Color.White.copy(alpha = 0.70f),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) LimeGreen else PillBg)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun HeroControlButton(icon: ImageVector, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(BtnBg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}
