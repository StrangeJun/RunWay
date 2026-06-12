package com.runway.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RunwayShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),   // 칩, 배지
    small      = RoundedCornerShape(14.dp),   // 아이콘 컨테이너
    medium     = RoundedCornerShape(20.dp),   // 기본 카드, 입력 필드
    large      = RoundedCornerShape(28.dp),   // 대형 카드
    extraLarge = RoundedCornerShape(32.dp),   // 통계 카드, 맵 컨테이너
)

/** 버튼 full-pill — RunwayPrimaryButton / RunwayLoadingButton 전용 */
val PillShape = RoundedCornerShape(999.dp)
