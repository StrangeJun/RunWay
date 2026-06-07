# RunWay UX Improvement Roadmap

> 작성일: 2026-05-21
> 기준 브랜치: `fix/codex-review-b23-b26` (B-26 Codex review fix 완료 상태)
> 작성 근거: 사용자 아이디어 1~13번 + 추가 권장사항 A~Q + 실제 코드베이스 직접 확인

---

## 1. Purpose

이 문서는 RunWay의 다음 UX 개선 방향을 체계적으로 정리한다. 사용자가 제시한 13개 아이디어와 추가 권장사항 A~Q를 코드베이스 실제 구현 상태와 대조하여 우선순위를 분류하고, Phase B-27부터 B-36까지의 로드맵을 제안한다.

이 문서의 목적:
- 각 UX 아이디어가 현재 코드에서 어디에 해당하는지 명확히 파악한다.
- 우선순위 기준(P0~P3)에 따라 실행 순서를 정한다.
- 다음 구현 Phase(B-27)의 상세 스코프와 실행 프롬프트를 제공한다.

---

## 2. Current UX Gap Summary

### 코드베이스 직접 확인 결과

| 항목 | 현재 상태 | 파일 위치 |
|---|---|---|
| HomeScreen 최근 런 클릭 | **버그**: `RecentRunCard`에 `onClick` 파라미터 없음. `RecentRun` 데이터 클래스에 `runId` 필드 없음 | `ui/components/RecentRunCard.kt`, `ui/home/HomeViewModel.kt` |
| BottomNav 구성 | HOME / DISCOVER / **LEADERBOARD** / PROFILE. `LeaderboardScreen`은 안내 텍스트만 표시 | `ui/navigation/MainTab.kt`, `ui/leaderboard/LeaderboardScreen.kt` |
| CourseDetailScreen 리더보드 | "순위 보기" 버튼으로 전체 화면 이동만 가능. **Top 5 인라인 미리보기 없음** | `ui/course/detail/CourseDetailScreen.kt` |
| ProfileScreen 닉네임/소개 편집 | 표시만 가능. `ProfileViewModel`에 `updateProfile()` 없음 | `ui/profile/ProfileViewModel.kt` |
| PUT /api/users/me | **이미 구현됨** — `UserApi.kt`에 `updateMe()` 존재. `UserRepository` 인터페이스에는 미노출 | `data/user/remote/UserApi.kt`, `domain/user/UserRepository.kt` |
| RunningTrackingScreen | 지도: `RouteMapPlaceholder`. 거리/페이스/속도 메트릭 그리드. Stop + Pause/Resume 2버튼 구성 | `ui/running/RunningTrackingScreen.kt` |
| 러닝 카운트다운 | **없음** | — |
| 공유 이미지 배경 | `RunShareImageScreen` — 템플릿 선택 가능, 배경 사진 선택 없음 | `ui/share/RunShareImageScreen.kt` |
| 코스 공개 조건 | Backend에 10회 완주 조건 없음. 메타데이터 필드(difficulty, slopeLevel 등) 없음 | `docs/api-specification.md` |
| 즐겨찾기 코스 | **없음** | — |
| 코스 이탈 경고 | **없음** | — |
| 인트로/스플래시 | OnboardingScreen 존재. 로그인 전 브랜드 인트로 없음 | `ui/onboarding/OnboardingScreen.kt` |

---

## 3. Improvements Organized by Product Area

### A. 인트로 / 스플래시 (아이디어 1, 추천사항 A)

**아이디어 1:** 로그인 화면 전 러닝 관련 시각 효과/인트로 → 로그인으로 연결
**추천사항 A:** 첫 실행만 브랜드 인트로 + 핵심 가치 설명. 이후는 짧은 스플래시만

현재 상태: `RunwayNavGraph.kt`에서 `isOnboardingCompleted == false`이면 `OnboardingScreen`으로 이동. 로그인 화면 앞에 별도 인트로 없음.

구현 방향:
- `OnboardingScreen` 앞에 `SplashScreen` (500ms) → 첫 실행 시만 `BrandIntroScreen` (러닝 영상/애니메이션) → 로그인
- DataStore에 `isBrandIntroDone` 별도 저장. 재실행 시 짧은 스플래시만.
- `RunwayNavGraph.kt`에 조건 분기 추가.

---

### B. 홈 화면 — Run Start 중심 재설계 (아이디어 2, 추천사항 B, Q)

**아이디어 2:** Nike Run Club 스타일 — 지도/현재위치 배경 + 운동 시작 버튼, 스크롤 시 기존 HomeScreen으로 자연스럽게 전환
**추천사항 B:** 상단 히어로: 지도+시작 카드, 하단 스크롤: 기존 콘텐츠
**추천사항 Q:** 런 시작 화면 퀵 액션: 자유 런 / 최근 코스 재도전 / 즐겨찾기 코스

현재 상태: `HomeScreen.kt` — 인사말 헤더, 주간 통계, `StartRunCard`, 주변 코스, 최근 런 목록의 `LazyColumn` 구조. 지도 없음.

구현 방향:
- `LazyColumn`의 첫 `item`을 `RunHeroCard`로 교체: Google Maps 미니 뷰(현재 위치 표시) + 큰 Start Run 버튼 + 퀵 액션(자유 런 / 최근 코스 재도전 / 즐겨찾기)
- 스크롤 시 주간 통계 → 주변 코스 → 최근 런 순서 유지
- `HomeViewModel`에 `lastCourse`, `favoriteCourses` 상태 추가

---

### C. 러닝 시작 전 설정 (추천사항 C)

**추천사항 C:** 런 타입/목표/오디오 안내/자동정지/지표 프리셋

현재 상태: "Start Run" 버튼 클릭 시 바로 `RunningTrackingScreen` 진입.

구현 방향:
- `RunSettingsBottomSheet` — 런 타입(자유/코스), 거리 목표, Auto-Pause on/off, 지표 프리셋(Basic / 페이스 중심 / 고도 중심) 설정
- DataStore에 마지막 설정 저장. 기본값으로 빠른 시작 가능.

---

### D. 러닝 세션 UI 재설계 — 활성/일시정지 2가지 상태 (아이디어 3, 4, 추천사항 D, E)

**아이디어 3:** 러닝 시작 전 3초 카운트다운. 러닝 중: 중앙에 거리 크게 표시, 아래 구성 가능한 지표들, 코스 도전 시 지도+실시간 진행률
**아이디어 4:** 러닝 중 일시정지 버튼만. 정지 후: 상단 지도(경로), 하단 러닝 데이터, 정지/재개 버튼
**추천사항 D:** 활성 상태(최소화, 큰 거리, 일시정지 버튼 1개) / 일시정지 상태(지도+요약+컨트롤)
**추천사항 E:** 지표 프리셋으로 시작 (Basic/페이스 중심/고도 중심), 이후 전체 커스텀

현재 상태: `RunningTrackingScreen.kt` — 상단 상태바, 타이머(displayLarge), 메트릭 그리드(DISTANCE/PACE/SPEED 3개), `RouteMapPlaceholder`, 하단 Stop+Pause/Resume 2버튼. 카운트다운 없음.

구현 방향:
- **카운트다운:** `RunningTrackingScreen` 진입 직후 `CountdownOverlay` (3→2→1→GO, 전체 화면) 표시. 카운트다운 완료 후 `viewModel.startTracking()` 호출.
- **활성 상태:** 거리를 `displayLarge`보다 더 크게 중앙 표시. 하단 지표는 설정된 프리셋 적용. 일시정지 버튼만 (Stop 버튼 제거 또는 숨김).
- **일시정지 상태:** 상단 `RouteMapView`(실제 GPS 경로), 하단 요약(거리/시간/페이스), Stop+Resume 버튼.
- Stop 버튼은 일시정지 상태에서만 노출하여 실수 종료 방지.

---

### E. HomeScreen 최근 런 클릭 버그 수정 (아이디어 5) — **P0**

**아이디어 5:** 홈 최근 런 항목 클릭 → RunDetailScreen 이동 안 되는 버그

현재 상태 (코드 직접 확인):
- `RecentRunCard.kt`: `onClick` 파라미터 없음. `Surface`에 `onClick` 없이 단순 레이아웃.
- `HomeViewModel.kt`의 `RecentRun` 데이터 클래스: `runId` 필드 없음 (`day`, `distanceKm`, `pace`, `duration`만 있음).
- `HomeScreen.kt`의 `items(viewModel.recentRuns)` 블록: `RecentRunCard`에 클릭 핸들러 연결 불가능 상태.

수정 방향:
1. `HomeViewModel.kt`의 `RecentRun`에 `runId: String` 필드 추가.
2. `toRecentRun()`에서 `runId = runId` 매핑.
3. `RecentRunCard.kt`에 `onClick: (() -> Unit)? = null` 파라미터 추가. `Surface`에 `onClick` 적용.
4. `HomeScreen.kt`의 `RecentRunCard` 호출 시 `onClick = { onNavigateToRunDetail(run.runId) }` 연결.
5. `HomeScreen`에 `onNavigateToRunDetail: (String) -> Unit = {}` 파라미터 추가.
6. `MainScaffold.kt`에서 `HomeScreen`에 `onNavigateToRunDetail = onNavigateToRunDetail` 전달.

---

### F. 코스 공개 규칙 (아이디어 6, 추천사항 F, G)

**아이디어 6:** 코스 공개 요건: 본인 코스 10회 완주 필요. 공개 시 난이도/경사/위험도/경고/주의사항/설명 필수
**추천사항 F:** 단계별 가시성 — PRIVATE → UNLISTED → PUBLIC → FEATURED
**추천사항 G:** 필수 메타데이터: `difficulty`, `slopeLevel`, `riskLevel`, `surfaceType`, `recommendedTime`, `warnings`, `description`

현재 상태: `PATCH /api/courses/{courseId}/publish` 존재. 완주 횟수 조건 없음. 메타데이터 필드 없음.

구현 방향 (Backend):
- `courses` 테이블에 `difficulty`, `slope_level`, `risk_level`, `surface_type`, `recommended_time_minutes`, `warnings`, `visibility` 컬럼 추가 (Flyway migration).
- `publish` API에 완주 횟수 확인 로직 추가 (본인 코스 완주 횟수 조회).
- `visibility` ENUM: `private` / `unlisted` / `public` / `featured`.

구현 방향 (Android):
- `CourseDetailScreen`에 메타데이터 표시.
- `CreateCourseDialog` 확장 또는 별도 `PublishCourseScreen` 생성.

---

### G. 공유 이미지 배경 커스터마이징 (아이디어 7, 8, 추천사항 H)

**아이디어 7:** 공유 이미지에 갤러리 사진 또는 경로 지도를 배경으로 사용
**아이디어 8:** 공유 이미지에서 기록 데이터를 Instagram Stories처럼 자유 배치. 지표는 그룹으로 이동
**추천사항 H:** 단계별 — 템플릿 → 배경 선택 → 지표 위치 → 전체 드래그앤드롭

현재 상태: `RunShareImageScreen.kt` — `ShareTemplate` 목록에서 템플릿 선택. Canvas 기반 이미지 생성. 배경 사진 선택 없음. 드래그앤드롭 없음.

구현 방향 (단계별):
- **Stage 2 (B-34):** 배경 선택 — 갤러리 이미지 pick (`ActivityResultContracts.GetContent`) 또는 경로 지도 캡처 중 선택.
- **Stage 3 (나중에):** 지표 위치 조정 — 그룹(거리/페이스/시간) 단위 배치 옵션.
- **Stage 4 (장기):** 전체 드래그앤드롭 — `PointerInput`으로 개별 지표 자유 배치.

---

### H. 하단 내비게이션 재설계 (아이디어 9, 추천사항 I)

**아이디어 9:** 랭킹 탭 제거. 내가 만든/즐겨찾는/참여한 코스, 완주 횟수, 코스 관리 화면으로 교체
**추천사항 I:** Run / Discover / Courses / Profile 또는 Run / Explore / Library / Profile

현재 상태: `MainTab.kt` — HOME / DISCOVER / LEADERBOARD / PROFILE. `LeaderboardScreen`은 안내 텍스트만 표시.

구현 방향:
- `LEADERBOARD` 탭을 `COURSES` 탭으로 교체.
- `MainTab.kt` 수정: `COURSES(Icons.Filled.Route, "코스")`
- `MainScaffold.kt`에서 `LeaderboardScreen` → `CoursesLibraryScreen`으로 교체.
- `CoursesLibraryScreen`: 내가 만든 코스 / 즐겨찾는 코스 / 참여한 코스 탭 구조.

---

### I. 리더보드 필터 강화 (아이디어 10, 11, 추천사항 K)

**아이디어 10:** 리더보드에 랭킹 필터: 최다 완주, 최고 기록, 기타
**아이디어 11:** 리더보드에서 내가 참여한 코스들의 랭킹 확인 가능
**추천사항 K:** 최고 기록 / 최다 완주 → 이후 최근 기록 / 내 주변 / 친구

현재 상태: `CourseLeaderboardScreen.kt` — 실제 API 연결됨. `GET /api/courses/{courseId}/leaderboard?limit=50`. 필터 없음. 정렬은 `best_time_seconds ASC`만.

구현 방향:
- `CourseLeaderboardScreen`에 필터 칩 UI 추가: "최고 기록" / "최다 완주"
- Backend: `GET /api/courses/{courseId}/leaderboard?sort=best_time|completion_count`로 정렬 파라미터 추가.
- `GET /api/courses/{courseId}/leaderboard/me` — 내 순위 별도 조회 API (내 순위 하이라이트).
- `CoursesLibraryScreen`에서 참여한 코스별 내 순위 표시.

---

### J. CourseDetailScreen 인라인 리더보드 미리보기 (아이디어 12, 추천사항 J)

**아이디어 12:** CourseDetailScreen에서 스크롤하면 리더보드 바로 보임
**추천사항 J:** 인라인 리더보드 미리보기 (Top 5)

현재 상태: `CourseDetailScreen.kt`의 `CourseDetailContent()` 함수 — 리더보드 섹션에 헤더와 "순위 보기" 버튼만 있음. 실제 데이터 미표시.

구현 방향:
- `CourseDetailViewModel`에서 `CourseAttemptRepository.getLeaderboard(courseId, limit=5)` 호출.
- `CourseDetailContent()` 리더보드 섹션에 `Column { leaderboardItems.take(5).forEach { LeaderboardPreviewRow(it) } }` 표시.
- "전체 순위 보기 →" 버튼으로 `CourseLeaderboardScreen` 이동.

---

### K. 프로필 편집 (아이디어 13, 추천사항 L)

**아이디어 13:** ProfileScreen에서 프로필 이미지, 닉네임, 소개 편집 가능
**추천사항 L:** 단계별 — 닉네임/소개 → 로컬 이미지 → 백엔드 업로드

현재 상태 (코드 직접 확인):
- `ProfileViewModel.kt`: `nickname`, `email`, `stats`, `personalRecords` 상태만 있음. `updateProfile()` 없음.
- `UserRepository` 인터페이스: `getMe()`와 `getAchievements()`만 있음. `updateMe()` 미노출.
- `UserApi.kt`: `@PUT("api/users/me") suspend fun updateMe(@Body request: UpdateProfileRequest)` 이미 구현됨.
- `UpdateProfileRequest`: `nickname: String`, `profileImageUrl: String?`, `bio: String?`
- `UserProfileResponse`: `bio: String?` 필드 존재하나 `ProfileScreen`에서 미표시.

구현 방향 (Stage 1 — B-27 포함):
1. `UserRepository` 인터페이스에 `suspend fun updateMe(request: UpdateProfileRequest): NetworkResult<UserProfileResponse>` 추가.
2. `UserRepositoryImpl`에서 `UserApi.updateMe()` 위임.
3. `ProfileViewModel`에 `bio`, `isEditing`, `editNickname`, `editBio` 상태 추가.
4. `ProfileViewModel`에 `startEditing()`, `saveProfile()`, `cancelEditing()` 메서드 추가.
5. `ProfileScreen`에 닉네임/소개 인라인 편집 또는 `EditProfileBottomSheet`.
6. 프로필 이미지: 초기에는 Avatar 이니셜 유지 (업로드는 B-33에서 처리).

---

### L. 권한 안내 화면 (추천사항 M)

**추천사항 M:** 명확한 권한 설명 화면

현재 상태: `RunningTrackingScreen.kt`에서 직접 권한 요청. `LocationPermissionCard`가 있으나 권한 거부 시 설명만. 온보딩에 권한 설명 없음.

구현 방향:
- `OnboardingScreen` 마지막 단계에 위치/알림 권한 요청 화면 추가.
- 각 권한의 사용 목적 명시 ("GPS는 러닝 경로 기록에 사용됩니다").

---

### M. 트래킹 복구 UX 개선 (추천사항 N)

**추천사항 N:** 이전 거리/시간 표시, 계속/완료/삭제

현재 상태: `TrackingRecoveryDialog.kt` 존재. `TrackingSessionStore`에서 복구. B-15에서 구현됨.

구현 방향:
- `TrackingRecoveryDialog`에 이전 세션의 `distanceMeters`와 `elapsedSeconds` 표시.
- 버튼 3가지: "계속하기" / "지금 완료하기" / "삭제".

---

### N. 코스 도전 중 이탈 경고 (추천사항 O)

**추천사항 O:** 코스 도전 중 이탈 경고

현재 상태: `CourseAttemptTrackingScreen.kt`에서 포기/완주만. GPS 경로 이탈 감지 없음.

구현 방향:
- 코스 경로에서 300m 이상 이탈 시 `AlertDialog` 표시: "코스 경로에서 벗어났습니다. 계속하시겠습니까?"
- Backend의 Privacy Zone masking과 충돌하지 않도록 마스킹된 좌표가 아닌 실제 코스 포인트로 비교.

---

### O. 즐겨찾기 코스 (추천사항 P)

**추천사항 P:** 즐겨찾기 코스: CourseDetail에서 토글, Courses 탭에서 목록

현재 상태: 즐겨찾기 기능 없음. `CourseDetailScreen`에 즐겨찾기 버튼 없음. Backend에 즐겨찾기 API 없음.

구현 방향:
- Backend: `POST /api/courses/{courseId}/favorite`, `DELETE /api/courses/{courseId}/favorite`, `GET /api/courses/favorites`
- Android: `CourseDetailScreen` 상단 TopAppBar에 즐겨찾기 하트 아이콘 토글.
- `CoursesLibraryScreen`의 "즐겨찾는 코스" 탭에서 목록 표시.

---

## 4. Priority Classification

| # | 개선사항 | 분류 | 우선순위 | Backend 필요 | 복잡도 | 권장 Phase |
|---|---|---|---|---|---|---|
| 5 | HomeScreen 최근 런 클릭 버그 | 버그 | **P0** | 없음 | Low | **B-27** |
| J | CourseDetailScreen 인라인 리더보드 Top 5 | UX 폴리시 | **P0/P1** | 없음 (API 있음) | Low | **B-27** |
| K | 프로필 닉네임/소개 편집 (Stage 1) | UX 폴리시 | **P1** | 없음 (API 있음) | Low | **B-27** |
| H | BottomNav 랭킹 탭 → Courses 탭 교체 | 구조 변경 | **P1** | 없음 | Low | **B-27** |
| D+E | 러닝 세션 UI 재설계 (카운트다운 + 2상태) | 핵심 UX | **P1** | 없음 | Medium | B-29 |
| B | Nike 스타일 Run Home | 홈 UX | **P1** | 없음 | Medium | B-28 |
| I | 리더보드 필터 (최고 기록 / 최다 완주) | 기능 강화 | **P1** | 낮음 | Low | B-32 |
| O | 즐겨찾기 코스 | 기능 강화 | **P1/P2** | 중간 | Medium | B-30 |
| F | 코스 공개 규칙 (메타데이터 + 완주 조건) | 제품 정책 | **P1/P2** | 높음 | High | B-31 |
| G | 공유 이미지 배경 선택 (Stage 2) | 차별화 | **P2** | 없음 | Medium | B-34 |
| A | 로그인 시각 인트로 + 스플래시 | 온보딩 | **P2** | 없음 | Low | B-35 |
| N | 코스 이탈 경고 | 안전 UX | **P2** | 없음 | Medium | B-36 |
| C | 러닝 시작 전 설정 | 고급 UX | **P2** | 없음 | Medium | B-29 |
| L | 권한 안내 화면 강화 | 온보딩 | **P2** | 없음 | Low | B-35 |
| M | 트래킹 복구 UX 개선 | 폴리시 | **P2** | 없음 | Low | B-27 (보너스) |
| 8 | 공유 이미지 드래그앤드롭 배치 | 차별화 | **P3** | 없음 | High | 장기 |

---

## 5. Proposed Phase Roadmap (B-27~B-36)

### B-27: Quick UX Fixes and Navigation Polish

**목표:** 데모/테스트 전 P0 버그 수정 + 즉시 가능한 P1 UX 개선
**브랜치:** `feature/ux-quick-fixes`
**Backend 변경:** 없음

| 작업 | 파일 | 상세 |
|---|---|---|
| 최근 런 클릭 버그 | `RecentRunCard.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `MainScaffold.kt` | `RecentRun`에 `runId` 추가, `RecentRunCard`에 `onClick` 파라미터 추가, `HomeScreen`에서 `onNavigateToRunDetail` 연결 |
| CourseDetail 인라인 리더보드 | `CourseDetailViewModel.kt`, `CourseDetailScreen.kt` | Top 5 미리보기 + "전체 보기" 버튼 |
| 프로필 닉네임/소개 편집 | `UserRepository.kt`, `UserRepositoryImpl.kt`, `ProfileViewModel.kt`, `ProfileScreen.kt` | `updateMe()` 노출 + 인라인 편집 UI |
| BottomNav LEADERBOARD → COURSES | `MainTab.kt`, `MainScaffold.kt` | `CoursesLibraryScreen` 기초 추가 |

**Acceptance Criteria:**
- [ ] HomeScreen에서 최근 런 카드 클릭 시 `RunDetailScreen`으로 이동한다.
- [ ] `CourseDetailScreen` 스크롤 시 Top 5 리더보드 미리보기가 표시된다.
- [ ] `CourseDetailScreen`의 "전체 순위 보기" 버튼 클릭 시 `CourseLeaderboardScreen`으로 이동한다.
- [ ] `ProfileScreen`에서 닉네임과 소개를 수정하고 저장할 수 있다.
- [ ] 저장 성공 시 변경된 닉네임/소개가 화면에 반영된다.
- [ ] BottomNav의 세 번째 탭이 "코스"로 표시된다.
- [ ] `./gradlew assembleDebug` 빌드 성공.

---

### B-28: Nike-style Run Home

**목표:** 홈 화면을 Run Start 중심으로 재설계
**브랜치:** `feature/run-home-redesign`
**Backend 변경:** 없음

주요 작업:
- `HomeScreen.kt`: 상단 `RunHeroSection` — Google Maps 미니 뷰 + 큰 "Run 시작" 버튼
- 퀵 액션: 자유 런 / 최근 코스 재도전 / 즐겨찾기 코스 (즐겨찾기는 B-30에서 연결)
- 스크롤 시 주간 통계 → 주변 코스 → 최근 런 순서 유지

---

### B-29: Running Session UX Redesign

**목표:** 러닝 세션 UX를 2가지 상태(활성/일시정지)로 재설계 + 카운트다운
**브랜치:** `feature/running-session-ux`
**Backend 변경:** 없음

주요 작업:
- `RunningTrackingScreen.kt`: `CountdownOverlay` 컴포넌트 (3→2→1→GO)
- 활성 상태: 거리 최대화, 일시정지 버튼만
- 일시정지 상태: `RouteMapView` + 요약 + Stop/Resume
- 지표 프리셋 설정 (Basic / 페이스 중심 / 고도 중심)

---

### B-30: Courses Tab / Library

**목표:** BottomNav Courses 탭 완성 + 즐겨찾기 코스 기능
**브랜치:** `feature/courses-library`
**Backend 변경:** 즐겨찾기 API 3개 추가

주요 작업:
- `CoursesLibraryScreen.kt`: 내가 만든 코스 / 즐겨찾는 코스 / 참여한 코스 탭
- `CourseDetailScreen.kt`: 즐겨찾기 하트 아이콘 토글
- Backend: `POST/DELETE /api/courses/{courseId}/favorite`, `GET /api/courses/favorites`

---

### B-31: Course Publishing Policy

**목표:** 코스 공개 규칙 — 완주 조건 + 필수 메타데이터 + 단계별 가시성
**브랜치:** `feature/course-publishing-policy`
**Backend 변경:** 높음 (스키마 변경 + publish 로직)

주요 작업:
- Flyway migration: `courses` 테이블에 `difficulty`, `slope_level`, `risk_level`, `surface_type`, `recommended_time_minutes`, `warnings`, `visibility` 추가
- `PATCH /api/courses/{courseId}/publish` 수정: 본인 코스 완주 횟수 ≥ 10 조건 확인
- Android: `PublishCourseScreen` 또는 `PublishCourseBottomSheet`

---

### B-32: Leaderboard Enhancement

**목표:** 리더보드 필터(최고 기록 / 최다 완주) + 내 순위 하이라이트
**브랜치:** `feature/leaderboard-enhancement`
**Backend 변경:** 낮음 (정렬 파라미터 추가)

주요 작업:
- `GET /api/courses/{courseId}/leaderboard?sort=best_time|completion_count`
- `CourseLeaderboardScreen.kt`: 필터 칩 UI
- 내 순위 하이라이트 행 표시

---

### B-33: Profile Editing — Full (이미지 포함)

**목표:** 프로필 이미지 로컬 선택 + 향후 업로드 기반 구축
**브랜치:** `feature/profile-editing`
**Backend 변경:** 중간 (이미지 업로드 S3 또는 presigned URL)

주요 작업:
- `ActivityResultContracts.GetContent`로 갤러리 이미지 선택
- 로컬 이미지 → `profileImageUrl`로 업로드 (또는 Base64 임시)
- `ProfileScreen`에 아바타 이미지 표시

---

### B-34: Share Image Customization Stage 2

**목표:** 공유 이미지 배경 선택 (갤러리 / 경로 지도 캡처)
**브랜치:** `feature/share-image-stage2`
**Backend 변경:** 없음

주요 작업:
- `RunShareImageScreen.kt`에 배경 선택 섹션 추가
- `ActivityResultContracts.GetContent`로 갤러리 이미지 pick
- `RouteMapView`를 Bitmap으로 캡처하여 배경으로 사용
- Canvas 합성 로직 수정

---

### B-35: Login Visual Intro and Permission UX Polish

**목표:** 브랜드 인트로 + 권한 설명 화면 강화
**브랜치:** `feature/intro-and-permissions`
**Backend 변경:** 없음

주요 작업:
- `SplashScreen.kt` (500ms 애니메이션)
- `BrandIntroScreen.kt` (첫 실행만) — RunWay 핵심 가치 3슬라이드
- DataStore에 `isBrandIntroDone` 저장
- `OnboardingScreen`에 권한 설명 단계 추가

---

### B-36: Course Attempt Deviation Warning

**목표:** 코스 도전 중 경로 이탈 경고 UX
**브랜치:** `feature/course-deviation-warning`
**Backend 변경:** 없음

주요 작업:
- `CourseAttemptTrackingViewModel.kt`에 현재 위치 vs 코스 경로 거리 계산
- 300m 이탈 시 `AlertDialog` 표시
- "무시하고 계속" / "경로로 돌아가기" / "포기" 선택

---

## 6. Technical Design Notes

### 6-1. RecentRun → RunId 추적 패턴

`HomeViewModel.toRecentRun()`은 `RunSummaryResponse`의 `runId` 필드를 드롭하고 있다. 수정 시 `RecentRun` 데이터 클래스에 `runId: String`을 추가하고, `toRecentRun()`에서 `runId = runId`로 매핑한다. `HomeScreen`은 `onNavigateToRunDetail: (String) -> Unit`을 파라미터로 받아야 하며, `MainScaffold → HomeScreen` 체인도 함께 업데이트해야 한다.

### 6-2. UserRepository 인터페이스 확장 패턴

`PUT /api/users/me`는 `UserApi.kt`에 이미 구현돼 있다. `UserRepository` 인터페이스와 `UserRepositoryImpl`에만 추가하면 된다. `UpdateProfileRequest(nickname, profileImageUrl?, bio?)`를 사용한다. 닉네임 중복(`DUPLICATED_NICKNAME` 409)과 validation 오류를 UI에서 처리해야 한다.

### 6-3. CourseDetailViewModel 리더보드 프리로드

`CourseDetailViewModel`은 이미 `CourseRepository`를 주입받는다. `CourseAttemptRepository.getLeaderboard(courseId, limit=5)`를 `init` 블록에서 `coursePoints` 로드와 병렬로 실행한다. `previewLeaderboard: List<LeaderboardItem>` 상태를 추가한다.

### 6-4. BottomNav 교체 시 주의사항

`MainTab.LEADERBOARD`를 `MainTab.COURSES`로 교체할 때, `MainScaffold.kt`에서 `LeaderboardScreen()` → `CoursesLibraryScreen()` 교체가 필요하다. `LeaderboardScreen`은 현재 안내 텍스트만 있으므로 제거해도 기능 손실 없음. 단, `CourseLeaderboardScreen`(코스별 리더보드)은 `RunwayNavGraph.kt`에서 독립 라우트로 유지된다.

### 6-5. 러닝 카운트다운 구현 전략

`RunningTrackingScreen.kt` 진입 직후 `LaunchedEffect(Unit)`에서 카운트다운 로직 실행. 3→2→1→GO는 `delay(1000L)` 루프. 카운트다운 중 `viewModel.startTracking()` 호출 억제. `CountdownOverlay`는 `Box(Modifier.fillMaxSize())`로 전체 화면 오버레이. 카운트다운 완료 후 자동으로 사라짐.

### 6-6. 코스 공개 완주 조건 구현

Backend에서 `PATCH /api/courses/{courseId}/publish` 처리 시 `course_attempts` 테이블에서 `owner_id = currentUserId AND course_id = courseId AND status = 'completed'` 조건으로 완주 횟수를 조회한다. 미충족 시 `COURSE_PUBLISH_NOT_ENOUGH_COMPLETIONS` 에러 코드 반환.

### 6-7. 즐겨찾기 코스 Backend 설계

신규 테이블 `course_favorites(user_id, course_id, created_at)` 추가. PK는 `(user_id, course_id)` 복합키. 즐겨찾기 목록은 `GET /api/courses/favorites`로 조회. `CourseDetailResponse`에 `isFavorited: Boolean` 필드 추가.

### 6-8. 공유 이미지 배경 선택 — 갤러리 vs 지도 캡처

갤러리 이미지: `ActivityResultContracts.GetContent("image/*")`로 URI 획득 → `ImageDecoder.createSource()` / `BitmapFactory`로 Bitmap 변환. 지도 캡처: `GoogleMap.snapshot()` 또는 Canvas RouteMap 컴포저블을 `drawToBitmap()`으로 캡처. 두 Bitmap을 기존 Canvas 합성 로직의 `background` 레이어로 교체.

---

## 7. Quick Wins

현재 브랜치(`fix/codex-review-b23-b26`)에서 바로 가능한 최소 변경 사항:

1. **RecentRunCard onClick (1~2시간):** `RecentRun`에 `runId` 추가 + `RecentRunCard`에 `onClick` 파라미터 + `HomeScreen` 연결. P0 버그 수정.
2. **CourseDetail 인라인 리더보드 (2~3시간):** `CourseDetailViewModel`에 Top 5 로드 추가 + `CourseDetailScreen`에 `LeaderboardPreviewRow` 컴포넌트 추가.
3. **BottomNav 레이블/아이콘 변경 (30분):** `MainTab.kt`에서 `LEADERBOARD` → `COURSES` 변경만. `CoursesLibraryScreen`은 빈 화면으로 시작해도 됨.
4. **ProfileScreen bio 표시 (30분):** `ProfileViewModel`의 `loadProfile()`에서 `bio = profileResult.data.bio ?: ""` 추가 + `ProfileScreen`에 소개 텍스트 표시만. 편집은 별도.

---

## 8. Decisions Needed from User

다음 항목은 설계 방향에 사용자 판단이 필요하다.

### 결정 1: BottomNav 구성

**옵션 A:** Run / Discover / **Courses** / Profile
**옵션 B:** Run / **Explore** / **Library** / Profile
**추천:** 옵션 A — 기존 `HOME`(Run 중심)과 `DISCOVER`(탐색)는 유지하고 `LEADERBOARD`만 `COURSES`로 교체. 변경 범위 최소화.

### 결정 2: 코스 공개 완주 조건 수치

10회 완주 조건이 진입 장벽이 될 수 있다. 대안:
- **5회 완주** — 더 낮은 진입 장벽
- **3회 완주** — 최소 검증
- **즉시 공개 가능, 단 메타데이터 필수** — 완주 조건 제거

### 결정 3: Global Leaderboard 탭 처리

현재 `LeaderboardScreen`은 "탐색 탭에서 코스를 찾아보세요" 안내만 있다.
- **옵션 A:** `COURSES` 탭으로 교체 (추천)
- **옵션 B:** 전체 참여 코스 랭킹 화면으로 발전
- **옵션 C:** 탭 자체 제거 (3탭: Run / Discover / Profile)

### 결정 4: 프로필 이미지 업로드 방식

- **옵션 A:** S3 presigned URL (Backend 변경 필요)
- **옵션 B:** 이미지 없이 닉네임/소개만 편집 (B-27 스코프 제한)
- **추천:** B-27에서는 닉네임/소개만. 이미지 업로드는 B-33에서 처리.

### 결정 5: 카운트다운 전 Run 설정 화면 여부

- **옵션 A:** Start → 바로 카운트다운 → 러닝 (단순)
- **옵션 B:** Start → 설정 바텀시트 → 카운트다운 → 러닝 (추천사항 C)
- **추천:** B-29에서는 옵션 A로 시작. 설정은 B-29 이후 별도 추가.

---

## 9. Recommended Next Phase (B-27 상세)

### B-27: Quick UX Fixes and Navigation Polish

**권장 이유:**
- 최근 런 클릭 버그(P0)를 수정하여 HomeScreen 기본 네비게이션 완성
- CourseDetailScreen 인라인 리더보드로 코스 발견 → 도전 흐름 강화
- 프로필 편집 Stage 1로 사용자 자기 표현 기본 지원
- 모두 Backend 변경 없이 Android 단독 구현 가능
- 모두 기존 API(`GET /api/courses/{courseId}/leaderboard`, `PUT /api/users/me`)가 이미 구현돼 있음

**변경될 파일 목록:**

Android:
- `ui/components/RecentRunCard.kt` — `onClick` 파라미터 추가
- `ui/home/HomeViewModel.kt` — `RecentRun`에 `runId` 필드 추가, `toRecentRun()` 수정
- `ui/home/HomeScreen.kt` — `onNavigateToRunDetail` 파라미터 추가, `RecentRunCard`에 onClick 연결
- `ui/navigation/MainScaffold.kt` — `HomeScreen`에 `onNavigateToRunDetail` 전달
- `ui/course/detail/CourseDetailViewModel.kt` — `previewLeaderboard` 상태 추가, Top 5 로드
- `ui/course/detail/CourseDetailScreen.kt` — `LeaderboardPreviewRow` 컴포넌트 추가
- `domain/user/UserRepository.kt` — `updateMe()` 메서드 추가
- `data/user/UserRepositoryImpl.kt` — `updateMe()` 구현 추가
- `ui/profile/ProfileViewModel.kt` — `bio`, `editNickname`, `editBio`, `isEditing` 상태 + 편집 메서드
- `ui/profile/ProfileScreen.kt` — 닉네임/소개 편집 UI + bio 표시
- `ui/navigation/MainTab.kt` — `LEADERBOARD` → `COURSES` 교체
- `ui/course/library/CoursesLibraryScreen.kt` — 신규 생성 (기본 구조만)

**Acceptance Criteria:**
- [ ] HomeScreen에서 최근 런 카드 클릭 시 `RunDetailScreen`으로 이동한다.
- [ ] `CourseDetailScreen` 스크롤 시 Top 5 리더보드 미리보기가 표시된다.
- [ ] `CourseDetailScreen`의 "전체 순위 보기" 버튼 클릭 시 `CourseLeaderboardScreen`으로 이동한다.
- [ ] `ProfileScreen`에서 닉네임과 소개를 수정하고 저장할 수 있다.
- [ ] 저장 성공 시 변경된 닉네임/소개가 화면에 반영된다.
- [ ] BottomNav의 세 번째 탭이 "코스"로 표시된다.
- [ ] `./gradlew assembleDebug` 빌드 성공.

---

## 10. Implementation Prompt for B-27

다음 프롬프트를 그대로 Claude Code에 입력하면 B-27 구현을 시작할 수 있다.

---

```
Implement Android Phase B-27: Quick UX Fixes and Navigation Polish.

Current branch: fix/codex-review-b23-b26
New branch to create: feature/ux-quick-fixes

Before implementing, confirm that the current branch is fix/codex-review-b23-b26.
Then create and switch to feature/ux-quick-fixes.

Source documents to read before starting:
- docs/ux-improvement-roadmap.md (this document — Section 9 for B-27 details)
- docs/api-specification.md (Section 6-2 for PUT /api/users/me, Section 9-1 for leaderboard)

## Context

B-27 targets four changes that are all Android-only (no backend changes needed), all using
APIs that are already implemented on the backend.

Bug confirmed by code inspection:
- RecentRunCard.kt has no onClick parameter → tapping recent runs in HomeScreen does nothing.
- HomeViewModel.RecentRun data class has no runId field → cannot navigate to RunDetailScreen.
- UserRepository interface does not expose updateMe() → ProfileViewModel cannot call it.
- UserApi.updateMe() IS already implemented with PUT /api/users/me.
- CourseDetailScreen shows "순위 보기" button but no inline leaderboard data.

## Scope

### Task 1: Fix RecentRunCard onClick Bug

Files to change:
- ui/components/RecentRunCard.kt
  Add: onClick: (() -> Unit)? = null parameter
  Change: the outer Surface to use onClick when non-null
  Keep: existing layout unchanged

- ui/home/HomeViewModel.kt
  Change: RecentRun data class — add runId: String field
  Change: toRecentRun() extension — add runId = runId mapping from RunSummaryResponse

- ui/home/HomeScreen.kt
  Add: onNavigateToRunDetail: (String) -> Unit = {} parameter
  Change: RecentRunCard call — add onClick = { onNavigateToRunDetail(run.runId) }

- ui/navigation/MainScaffold.kt
  Change: HomeScreen(...) call — add onNavigateToRunDetail = onNavigateToRunDetail
  The onNavigateToRunDetail is already in MainScaffold's parameters from RunwayNavGraph.

Verify: RunwayNavGraph.kt already passes onNavigateToRunDetail to MainScaffold. Check lines
around MainScaffold(...) composable call. If missing, add it.

### Task 2: CourseDetailScreen Inline Leaderboard Preview (Top 5)

Files to change:
- ui/course/detail/CourseDetailViewModel.kt
  Add: previewLeaderboard: List<LeaderboardItem> state (mutableStateOf, initially empty)
  Add: isLoadingLeaderboard: Boolean state
  In init or loadDetail(): launch a parallel coroutine to call
    courseAttemptRepository.getLeaderboard(courseId, limit = 5)
  On success: previewLeaderboard = result.data.items.take(5)
  CourseDetailViewModel already injects CourseRepository. Add CourseAttemptRepository injection
  following the same @Inject constructor pattern.

- ui/course/detail/CourseDetailScreen.kt
  In CourseDetailContent(), find the leaderboard section (around the Row with EmojiEvents icon).
  After the Row header, add a new Column showing:
    if (viewModel.previewLeaderboard.isEmpty() && !viewModel.isLoadingLeaderboard) →
      Text("아직 완주 기록이 없습니다", bodySmall, onSurfaceVariant)
    else → forEach previewLeaderboard item: LeaderboardPreviewRow(item)
  Add private composable LeaderboardPreviewRow(item: LeaderboardItem) showing:
    rank number + nickname + bestTimeSeconds formatted as mm:ss or h:mm:ss
  Keep the existing "순위 보기" button below.

### Task 3: Profile Nickname/Bio Editing

Files to change:
- domain/user/UserRepository.kt
  Add: suspend fun updateMe(request: UpdateProfileRequest): NetworkResult<UserProfileResponse>
  Import: UpdateProfileRequest from data.user.model

- data/user/UserRepositoryImpl.kt
  Add: override suspend fun updateMe(request: UpdateProfileRequest): NetworkResult<UserProfileResponse>
    = safeApiCall { userApi.updateMe(request) }

- ui/profile/ProfileViewModel.kt
  Add state: bio by mutableStateOf("") private set
  Add state: editNickname by mutableStateOf("") private set
  Add state: editBio by mutableStateOf("") private set
  Add state: isEditing by mutableStateOf(false) private set
  Add state: isSaving by mutableStateOf(false) private set
  Add state: saveError by mutableStateOf<String?>(null) private set
  In loadProfile() success block: bio = profileResult.data.bio ?: ""
  Add functions:
    fun startEditing() { editNickname = nickname; editBio = bio; isEditing = true }
    fun cancelEditing() { isEditing = false; saveError = null }
    fun saveProfile() {
      viewModelScope.launch {
        isSaving = true; saveError = null
        val result = userRepository.updateMe(
          UpdateProfileRequest(editNickname.trim(), null, editBio.trim().ifEmpty { null })
        )
        if (result is NetworkResult.Success) {
          nickname = result.data.nickname
          bio = result.data.bio ?: ""
          isEditing = false
        } else {
          saveError = "저장에 실패했습니다."
        }
        isSaving = false
      }
    }

- ui/profile/ProfileScreen.kt
  In the profile header section (below email Text):
    If !viewModel.isEditing:
      show bio text if non-empty
      show "편집" TextButton that calls viewModel.startEditing()
    If viewModel.isEditing:
      OutlinedTextField for nickname (value = viewModel.editNickname)
      OutlinedTextField for bio (value = viewModel.editBio)
      Row with "저장" Button (calls viewModel.saveProfile()) and "취소" TextButton
      show saveError Text if non-null
  Keep existing Stats, PersonalRecordsSection, and menu items unchanged.

### Task 4: Replace LEADERBOARD Tab with COURSES Tab

Files to change:
- ui/navigation/MainTab.kt
  Change: LEADERBOARD(...) to COURSES(Icons.Filled.Route, "코스")
  Import: Icons.Filled.Route (or Icons.AutoMirrored.Filled.Route if needed)

- ui/navigation/MainScaffold.kt
  Change: MainTab.LEADERBOARD -> LeaderboardScreen()
    to: MainTab.COURSES -> CoursesLibraryScreen()

- Create: ui/course/library/CoursesLibraryScreen.kt
  A minimal placeholder screen showing:
    - TopAppBar with title "내 코스"
    - Three tab headers: "만든 코스" / "즐겨찾기" / "참여한 코스"
    - Each tab body: Box(contentAlignment = Center) { Text("준비 중입니다.") }
  Use TabRow with remember { mutableIntStateOf(0) } for selectedTabIndex.

Note: LeaderboardScreen.kt and old LEADERBOARD references can remain — just not routed to.

## Out of Scope for B-27

- Profile image upload (B-33)
- Course favorites backend API (B-30)
- CoursesLibraryScreen real data (B-30)
- Running countdown (B-29)
- Nike-style home redesign (B-28)
- Any backend changes

## Build Command

./gradlew assembleDebug

## Completion Criteria

1. Tapping a recent run card in HomeScreen navigates to RunDetailScreen for that run.
2. CourseDetailScreen shows up to 5 leaderboard entries inline (or "아직 완주 기록이 없습니다").
3. "순위 보기" button still works and navigates to CourseLeaderboardScreen.
4. ProfileScreen shows bio text when available.
5. ProfileScreen has an "편집" button that opens inline editing for nickname and bio.
6. Save calls PUT /api/users/me and updates the displayed nickname/bio on success.
7. BottomNav third tab shows "코스" with Route icon.
8. Tapping "코스" tab shows CoursesLibraryScreen (placeholder tabs are fine).
9. ./gradlew assembleDebug succeeds with no errors.

## Commit Rule

- Run ./gradlew assembleDebug before committing.
- Show changed file list before commit.
- Commit only after successful build.
- Do NOT run git push unless explicitly asked.
- Commit message:
  feat(android): [B-27] fix recent run navigation, inline leaderboard, profile editing, nav redesign
```