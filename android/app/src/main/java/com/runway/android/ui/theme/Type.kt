package com.runway.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.runway.android.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val soraFont        = GoogleFont("Sora")
private val spaceGrotesk    = GoogleFont("Space Grotesk")
private val notoSansKr      = GoogleFont("Noto Sans KR")

/** Headlines, buttons, large numbers — Sora */
val DisplayFontFamily = FontFamily(
    Font(googleFont = soraFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = soraFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = soraFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = soraFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = soraFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
)

/** Metric labels, units — Space Grotesk */
val MetricFontFamily = FontFamily(
    Font(googleFont = spaceGrotesk, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = spaceGrotesk, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = spaceGrotesk, fontProvider = fontProvider, weight = FontWeight.Bold),
)

/** Body text, Korean UI — Noto Sans KR */
val BodyFontFamily = FontFamily(
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
)

val RunwayTypography = Typography(
    // ── 러닝 대형 숫자 (0.00 km, 28:14) — Sora ExtraBold ──
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 84.sp,
        letterSpacing = (-3.5).sp,
        lineHeight = 84.sp,
    ),
    // ── 주요 통계 숫자 ──
    displayMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp,
        letterSpacing = (-2.5).sp,
        lineHeight = 64.sp,
    ),
    // ── 화면 타이틀 — Sora Bold ──
    headlineLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 40.sp,
    ),
    // ── 섹션 헤더 / 카드 타이틀 ──
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 36.sp,
    ),
    // ── 서브 헤더 ──
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        lineHeight = 30.sp,
    ),
    // ── 추적 그리드 수치 — Sora ──
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp,
    ),
    // ── 리스트 항목 제목 ──
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.sp,
        lineHeight = 25.sp,
    ),
    // ── 서브 레이블 ──
    titleSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    // ── 본문 — Noto Sans KR ──
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 21.sp,
    ),
    // ── 버튼 텍스트 — Sora SemiBold ──
    labelLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
    ),
    // ── 메트릭 레이블 — Space Grotesk (uppercase 적용은 호출 측에서) ──
    labelMedium = TextStyle(
        fontFamily = MetricFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
        lineHeight = 16.sp,
    ),
    // ── 단위 레이블 (km, /km) ──
    labelSmall = TextStyle(
        fontFamily = MetricFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp,
    ),
)
