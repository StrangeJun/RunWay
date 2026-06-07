# Android UI/Animation Polish Plan

> 작성일: 2026-05-23  
> 대상 브랜치: `feature/android-course-pr` (이후 별도 feature 브랜치로 분리)  
> 목적: RunWay Android 앱이 기능적 대시보드가 아닌 모던 러닝 앱처럼 느껴지도록 Compose 애니메이션, micro-interaction, 상태별 피드백을 전면 적용한다.

---

## 우선순위 기준

| 등급 | 기준 |
|------|------|
| **P0** | 체감 품질에 즉각 영향. 기능은 있지만 느낌이 없는 곳. |
| **P1** | 중요한 순간의 시각적 무게감. 앱이 "살아있다"는 느낌. |
| **P2** | 경험 차별화. 앱을 "프리미엄"으로 만드는 레이어. |
| **P3** | 감성 레이어. 앱이 살아있다는 느낌의 마지막 10%. |

---

## P0 — 즉각적 체감 개선

### P0-1. OnboardingScreen — 페이지 전환 애니메이션

| 항목 | 내용 |
|------|------|
| **현재 상태** | 정적 아이콘-인-서클 레이아웃. 페이지 전환 없음. 정적 dot indicator. 스와이프 없음. |
| **개선** | `HorizontalPager` + `AnimatedContent` 페이지 슬라이드. dot indicator 크기 애니메이션(`animateDpAsState`). 마지막 페이지 CTA 버튼 위로 슬라이드인. |
| **Animation type** | Compose `HorizontalPager` + `animateFloatAsState` + `animateDpAsState` |
| **복잡도** | Medium |
| **변경 파일** | `ui/onboarding/OnboardingScreen.kt` |
| **Risk** | 낮음 — 페이지 구조만 변경, 비즈니스 로직 없음 |
| **Acceptance criteria** | 스와이프로 페이지 이동. dot 크기 변화로 현재 페이지 시각적 강조. 버튼이 뚝 나타나지 않고 슬라이드로 등장. |

---

### P0-2. RunHeroSection — "시작" 버튼 탭 피드백

| 항목 | 내용 |
|------|------|
| **현재 상태** | LimeGreen 96dp 버튼이 탭 시 즉시 화면 전환. 탭 피드백 없음. |
| **개선** | 탭 시 버튼이 살짝 scale-down(0.93) → spring bounce back → 화면 전환. |
| **Animation type** | Compose `InteractionSource` + `animateFloatAsState(spring)` + `graphicsLayer` |
| **복잡도** | Low |
| **변경 파일** | `ui/components/RunHeroSection.kt` |
| **Risk** | 최저 — `graphicsLayer` scale만 추가 |
| **Acceptance criteria** | 탭 → 버튼 살짝 수축 → 튕기며 복귀 → 화면 전환. 기계적인 느낌이 아닌 물리적 반응. |

---

### P0-3. CourseAttemptTrackingScreen — Progress bar 애니메이션

| 항목 | 내용 |
|------|------|
| **현재 상태** | progress bar가 GPS 업데이트마다 즉시 점프. 뚝뚝 끊김. |
| **개선** | `animateFloatAsState(tween(800, FastOutSlowInEasing))`로 부드러운 전환. 90% 도달 시 LimeGreen으로 color animate. |
| **Animation type** | Compose `animateFloatAsState` + `animateColorAsState` |
| **복잡도** | Low |
| **변경 파일** | `ui/attempt/CourseAttemptTrackingScreen.kt` |
| **Risk** | 최저 |
| **Acceptance criteria** | progress가 뚝뚝 점프하지 않고 흘러감. 90% 구간에서 색상 전환. |

---

### P0-4. CourseLeaderboardScreen — PR 배너 등장 애니메이션

| 항목 | 내용 |
|------|------|
| **현재 상태** | PR/regression 배너가 화면 로드 시 즉시 나타남. 감흥 없음. |
| **개선** | 배너가 아래에서 슬라이드업 + fade-in(`AnimatedVisibility`). PR일 때 배너 배경에 shimmer sweep 1회. |
| **Animation type** | Compose `AnimatedVisibility(slideInVertically + fadeIn)` + Canvas shimmer (PR only) |
| **복잡도** | Low-Medium |
| **변경 파일** | `ui/leaderboard/CourseLeaderboardScreen.kt` |
| **Risk** | 낮음 |
| **Acceptance criteria** | 배너가 아래에서 올라오며 등장. PR 배너는 한 번 빛 흐름. regression 배너는 fade-in만. |

---

## P1 — 중요한 순간의 시각적 무게감

### P1-1. RunHeroSection — 날씨/GPS pill 로딩 shimmer

| 항목 | 내용 |
|------|------|
| **현재 상태** | 위치/날씨 로딩 중 정적 텍스트 또는 빈 공간. |
| **개선** | 데이터 로딩 전 pill 모양 shimmer placeholder. `InfiniteTransition` + gradient sweep. |
| **Animation type** | Compose Canvas shimmer (gradient brush + `translateX` animate) |
| **복잡도** | Medium |
| **변경 파일** | `ui/components/RunHeroSection.kt` |
| **Risk** | 낮음 |
| **Acceptance criteria** | pill이 흰색으로 sweep되다가 실제 데이터로 crossfade 전환. |

---

### P1-2. RunningTrackingScreen — 거리 숫자 slot machine 전환

| 항목 | 내용 |
|------|------|
| **현재 상태** | 84sp 거리 숫자가 GPS 업데이트마다 즉시 변경. |
| **개선** | 숫자 변경 시 `AnimatedContent(slideInVertically toward top + fadeIn)`. 증가하는 숫자는 위로 올라가며 교체되는 slot machine 효과. |
| **Animation type** | Compose `AnimatedContent` + `slideInVertically` |
| **복잡도** | Medium |
| **변경 파일** | `ui/running/tracking/RunningTrackingScreen.kt` |
| **Risk** | 낮음 — 표시 로직만 변경, 데이터 플로우 무관 |
| **Acceptance criteria** | 0.5km → 0.6km 전환 시 숫자가 위로 스크롤되며 교체. 빠른 업데이트에서 버퍼링 없이 부드럽게. |

---

### P1-3. HomeScreen — 런 카드 리스트 staggered 등장

| 항목 | 내용 |
|------|------|
| **현재 상태** | 런 기록 카드들이 한꺼번에 즉시 나타남. |
| **개선** | 아이템별 index 기반 delay. 초기 `offsetY` slide-in. 첫 로드 시만 stagger, 이후 스크롤은 기본. |
| **Animation type** | Compose `LaunchedEffect(index)` + `animateFloatAsState` 초기 Y offset |
| **복잡도** | Medium |
| **변경 파일** | `ui/home/HomeScreen.kt` |
| **Risk** | 낮음 |
| **Acceptance criteria** | 처음 목록 로드 시 카드들이 0ms, 60ms, 120ms... 간격으로 아래에서 올라옴. 새로고침 후 재실행. |

---

### P1-4. CourseDetailScreen — 코스 통계 숫자 count-up

| 항목 | 내용 |
|------|------|
| **현재 상태** | 거리, 완주율 등 숫자가 화면 진입 즉시 최종값으로 표시. |
| **개선** | 화면 진입 후 1.2초간 0→최종값으로 count-up. `LaunchedEffect(targetValue)` + `animateFloatAsState(tween(1200))`. |
| **Animation type** | Compose `animateFloatAsState` |
| **복잡도** | Low |
| **변경 파일** | `ui/course/detail/CourseDetailScreen.kt` |
| **Risk** | 최저 |
| **Acceptance criteria** | 숫자들이 0부터 최종값까지 카운트업. 이미 로드된 상태에서 화면 재진입 시 재실행하지 않음. |

---

### P1-5. RunShareImageScreen — 템플릿 선택 preview 전환

| 항목 | 내용 |
|------|------|
| **현재 상태** | 템플릿 선택 시 즉시 교체. |
| **개선** | `Crossfade(targetState = selectedTemplate)`. 선택된 템플릿 썸네일에 scale-up + 테두리 animate. |
| **Animation type** | Compose `Crossfade` + `animateDpAsState`(border) + `animateFloatAsState`(scale) |
| **복잡도** | Low |
| **변경 파일** | `ui/running/share/RunShareImageScreen.kt` |
| **Risk** | 최저 |
| **Acceptance criteria** | 템플릿 전환 시 crossfade. 선택 썸네일 강조 애니메이션. |

---

## P2 — 경험 차별화

### P2-1. Splash → Onboarding 전환 — 로고 hero 전환

| 항목 | 내용 |
|------|------|
| **현재 상태** | 2초 후 즉시 화면 전환. |
| **개선** | Splash 로고가 shrink + fade로 사라지고 Onboarding에서 같은 위치에 fade-in. `SharedTransitionLayout`(Compose 1.7+) 또는 orchestrated `AnimatedVisibility` 시퀀스로 구현. |
| **Animation type** | Compose `SharedTransitionLayout` 또는 orchestrated `AnimatedVisibility` |
| **복잡도** | High |
| **변경 파일** | `ui/splash/RunwaySplashScreen.kt`, `ui/onboarding/OnboardingScreen.kt`, `ui/navigation/RunwayNavGraph.kt` |
| **Risk** | 중간 — Navigation 전환 타이밍 맞추기 까다로움 |
| **Acceptance criteria** | 앱 시작 시 로고가 연속성 있게 흐름. 뚝 끊기지 않음. |

---

### P2-2. CourseAttemptTrackingScreen — 완주 순간 celebration

| 항목 | 내용 |
|------|------|
| **현재 상태** | 완주 시 즉시 결과 화면으로 전환. 감흥 없음. |
| **개선** | 완주 감지 후 0.8초 딜레이 동안 Canvas 파티클 burst(confetti 20~30개, gravity + scatter). 이후 결과 화면으로 전환. |
| **Animation type** | Canvas custom particles + `LaunchedEffect` orchestration |
| **복잡도** | High |
| **변경 파일** | `ui/attempt/CourseAttemptTrackingScreen.kt`, 신규 `ui/components/ConfettiCanvas.kt` |
| **Risk** | 낮음 — 완주 감지 조건은 이미 구현됨. Canvas particle은 독립 컴포넌트. |
| **Acceptance criteria** | 완주 시 파티클 0.8초 표시 → 화면 전환. 60fps 유지. |

---

### P2-3. ProfileScreen — 통계 count-up + streak flame pulse

| 항목 | 내용 |
|------|------|
| **현재 상태** | 통계 그리드 즉시 표시. 정적 streak 카드. |
| **개선** | 통계 count-up(P1-4 패턴 재사용). Streak 카드의 불꽃 아이콘에 `rememberInfiniteTransition` scale pulse(1.0↔1.15, 800ms). Streak 0일 때 flame 회색 + no animation. |
| **Animation type** | Compose `animateFloatAsState` + `rememberInfiniteTransition` |
| **복잡도** | Low-Medium |
| **변경 파일** | `ui/profile/ProfileScreen.kt` |
| **Risk** | 최저 |
| **Acceptance criteria** | 통계 count-up. 스트릭 있으면 불꽃 pulse. 없으면 정적 회색. |

---

### P2-4. CoursesLibraryScreen — 탭 전환 content 슬라이드

| 항목 | 내용 |
|------|------|
| **현재 상태** | TabRow 탭 전환 시 content 즉시 교체. |
| **개선** | `HorizontalPager`로 content 교체. 탭 이동 방향에 따라 좌/우 슬라이드. |
| **Animation type** | Compose `HorizontalPager` + `TabRow` pagerState 연동 |
| **복잡도** | Medium |
| **변경 파일** | `ui/course/library/CoursesLibraryScreen.kt` |
| **Risk** | 낮음 — 탭 구조 그대로 유지 |
| **Acceptance criteria** | 탭 클릭/스와이프로 content 슬라이드 전환. indicator도 따라 이동. |

---

### P2-5. DiscoverScreen → CourseDetailScreen — 카드 expand 전환

| 항목 | 내용 |
|------|------|
| **현재 상태** | 카드 탭 → 즉시 새 화면. |
| **개선** | 카드 탭 시 `scaleIn(0.92) + fadeIn` enter transition. 뒤로가기 시 `scaleOut + fadeOut` 역방향. |
| **Animation type** | Compose Navigation `EnterTransition` / `ExitTransition` |
| **복잡도** | Medium |
| **변경 파일** | `ui/navigation/RunwayNavGraph.kt`, `ui/discover/DiscoverScreen.kt` |
| **Risk** | 낮음 — Navigation 전환 설정만 변경 |
| **Acceptance criteria** | 카드 탭 → 화면이 카드에서 확장되는 느낌. 뒤로가기 시 역방향. |

---

## P3 — 감성 레이어

### P3-1. RunHeroSection — 배경 지도 subtle parallax

| 항목 | 내용 |
|------|------|
| **현재 상태** | 스크롤 시 지도 배경 고정. (graphicsLayer translationY fade는 구현됨) |
| **개선** | 스크롤 내릴수록 지도 배경이 `translationY = scrollOffset * 0.3f`로 느리게 올라가는 parallax. 현재 구현에 +1줄. |
| **복잡도** | Low |
| **변경 파일** | `ui/components/RunHeroSection.kt` |

---

### P3-2. RunningTrackingScreen — 페이스 변화 색상 pulse

| 항목 | 내용 |
|------|------|
| **현재 상태** | 페이스 텍스트 정적 색상. |
| **개선** | 페이스가 이전보다 빨라지면 LimeGreen pulse 1회. 느려지면 subtle orange pulse. `LaunchedEffect(pace)` + `animateColorAsState`. |
| **복잡도** | Low |
| **변경 파일** | `ui/running/tracking/RunningTrackingScreen.kt` |

---

### P3-3. CourseDetailScreen — Route map draw-on 애니메이션

| 항목 | 내용 |
|------|------|
| **현재 상태** | 루트 오버레이 즉시 표시. |
| **개선** | 화면 진입 시 루트 선이 시작점부터 끝점까지 1.5초간 그려지는 path animation. Canvas `PathMeasure` + `animateFloatAsState`. |
| **복잡도** | High |
| **변경 파일** | `ui/course/detail/CourseDetailScreen.kt` 또는 루트 Canvas 컴포넌트 |
| **Risk** | 중간 — PathMeasure와 Canvas route 오버레이 연동 필요 |

---

### P3-4. Empty/Error 상태 — 아이콘 micro-animation

| 항목 | 내용 |
|------|------|
| **현재 상태** | 빈 상태가 텍스트 + 정적 아이콘. |
| **개선** | 빈 러닝 기록: 달리는 사람 아이콘 bounce. 네트워크 에러: 와이파이 아이콘 pulse. `rememberInfiniteTransition` + `translateY` or `scale`. |
| **복잡도** | Low |
| **변경 파일** | `ui/home/HomeScreen.kt`, `ui/discover/DiscoverScreen.kt`, `ui/course/library/CoursesLibraryScreen.kt` |

---

## 구현 로드맵

```
Week 1 — P0 전체 (4개)
  P0-2 RunHeroSection 버튼 탭 피드백     (30분)
  P0-3 Progress bar 애니메이션           (30분)
  P0-4 PR 배너 슬라이드인               (1시간)
  P0-1 Onboarding 페이지 전환           (2시간)

Week 2 — P1 전체 (5개)
  P1-2 거리 숫자 slot machine            (1시간)
  P1-4 코스 통계 count-up               (30분)
  P1-5 템플릿 선택 crossfade            (30분)
  P1-1 GPS pill shimmer                  (1.5시간)
  P1-3 홈 카드 staggered 등장           (1.5시간)

Week 3 — P2 (5개, 병렬 가능)
  P2-3 Profile count-up + streak        (1시간)
  P2-4 탭 전환 HorizontalPager          (1.5시간)
  P2-5 Discover → Detail 전환           (1시간)
  P2-2 완주 celebration confetti        (3시간)
  P2-1 Splash → Onboarding 로고 전환   (3시간)

Week 4 — P3 (4개)
  P3-1 지도 parallax                    (30분)
  P3-2 페이스 color pulse               (30분)
  P3-4 Empty state micro-animation      (1시간)
  P3-3 Route draw-on animation          (3시간)
```

---

## 첫 번째 구현 권고

**P0-2 RunHeroSection 시작 버튼 탭 피드백**

- 변경 파일 1개, 코드 10줄 미만
- 가장 자주 탭하는 핵심 버튼 — 체감 효과 대비 비용 최소
- 다른 작업과 충돌 없음
- 빌드 리스크 없음

```kotlin
// 구현 패턴 (참고용)
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.93f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "startButtonScale"
)
// Button modifier에 graphicsLayer { scaleX = scale; scaleY = scale } 추가
```
