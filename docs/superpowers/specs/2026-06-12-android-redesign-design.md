# Android 앱 전체 디자인 리디자인 스펙

## 개요

Stitch 신규 디자인 스크린 컨셉을 기반으로 Android 앱 전체 화면을 리디자인한다. 스플래시 화면은 제외한다.

## 디자인 토큰 변경

### 컬러 팔레트

| 토큰 | 현재 | 변경 후 | 비고 |
|------|------|---------|------|
| BackgroundDark | #111119 | #0A0A0A | 순수 블랙 |
| SurfaceDark | #1B1B2B | #141414 | 뉴트럴 다크 |
| MutedDark | #222232 | #1E1E1E | |
| SecondaryContainerDark | #262638 | #242424 | |
| BorderColorDark | #2B2B3D | #2A2A2A | |
| GlassSurface | — | rgba(255,255,255,0.05) | 신규: 글래스 카드 배경 |
| GlassBorder | — | rgba(255,255,255,0.10) | 신규: 글래스 카드 테두리 |

### 악센트 컬러 추가

기존 `AccentColor` enum에 다음 추가:
- `WHITE` ("화이트", #FFFFFF) — `darkContainer: #1E1E1E`, `lightContainer: #F0F0F0`
- `ENERGY_ORANGE` ("오렌지", #FF6B35) — `darkContainer: #3D1A0A`, `lightContainer: #FFD5C2`
- `ELECTRIC_BLUE` ("블루", #4D9EFF) — `darkContainer: #0A1E3D`, `lightContainer: #C2DCFF`

기존 `RED`, `ORANGE(#FFA45B)`, `YELLOW`, `GREEN(기본)`, `BLUE(#65B7FF)`, `PURPLE` 유지.

### 코너 반경 (Shape.kt)

| 이름 | 현재 | 변경 후 | 용도 |
|------|------|---------|------|
| extraSmall | 8dp | 10dp | 칩, 배지 |
| small | 12dp | 14dp | 아이콘 컨테이너 |
| medium | 16dp | 20dp | 기본 카드 |
| large | 20dp | 28dp | 입력 필드, 대형 카드 |
| extraLarge | 24dp | 32dp | 통계 카드, 맵 컨테이너 |
| pill | — | 999dp | 신규: 버튼 full-pill |

## 글래스모피즘 카드 패턴

모든 카드 컴포넌트에 아래 패턴 적용:

```kotlin
// GlassCard modifier
Modifier
  .background(
    color = Color.White.copy(alpha = 0.05f),
    shape = MaterialTheme.shapes.extraLarge
  )
  .border(
    width = 1.dp,
    color = Color.White.copy(alpha = 0.10f),
    shape = MaterialTheme.shapes.extraLarge
  )
```

## 버튼 스타일 변경

- `RunwayPrimaryButton`: `shape = MaterialTheme.shapes.pill` (full-pill)
- `RunwayLoadingButton`: 동일
- 높이 56dp → 58dp

## 변경 파일 목록

### 1. 디자인 토큰 (3개)
- `ui/theme/Color.kt`
- `ui/theme/Shape.kt`
- `ui/theme/Theme.kt`

### 2. 공통 컴포넌트 (12개)
- `ui/components/RunwayPrimaryButton.kt`
- `ui/components/RunwayLoadingButton.kt`
- `ui/components/RunwayTextField.kt`
- `ui/components/RunwayBottomNav.kt`
- `ui/components/RunMetricCard.kt`
- `ui/components/RecentRunCard.kt`
- `ui/components/DiscoverCourseCard.kt`
- `ui/components/RunHeroSection.kt`
- `ui/components/WeeklyStatsCard.kt`
- `ui/components/StatsSummaryCard.kt`
- `ui/components/StartRunCard.kt`
- `ui/components/RunHistoryCard.kt`

### 3. 주요 화면 (25개, 스플래시 제외)
- `ui/auth/login/LoginScreen.kt`
- `ui/auth/signup/SignupScreen.kt`
- `ui/home/HomeScreen.kt`
- `ui/running/RunningTrackingScreen.kt`
- `ui/running/RunResultScreen.kt`
- `ui/running/history/MyRunsScreen.kt`
- `ui/running/history/RunDetailScreen.kt`
- `ui/course/detail/CourseDetailScreen.kt`
- `ui/course/library/CoursesLibraryScreen.kt`
- `ui/course/my/MyCoursesScreen.kt`
- `ui/discover/DiscoverScreen.kt`
- `ui/attempt/CourseAttemptTrackingScreen.kt`
- `ui/leaderboard/CourseLeaderboardScreen.kt`
- `ui/leaderboard/LeaderboardScreen.kt`
- `ui/profile/ProfileScreen.kt`
- `ui/stats/StatsScreen.kt`
- `ui/achievements/AchievementsScreen.kt`
- `ui/posture/PostureHomeScreen.kt`
- `ui/posture/PostureCaptureScreen.kt`
- `ui/posture/PostureResultScreen.kt`
- `ui/settings/SettingsScreen.kt`
- `ui/onboarding/OnboardingScreen.kt`
- `ui/permission/PermissionScreen.kt`
- `ui/navigation/MainScaffold.kt`
- `ui/share/RunShareImageScreen.kt`

## 구현 제외

- `ui/splash/RunwaySplashScreen.kt` — 사용자 요청으로 제외
- `ui/splash/RunwayLogoAnimation.kt` — 제외
- ViewModel 파일들 — 디자인 변경 없음
- Backend 코드 — 변경 없음
