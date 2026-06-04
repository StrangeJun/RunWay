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

private val notoSansKr = GoogleFont("Noto Sans KR")
private val jetbrainsMono = GoogleFont("JetBrains Mono")

val DisplayFontFamily = FontFamily(
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = notoSansKr, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
)

val MonoFontFamily = FontFamily(
    Font(googleFont = jetbrainsMono, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = jetbrainsMono, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = jetbrainsMono, fontProvider = fontProvider, weight = FontWeight.Bold),
)

val RunwayTypography = Typography(
    // ── 대형 숫자 ── 러닝 타이머 (28:14), 영웅 거리 (5.42 km)
    displayLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        letterSpacing = (-2).sp,
        lineHeight = 70.sp,
    ),
    // ── 주요 통계 숫자 ── 주간 거리 (24.6 km)
    displayMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-1).sp,
        lineHeight = 54.sp,
    ),
    // ── 화면 타이틀 ── "코스 탐색", "리더보드"
    headlineLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 38.sp,
    ),
    // ── 섹션 헤더 / 카드 타이틀 ── "Riverside Loop"
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 32.sp,
    ),
    // ── 소형 화면 헤더 ── 설정, 내 기록 서브헤더
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp,
    ),
    // ── 추적 그리드 수치 ── "5'12\"", "11.5"
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        lineHeight = 30.sp,
    ),
    // ── 리스트 항목 제목 / 카드 이름
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.sp,
        lineHeight = 25.sp,
    ),
    // ── 서브 레이블 / 작은 제목
    titleSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    // ── 본문 / 설명 ── 기본 읽기 텍스트
    bodyLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 26.sp,
    ),
    // ── 보조 본문 ── 메타데이터, 거리·위치
    bodyMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 23.sp,
    ),
    // ── 캡션 / 설명 서브텍스트
    bodySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 21.sp,
    ),
    // ── 칩 / 배지 / 탭 레이블
    labelLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
    ),
    // ── 소형 캡션 ── "거리", "페이스"
    labelMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
    ),
    // ── 최소 레이블 ── 단위 (km, /km)
    labelSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp,
    ),
)
