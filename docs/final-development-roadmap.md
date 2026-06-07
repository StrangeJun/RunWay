# RunWay Final Development Roadmap

> 작성일: 2026-05-20  
> 기준 브랜치: `feature/android-tracking-reliability` (B-15 완료 상태)  
> 작성 근거: `docs/product-gap-analysis.md` + `docs/product-gap-analysis-review.md` + 실제 코드베이스 직접 확인

---

## 1. Purpose

이 문서는 세 가지 출처를 종합하여 RunWay의 실용적이고 현실적인 다음 개발 순서를 확정하기 위해 작성됐다.

1. **`docs/product-gap-analysis.md`** — 경쟁 앱 분석 및 기능 갭 식별
2. **`docs/product-gap-analysis-review.md`** — 위 문서의 코드베이스 기반 정확성 검토 및 우선순위 재조정
3. **실제 코드베이스 직접 검사** — 두 문서의 불일치를 코드로 직접 확인하여 최종 판단

두 분석 문서가 작성된 이후 Phase B-15(Tracking Reliability)가 구현됐다. 따라서 이 로드맵은 B-15 완료를 기준으로 그 다음 단계를 정의한다.

---

## 2. Source Documents Summary

### `docs/product-gap-analysis.md` 핵심 요점

- RunWay의 핵심 차별화는 **사용자 생성 코스 + 주변 코스 탐색 + 코스 리더보드** 3가지다.
- GPS 비정상 포인트 필터링 없음, Auto-Pause 없음을 P0로 분류했다.
- `CourseAttemptTrackingScreen` 지도 없음, Global Leaderboard 더미 데이터를 P0로 분류했다.
- B-16을 "Running Quality & Core UX Polish"로 제안하며 GPS 필터링, Auto-Pause, 코스 도전 지도, Leaderboard API 연결을 한 Phase에 묶었다.
- Certification Image를 P1로 상대적으로 높게 배치했다.

### `docs/product-gap-analysis-review.md` 핵심 수정 사항

- Crash recovery / persistent GPS queue를 "P0 중 P0"로 격상 — 앱 개발 당시 가장 치명적인 신뢰성 결함으로 판단.
- GPS 필터링에 대해 "없음"이 아닌 "delta filter는 있으나 raw point upload에는 적용 안 됨"으로 더 정확하게 기술.
- `pause()` 중 point 수집 없음을 확인 — `product-gap-analysis.md`의 오류 수정.
- `DiscoverViewModel`의 `TEST_LATITUDE/TEST_LONGITUDE` 하드코딩을 P0로 새로 추가.
- Certification Image를 P3로 하향 — 기록 신뢰성이 먼저.
- Personal Records를 P2로 하향 — GPS 정확성이 전제.
- 다음 Phase로 "Crash recovery / tracking reliability"를 권장.

### 두 문서의 주요 불일치

| 항목 | `product-gap-analysis.md` | `product-gap-analysis-review.md` | 최종 판단 |
|---|---|---|---|
| GPS 비정상 포인트 필터링 | 없음 (P0) | delta filter 있으나 raw 경로 오염 (P0) | B-15에서 `GpsPointValidator` 구현됨 → **해결** |
| Pause 중 point 수집 | 수집됨 (버그) | 수집 안 됨 (정상) | 코드 확인 → **정상** |
| Crash recovery | 약한 언급 (P0 미분류) | P0 최우선 | B-15에서 `PendingPointQueue`+`TrackingSessionStore` 구현됨 → **해결** |
| 다음 Phase | B-16 Running Quality | B-15 Crash Recovery | **B-15 완료됨. 다음은 B-16** |
| Certification Image | P1 | P3 | Review 문서 채택 — 신뢰성 먼저 |
| Global Leaderboard tab | P0 (API 연결) | P1 (dummy 제거 또는 재정의) | P1 채택 — real user test blocker 아님 |
| Nearby discovery 위치 | 미언급 | P0 (hardcoded 좌표) | 코드 확인 → **P0 채택** |

### 코드베이스 확인 후 최종 해석

- B-15에서 `GpsPointValidator`(12 m/s, 200 m, 1 m 필터), `PendingPointQueue`(Room), `TrackingSessionStore`(DataStore), `TrackingRecoveryDialog/ViewModel`이 모두 구현됐다. B-15는 완료 상태다.
- `DiscoverViewModel`은 여전히 `TEST_LATITUDE = 36.9706`, `TEST_LONGITUDE = 127.8718`을 사용한다.
- `LeaderboardScreen`은 `DUMMY_ENTRIES`를 하드코딩하고 있다.
- `CourseAttemptTrackingScreen`은 `RouteMapPlaceholder`를 사용한다.
- `HomeScreen`의 Nearby courses 섹션은 빈 `Row`에 주석만 있다.

---

## 3. Current Implementation Status

| 영역 | 현재 상태 | 코드베이스 근거 | 비고 |
|---|---|---|---|
| **Auth (이메일 회원가입/로그인)** | 완료 | `auth/controller/AuthController.java`, `LoginScreen.kt`, `SignupScreen.kt` | JWT Access+Refresh Token |
| **Token 자동 재발급** | 완료 | `core/network/TokenAuthenticator.kt`, `AuthService.reissue()` | 만료 시 자동 reissue, 실패 시 로그아웃 |
| **Free running 추적** | 완료 | `RunningTrackingScreen.kt`, `RunningTrackingViewModel.kt`, `RunTrackingManager.kt` | ForegroundService 기반, 수동 pause/resume |
| **ForegroundService** | 완료 | `RunTrackingService.kt`, `RunTrackingNotification.kt` | START_NOT_STICKY |
| **GPS 포인트 검증 (raw filtering)** | 완료 | `core/location/GpsPointValidator.kt` | B-15: 12 m/s, 200 m, 1 m 필터 |
| **Tracking crash recovery** | 완료 | `TrackingSessionStore.kt`, `TrackingRecoveryViewModel.kt`, `TrackingRecoveryDialog.kt` | B-15: MainScaffold에서 복구 다이얼로그 표시 |
| **Persistent pending point queue** | 완료 | `PendingPointQueue.kt`, `TrackingDatabase.kt`, `PendingRunPointDao.kt` | B-15: Room 기반, 업로드 성공 시 삭제 |
| **러닝 기록 (My Runs)** | 완료 | `MyRunsScreen.kt`, `MyRunsViewModel.kt` | `GET /api/runs/me`, 최대 50개 고정 조회 |
| **Run Detail** | 완료 | `RunDetailScreen.kt`, `RunDetailViewModel.kt` | Google Maps `RouteMapView` 포함 |
| **Google Maps 경로 미리보기** | 완료 | `RouteMapView.kt`, `RunDetailScreen.kt`, `CourseDetailScreen.kt`, `RunResultScreen.kt` | B-15 이전(Maps Phase)에서 구현 |
| **코스 생성 (from run)** | 완료 | `CreateCourseDialog.kt`, `POST /api/courses/from-run/{runId}` | RunResultScreen에서 접근 |
| **인근 코스 탐색 UI** | 부분 완료 | `DiscoverScreen.kt`, `DiscoverViewModel.kt` | **좌표 하드코딩**: `TEST_LATITUDE=36.9706` |
| **코스 상세** | 완료 | `CourseDetailScreen.kt`, `CourseDetailViewModel.kt` | `RouteMapView` 포함, 도전 시작 버튼 |
| **코스 도전 (시작/완주/포기)** | 완료 (지도 제외) | `CourseAttemptTrackingScreen.kt`, `CourseAttemptTrackingViewModel.kt` | **지도는 `RouteMapPlaceholder`** — 코스 경로 미표시 |
| **코스 리더보드** | 완료 | `CourseLeaderboardScreen.kt`, `CourseLeaderboardViewModel.kt` | 코스 상세에서 접근, 실제 API 연결 |
| **Global Leaderboard 탭** | 미완료 | `LeaderboardScreen.kt` — `DUMMY_ENTRIES` 하드코딩 | 실제 API 미연결, 제품 정의 불명확 |
| **프로필** | 완료 | `ProfileScreen.kt`, `ProfileViewModel.kt` | 닉네임, 이메일, 총 러닝 통계 |
| **HomeScreen Nearby Courses** | 미완료 | `HomeScreen.kt` 82-83번째 줄 — 빈 `Row`에 주석만 있음 | 실제 데이터 없음 |
| **Privacy Zone** | 미구현 | backend/android 어디에도 없음 | 코스 시작점 정밀 좌표 공개 상태 |
| **1km Splits** | 미구현 | `RunDetailScreen.kt`, backend 모두 없음 | `running_points` 데이터로 계산 가능 |
| **Auto-Pause** | 미구현 | `DefaultLocationTracker.kt`에 속도 임계값 없음 | 수동 pause만 있음 |
| **My Courses 화면** | 미구현 | `GET /api/courses/me` backend 있음, Android UI 없음 | ProfileScreen에 진입점 없음 |
| **온보딩** | 미구현 | 없음 | 최초 실행 시 Login/Signup으로 바로 이동 |
| **Personal Records** | 미구현 | backend/android 모두 없음 | 기록 신뢰성 확보 후 구현 예정 |

---

## 4. Real User Testing Readiness

### 준비된 것

- Auth 플로우 (회원가입 → 로그인 → 자동 토큰 재발급 → 로그아웃)
- 자유 런 시작/추적/완료/기록 저장 전체 플로우
- GPS 포인트 배치 전송 (5초마다, 실패 시 Room 큐 보존)
- Crash recovery (앱 강제 종료 후 재시작 시 복구 다이얼로그)
- 런 완료 후 코스 생성
- 주변 코스 탐색 (단, 하드코딩된 좌표로)
- 코스 상세 — 경로 지도, 도전 시작
- 코스 도전 시작/완주/포기 플로우
- 코스 리더보드 (완주자 기록 비교)
- Run Detail 지도 경로 표시

### 준비 안 된 것 (Blockers)

- **`DiscoverViewModel` 하드코딩 좌표** — "주변 코스"가 실제 사용자 위치와 무관한 충주시 좌표를 기준으로 조회됨. 테스트 사용자들이 자신 근처의 코스를 탐색할 수 없다.
- **`CourseAttemptTrackingScreen`에 코스 경로 없음** — RunWay의 핵심 가치인 "등록된 코스를 달린다"는 경험에서 코스가 지도에 보이지 않는다. 사용자가 어디로 가야 할지 알 수 없다.
- **Privacy Zone 없음** — 집 앞에서 시작한 코스의 정확한 GPS 좌표가 공개된다. 실사용자에게 개인정보 위험 노출.

### 경고와 함께 테스트 가능한 것

- `HomeScreen` Nearby Courses — 빈 섹션이 표시됨. 사용자 혼란 유발하나 앱 사용에 치명적이진 않음.
- `Global Leaderboard` 탭 — DUMMY_ENTRIES가 표시됨. 내부 테스트 환경에서는 혼란 유발 가능. 레이블 변경이나 임시 제거로 완화 가능.
- `RunResultScreen` "Free run" 하드코딩 — 코스 기반 런에서도 동일 표시. UX 불일치이나 기능 저해는 없음.
- 페이지네이션 고정 size — 기록이 50개 이상이면 이전 기록 누락. 초기 테스트에서는 해당 없음.

### 판단

> **내부 테스트 가능 — 단, Nearby Discovery 위치 수정과 CourseAttempt 지도 표시가 해결돼야 외부 소수 사용자 테스트를 진행할 수 있다.**
>
> Privacy Zone 없이 실제 주소가 노출되는 상태에서는 외부 사용자 테스트를 시작하면 안 된다.

---

## 5. Final Priority Framework

### P0 — 소수 실사용자 테스트 전 필수 수정

| 우선순위 | 기능 | 중요한 이유 | Backend 영향 | Android 영향 | 복잡도 | 권장 Phase |
|---|---|---|---|---|---|---|
| P0-1 | **코스 도전 중 코스 경로 지도 표시** | RunWay 핵심 UX — "코스를 달린다"는 경험이 없으면 차별화 가치 없음. `RouteMapPlaceholder`로는 전달 불가 | 없음 | 낮음 | Low | B-16 |
| P0-2 | **실제 GPS 위치 기반 Nearby Discovery** | `TEST_LATITUDE/TEST_LONGITUDE` 하드코딩으로 실사용자가 자신 근처 코스를 탐색 불가 | 없음 | 낮음 | Low | B-16 |
| P0-3 | **HomeScreen Nearby Courses 데이터 연결** | 빈 섹션이 그대로 노출. 실제 데이터 또는 CTA로 교체 필요 | 없음 | 낮음 | Low | B-16 |
| P0-4 | **Privacy Zone (코스 시작점 좌표 마스킹)** | 집/직장 앞 출발 코스의 정밀 좌표가 공개됨. 실사용자 개인정보 위험 | 중간 | 중간 | Medium | B-17 |

### P1 — P0 이후 MVP 폴리시

| 우선순위 | 기능 | 중요한 이유 | Backend 영향 | Android 영향 | 복잡도 | 권장 Phase |
|---|---|---|---|---|---|---|
| P1-1 | **Global Leaderboard 탭 정리** | DUMMY_ENTRIES 하드코딩 상태로 외부 공개 불가. 제품 정의 후 실제 데이터로 교체 또는 탭 제거 | 낮음 | 낮음 | Low | B-17 |
| P1-2 | **Auto-Pause** | 신호등·음수대 정지 중 타이머/거리 계속 누적. 모든 경쟁 앱 제공. 페이스가 부정확해짐 | 없음 | 중간 | Low | B-18 |
| P1-3 | **1km Splits** | 런 분석 기본. `RunDetailScreen`에 구간 페이스 표시. `running_points` 데이터로 계산 가능 | 없음 (계산만) | 중간 | Medium | B-18 |
| P1-4 | **My Courses 화면** | `GET /api/courses/me` backend 완료. Android UI만 없음. 내가 만든 코스 관리 불가 | 없음 | 낮음 | Low | B-19 |
| P1-5 | **코스 신고 기능** | User-generated public course에는 부적절한 코스 신고 채널이 조기 필요 | 중간 | 낮음 | Medium | B-19 |
| P1-6 | **Backend 데이터 일관성 방어** | finish/abandon 실패 시 `running_records`와 `course_attempts` 상태 불일치 가능성. client-provided distance 신뢰 위험 | 중간 | 없음 | Medium | B-17 |

### P2 — 차별화 강화 기능

| 우선순위 | 기능 | 중요한 이유 | Backend 영향 | Android 영향 | 복잡도 | 권장 Phase |
|---|---|---|---|---|---|---|
| P2-1 | **Personal Records** | B-15로 기록 신뢰성 확보됨. 이제 "5km PR 달성" 알림이 의미 있어짐 | 중간 | 중간 | Medium | B-20 |
| P2-2 | **코스 검색 (키워드)** | `DiscoverScreen` 검색 바가 장식. 이름 기반 검색 API 없음 | 중간 | 낮음 | Medium | B-20 |
| P2-3 | **온보딩 플로우** | 앱 첫 실행 시 RunWay 개념 설명 없음. 코스 기반 경쟁 차별화 전달 필요 | 없음 | 중간 | Low | B-19 |
| P2-4 | **런 완료 CTA 폴리시** | Run complete 후 코스 생성 제안이 더 자연스럽게 이어져야 core loop 완성 | 없음 | 낮음 | Low | B-19 |
| P2-5 | **GPS 경로 기반 리더보드 무결성** | 현재 finish 시 자동 `verified`. 코스를 실제 달렸는지 경로 매칭 필요 | 높음 | 낮음 | High | B-21 |
| P2-6 | **코스 평가 (rating)** | 저품질 코스 필터링 신호. 데이터가 쌓이면 탐색 UX 개선 | 중간 | 중간 | Medium | B-21 |
| P2-7 | **페이스/속도 차트** | Splits 이후 자연스러운 analytics 확장. `RunDetailScreen`에 시계열 차트 | 없음 | 중간 | Medium | B-20 |
| P2-8 | **이상 기록 감지** | 비정상적으로 빠른 완주(예: 5km를 1분) 자동 필터링 | 중간 | 없음 | Medium | B-21 |

### P3 — 이후 개선 사항

| 우선순위 | 기능 | 중요한 이유 | Backend 영향 | Android 영향 | 복잡도 | 권장 Phase |
|---|---|---|---|---|---|---|
| P3-1 | **공유 이미지 생성 (Certification Image)** | 사용자 획득 채널이지만 기록 신뢰성이 먼저. Android local Canvas 기반으로 시작 | 중간 | 중간 | Medium | B-22 |
| P3-2 | **월간/연간 통계** | 장기 트렌드. PR 기능 이후 의미가 커짐 | 중간 | 중간 | Medium | B-22 |
| P3-3 | **Streak (연속 달성)** | 리텐션 레이어. 기록 안정화 이후 | 낮음 | 낮음 | Low | B-22 |
| P3-4 | **Achievement/Badge** | 마일스톤 시각화. Streak와 함께 구현 | 중간 | 중간 | Medium | B-22 |
| P3-5 | **Push Notification** | FCM 연동. PR, 새 도전자, 주간 리마인더 | 높음 | 중간 | Medium | B-23 |
| P3-6 | **캘린더 뷰 (My Runs)** | 날짜별 운동 시각화. 현재 리스트 보완 | 없음 | 중간 | Medium | B-23 |
| P3-7 | **km 마일스톤 진동 알림** | 런 중 1km마다 진동. 1km Splits와 함께 구현하면 시너지 | 없음 | 낮음 | Low | B-18 |
| P3-8 | **배터리 최적화 안내** | 실기기 30분+ 추적 안정성. 사용자에게 배터리 최적화 제외 안내 | 없음 | 낮음 | Low | B-18 |

---

## 6. Final Recommended Next Phase

### B-15 완료 확인

다음 파일 모두 `feature/android-tracking-reliability` 브랜치에 존재함을 코드로 확인:

- `core/location/GpsPointValidator.kt` ✅
- `core/tracking/TrackingSessionSnapshot.kt` ✅
- `core/tracking/TrackingSessionStore.kt` ✅
- `core/tracking/local/TrackingDatabase.kt` ✅
- `core/tracking/PendingPointQueue.kt` ✅
- `ui/tracking/TrackingRecoveryDialog.kt` ✅
- `ui/tracking/TrackingRecoveryViewModel.kt` ✅

**B-15는 완료됐다. 다음 Phase는 B-16이다.**

---

### B-16: Course Attempt Map Overlay & Real Nearby Discovery

**브랜치:** `feature/android-map-and-discovery`

**왜 지금 이 Phase가 필요한가:**

B-15로 추적 신뢰성이 확보됐다. 이제 RunWay의 핵심 가치인 "코스를 달린다"는 경험과 "주변 코스를 발견한다"는 경험을 완성해야 한다. 이 두 가지는 P0 수준의 결함이다:

1. `CourseAttemptTrackingScreen`에서 코스 경로가 보이지 않으면 "어디로 달려야 할지 알 수 없다". 사용자가 RunWay의 차별점을 체감할 수 없다.
2. `DiscoverViewModel`이 충주시 좌표를 하드코딩하면 실제 사용자는 자신 근처의 코스를 탐색할 수 없다.
3. `HomeScreen`의 Nearby courses 섹션이 비어 있으면 앱이 미완성처럼 보인다.

이 세 가지는 모두 Backend 변경 없이 Android 단독으로 해결 가능하다.

**Scope (구현 대상):**

| 작업 | 파일 | 상세 |
|---|---|---|
| 코스 도전 중 코스 경로 오버레이 | `CourseAttemptTrackingScreen.kt`, `CourseAttemptTrackingViewModel.kt` | `RouteMapPlaceholder` → `RouteMapView(coursePoints)`. 코스 경로(회색 Polyline) + 현재 위치(파란 점) 동시 표시 |
| 코스 포인트 로드 | `CourseAttemptTrackingViewModel.kt` | `CourseRepository.getCoursePoints(courseId)` 호출, `MapPoint` 리스트로 변환 |
| 실제 GPS 위치 기반 탐색 | `DiscoverViewModel.kt` | `TEST_LATITUDE/TEST_LONGITUDE` 제거. Fused Location Provider로 현재 위치 1회 취득 후 탐색 |
| 위치 권한 처리 | `DiscoverViewModel.kt`, `DiscoverScreen.kt` | 위치 권한 없을 때 권한 요청 또는 안내 메시지 표시 |
| HomeScreen Nearby Courses 데이터 연결 | `HomeViewModel.kt`, `HomeScreen.kt` | `HomeViewModel`에서 현재 위치 기반 주변 코스 최대 5개 조회. 빈 상태면 "See all" CTA만 유지 |

**Out of Scope:**

- Privacy Zone (B-17에서 처리)
- Global Leaderboard 탭 정리 (B-17에서 처리)
- Auto-Pause, 1km Splits (B-18)
- 코스 도전 중 실시간 경로 비교(얼마나 코스에서 벗어났는지) — 별도 Phase

**Backend 변경:** 없음 (`GET /api/courses/{courseId}/points`와 `GET /api/courses/nearby` 이미 구현됨)

**Android 변경:**
- `CourseAttemptTrackingScreen.kt` — `RouteMapPlaceholder` 교체
- `CourseAttemptTrackingViewModel.kt` — coursePoints 상태 추가, `CourseRepository` 주입
- `DiscoverViewModel.kt` — 하드코딩 좌표 제거, `LocationTracker` 또는 `FusedLocationProvider` 주입
- `DiscoverScreen.kt` — 위치 권한 처리 로직
- `HomeViewModel.kt` — 현재 위치 기반 nearby courses 조회
- `HomeScreen.kt` — Nearby courses `Row` 실제 데이터 또는 빈 상태 처리

**Acceptance Criteria:**
- [ ] 코스 도전 시작 직후 지도에 코스 경로(Polyline)가 표시된다.
- [ ] 현재 내 위치가 파란 마커로 지도 위에 표시된다.
- [ ] `DiscoverScreen`에서 실제 기기 위치 기준으로 주변 코스가 로드된다.
- [ ] `DiscoverScreen`에서 위치 권한이 없을 때 명확한 안내가 표시된다.
- [ ] `HomeScreen`의 Nearby courses 섹션에 주변 코스 카드가 표시된다 (최대 5개).
- [ ] `HomeScreen`에서 주변 코스 카드 클릭 시 `CourseDetailScreen`으로 이동한다.

**Risks:**
- `DiscoverViewModel`에 `LocationTracker`를 직접 주입하면 위치 권한 흐름이 복잡해질 수 있다. DiscoverScreen에서 권한을 먼저 처리하고 위치를 전달하는 방식이 더 단순하다.
- `HomeViewModel`에서 위치를 사용하면 `HomeScreen` 진입마다 위치를 요청할 수 있다. 캐싱 또는 마지막 알려진 위치(last known location) 사용 검토 필요.
- 코스 포인트 로드가 실패하면 빈 지도가 표시된다. `RouteMapView`는 ≥2 포인트가 없을 때 Canvas 폴백을 사용하므로 graceful degradation 가능.

**권장 커밋 메시지:**
```
feat(android): [B-16] course attempt map overlay and real nearby discovery
```

---

## 7. Recommended Next 6 Phases

### B-16: Course Attempt Map Overlay & Real Nearby Discovery

**목표:** P0 UX 결함 해소 — 코스 경로 지도 표시 + 실제 위치 기반 탐색  
**주요 작업:**
- `CourseAttemptTrackingScreen`에 코스 경로 `RouteMapView` 오버레이 + 현재 위치 마커
- `DiscoverViewModel` hardcoded 좌표 제거, 실제 Fused Location 사용
- `HomeScreen` Nearby Courses 섹션 실제 API 데이터 연결

**Backend 영향:** 없음 (API 이미 구현됨)  
**Android 영향:** `CourseAttemptTrackingScreen.kt`, `DiscoverViewModel.kt`, `HomeViewModel.kt`  
**복잡도:** Low-Medium  
**왜 지금:** B-15 완료 후 P0 수준 UX 결함. Backend 변경 없이 빠르게 해결 가능.

---

### B-17: Privacy Zone & Backend Data Integrity

**목표:** 실제 사용자 공개 테스트 전 프라이버시 안전 + 데이터 무결성 방어  
**주요 작업:**
- **Backend**: 코스 `start_location`, `end_location`을 API 응답 시 반경 150m 이내 랜덤 offset 처리 (정밀 좌표 비공개). 설정에서 Privacy Zone 활성화/비활성화.
- **Backend**: `finishAttempt()`에서 impossible pace 방어 (예: duration이 distance/최고속도보다 짧으면 거부)
- **Backend**: zero/negative duration, 0 distance attempt 거부
- **Android**: `CourseDetailScreen`의 출발점 마커가 마스킹된 좌표를 사용함을 확인
- **Android**: Global Leaderboard 탭 정리 — DUMMY_ENTRIES 제거, "Featured Courses" 또는 "Trending" 형태로 재정의하거나 탭 자체를 Explore로 통합

**Backend 영향:** 중간 (`CourseService`, `CourseAttemptService` 수정)  
**Android 영향:** 낮음 (Global Leaderboard 탭 재설계)  
**복잡도:** Medium  
**왜 지금:** Privacy Zone 없이는 외부 공개 불가. Backend 데이터 무결성은 실사용 데이터 오염 방지에 필수.

---

### B-18: Run Analytics (1km Splits, Auto-Pause, km Milestone)

**목표:** 러닝 추적 품질 향상 — 경쟁 앱과의 기본 기능 격차 해소  
**주요 작업:**
- **Android**: `DefaultLocationTracker`에 Auto-Pause 로직 추가 — speed < 0.3 m/s가 10초 이상 지속될 때 자동 pause, 재개 감지
- **Android**: `RunDetailScreen`에 1km Splits 테이블 — `running_points` 데이터를 클라이언트에서 처리하여 km당 페이스 계산
- **Android**: 런 중 1km, 2km, 3km... 도달 시 진동 알림 + 현재 페이스 표시 (km 마일스톤)
- **Android**: Auto-Pause on/off 설정 옵션 (DataStore)
- **Android**: 배터리 최적화 제외 안내 (최초 서비스 시작 시)

**Backend 영향:** 없음  
**Android 영향:** `DefaultLocationTracker.kt`, `RunTrackingManager.kt`, `RunDetailScreen.kt`, `RunningTrackingScreen.kt`  
**복잡도:** Medium  
**왜 지금:** Auto-Pause는 모든 경쟁 앱이 제공하는 기본 기능. 1km Splits는 사용자가 훈련 피드백을 받는 기본 도구. B-17 이후 실사용자 테스트를 진행하면 이 두 가지 부재가 바로 피드백으로 나온다.

---

### B-19: My Courses, Onboarding, Course Report

**목표:** 앱 완성도 향상 — 내 코스 관리, 첫 사용자 경험, 기본 안전 장치  
**주요 작업:**
- **Android**: My Courses 화면 — `ProfileScreen`에서 "My Courses" 메뉴 추가, `GET /api/courses/me` 연결, 코스 목록 + 발행/보관 상태 표시
- **Android**: Onboarding 3-step — 앱 최초 실행 시: RunWay 소개 → 코스 개념 설명 → 시작 버튼. DataStore에 완료 여부 저장.
- **Android**: Run completion CTA 개선 — `RunResultScreen`에서 "이 경로로 코스 만들기" 버튼 더 눈에 띄게. 코스 생성 성공/실패 피드백 개선.
- **Backend + Android**: 코스 신고 API (`POST /api/courses/{courseId}/report`) + Android 신고 버튼 (`CourseDetailScreen`)

**Backend 영향:** 낮음 (신고 API 추가)  
**Android 영향:** 중간 (`ProfileScreen`, `RunResultScreen`, 신규 `OnboardingScreen`)  
**복잡도:** Low-Medium  
**왜 지금:** My Courses는 코스 생성자가 자신의 코스를 관리할 수단. 온보딩은 신규 사용자 이탈 방지. 코스 신고는 user-generated content의 기본 안전 장치.

---

### B-20: Personal Records & Course Search

**목표:** 앱 재방문 이유 제공 — 기록 성장 확인 + 탐색 개선  
**주요 작업:**
- **Backend**: Personal Records API — `GET /api/runs/me/records`: 최고 5km, 10km, 최장 거리, 최빠른 페이스 집계
- **Android**: Personal Records 섹션 — `ProfileScreen`에 PR 카드. "오늘 5km PR 달성!" 알림.
- **Backend**: 코스 키워드 검색 API — `GET /api/courses/nearby?keyword=탄금대` (name ILIKE 추가)
- **Android**: `DiscoverScreen` 검색 바 활성화
- **Android**: `RunDetailScreen`에 페이스/속도 시계열 차트 (선 그래프, Canvas 기반)

**Backend 영향:** 중간 (`RunningController`에 records endpoint, `CourseController`에 keyword 파라미터)  
**Android 영향:** 중간  
**복잡도:** Medium  
**왜 지금:** B-15로 GPS 필터링이 정착됐고, B-18로 1km Splits가 있으면 PR 계산 근거가 생긴다. B-19 온보딩 이후 신규 사용자가 들어오면 리텐션 레이어가 필요해진다.

---

### B-21: Share Image & Leaderboard Integrity

**목표:** 사용자 획득 채널 + 리더보드 신뢰도 기반 구축  
**주요 작업:**
- **Android**: 완주 인증 카드 (Android Canvas 기반 로컬 생성) — 코스명, 완주 시간, 간단 경로 썸네일, 공유 버튼 → Android ShareSheet. **Backend S3 없이** 로컬 생성으로 시작.
- **Android**: 자유 런 결과 공유 이미지 — `RunResultScreen`에서 Canvas 기반 카드 이미지 생성
- **Backend**: 이상 기록 감지 — `finishAttempt()` 시 코스 거리 대비 불가능한 페이스(예: 1km/min 이상) 자동 `rejected` 처리
- **Backend**: GPS 경로 간단 검증 — 완주 GPS 경로의 시작/종료점이 코스 시작/종료점에서 500m 이내인지 확인 (Phase 2 경로 매칭의 축소 버전)
- **Backend**: 코스 평가 API (`POST /api/courses/{courseId}/ratings`) + Android 완주 후 평가 팝업

**Backend 영향:** 중간-높음 (이상 감지, 경로 간단 검증, 평가 API)  
**Android 영향:** 중간  
**복잡도:** Medium-High  
**왜 지금:** 사용자 기반이 생기기 시작하면 리더보드 무결성이 핵심 신뢰도 문제로 부상한다. 공유 이미지는 신규 사용자 유입의 가장 직접적인 채널이나, backend 없이 로컬 생성으로 복잡도를 낮춘다.

---

## 8. Features to Postpone

### ❌ 전체 소셜 피드 (Activity Feed / 팔로우/팔로워)

**이유:** 팔로우 테이블, 피드 집계 쿼리, 알림 시스템이 별도 복잡도를 갖는다. 코어 러닝/코스 루프가 안정화되고 사용자 기반이 생긴 후에 의미가 있다. 지금 만들면 빈 피드만 보인다.

### ❌ AI 이미지 생성

**이유:** LLM API 비용, 프롬프트 엔지니어링, 이미지 품질 관리가 복잡하다. Android Canvas 기반 Certification Image로 공유 기능의 80%를 달성할 수 있다.

### ❌ 훈련 계획 (Training Plan)

**이유:** 코칭 콘텐츠와 도메인 지식이 필요하다. RunWay의 차별화인 "코스 기반 경쟁"과 방향이 다르다. 전문 러닝 코치 설계 없이는 경쟁 앱 대비 열위.

### ❌ 수익화 모델 (구독/인앱 결제)

**이유:** 사용자 리텐션 메트릭이 안정화된 후 설계해야 기능 우선순위가 비즈니스 로직에 오염되지 않는다.

### ❌ 웨어러블 연동 (Wear OS / Galaxy Watch)

**이유:** 각 플랫폼별 별도 SDK와 에코시스템이 필요하다. 스마트폰 GPS로 충분한 MVP 단계에서는 불필요하다.

### ❌ 지도 위 코스 수동 그리기 (Route Editor)

**이유:** RunWay의 핵심 가치는 "실제로 달린 경로를 코스로 만든다"는 것이다. 지도 위 수동 편집은 이 차별화와 반대 방향이다.

### ❌ 대규모 추천 엔진

**이유:** 추천 알고리즘은 충분한 사용자 행동 데이터가 쌓인 후에 의미가 있다. 초기에는 PostGIS `ST_Distance` 거리순 정렬로 충분하다.

### ❌ 그룹 런 / 실시간 위치 공유

**이유:** WebSocket 기반 실시간 위치 공유는 인프라 복잡도(서버 부하, 연결 관리)가 매우 높다. 개인 러닝 기록이 안정화된 후에 도입한다.

### ❌ Full GPS Route Verification (Hausdorff Distance)

**이유:** 완전한 경로 매칭은 계산 비용이 높고 구현이 복잡하다. B-21에서 시작점/종료점 반경 확인으로 시작하고, 실제 부정 사례가 확인된 후 고도화한다.

### ❌ Google/Apple Health 연동

**이유:** 플랫폼별 헬스 API 통합 복잡도가 높다. 핵심 추적 기능 완성 이후에 추가하는 것이 적합하다.

---

## 9. Actionable Prompt for the Next Implementation Phase

다음 프롬프트를 그대로 Claude Code에 입력하면 B-16 구현을 시작할 수 있다.

---

```
Implement Android Phase B-16: Course Attempt Map Overlay & Real Nearby Discovery.

Current branch: feature/android-tracking-reliability
New branch to create: feature/android-map-and-discovery

Branch from: feature/android-tracking-reliability (B-15 completed)

Before implementing, confirm that the current branch is feature/android-tracking-reliability.
Then create and switch to feature/android-map-and-discovery.

## Scope

### Task 1: CourseAttemptTrackingScreen — Course Route Overlay

Replace RouteMapPlaceholder in CourseAttemptTrackingScreen.kt with an actual
Google Maps view that shows:
- The course path as a gray/green Polyline (from CourseRepository.getCoursePoints)
- The user's current GPS position as a blue dot marker (from manager.state)

Files to update:
- ui/attempt/CourseAttemptTrackingScreen.kt
  Replace: import and usage of RouteMapPlaceholder
  Add: RouteMapView with coursePoints + current location marker
- ui/attempt/CourseAttemptTrackingViewModel.kt
  Add: CourseRepository injection
  Add: coursePoints: List<MapPoint> state loaded via getCoursePoints(courseId)
  Add: init block that calls getCoursePoints

The courseId is already available in CourseAttemptTrackingViewModel via SavedStateHandle.
CourseRepository.getCoursePoints(courseId) already exists and returns NetworkResult.

### Task 2: DiscoverViewModel — Real GPS Location

Replace TEST_LATITUDE and TEST_LONGITUDE hardcoding with actual device location.

Files to update:
- ui/discover/DiscoverViewModel.kt
  Remove: TEST_LATITUDE, TEST_LONGITUDE constants
  Add: FusedLocationProviderClient or LocationTracker injection
  Logic: on init, request last known location (non-blocking). If available, use it.
         If not available, trigger a one-time location request.
         Show a loading or "위치를 가져오는 중..." state while waiting.
         If permission denied, show location permission request or error message.
- ui/discover/DiscoverScreen.kt
  Add: location permission handling (check, request if missing, handle denial)
  Show: appropriate state when location is unavailable

Use FusedLocationProviderClient (play-services-location is already in build.gradle.kts)
via Hilt injection in a new LocationModule or via ApplicationContext.
Keep it simple: getLastLocation() first, fallback to requestSingleUpdate if null.

### Task 3: HomeScreen Nearby Courses — Real Data

Replace the empty Row placeholder in HomeScreen with actual nearby course cards.

Files to update:
- ui/home/HomeViewModel.kt
  Add: nearbyCourses: List<NearbyCourseItem> state
  Add: isLoadingNearbyCourses state
  On init or when location available: call CourseRepository.getNearbyCourses(lat, lon, 3000)
  Load max 5 courses for the home preview.
  Reuse same location acquisition pattern as DiscoverViewModel (last known location).
- ui/home/HomeScreen.kt
  Replace: the empty Row (lines 75-84) with items(viewModel.nearbyCourses) { course -> NearbyCourseCard(...) }
  Show: if no courses nearby, remove the section header OR show a simple empty state text
  Wire: NearbyCourseCard click → onNavigateToCourseDetail callback (already passed to HomeScreen)

HomeScreen already has onNavigateToCourseDetail available via MainScaffold.
Check MainScaffold.kt and RunwayNavGraph.kt to verify the callback chain.

## Out of Scope for B-16

- Privacy Zone masking (B-17)
- Global Leaderboard tab cleanup (B-17)
- Auto-Pause (B-18)
- 1km Splits (B-18)
- Real-time course path deviation tracking during attempt (later)

## Architecture Notes

- CourseRepository is already injected in CourseDetailViewModel — follow same pattern
- RouteMapView already accepts List<MapPoint> — coursePoints need to be converted to MapPoint
- For location in ViewModels: inject @ApplicationContext and use FusedLocationProviderClient directly
  OR create a separate wrapper. Check if LocationTracker/DefaultLocationTracker can be reused.
- Do not use hardcoded coordinates anywhere. If location is unavailable, show a clear state.

## Completion Criteria

1. CourseAttemptTrackingScreen shows the course route (Polyline) on a Google Map after start.
2. The user's current GPS position is visible as a blue marker during attempt.
3. DiscoverScreen loads courses based on actual device location, not hardcoded coordinates.
4. DiscoverScreen shows a clear message or permission request when location is unavailable.
5. HomeScreen Nearby Courses section shows up to 5 real nearby course cards.
6. HomeScreen course card tap navigates to CourseDetailScreen.
7. Build must succeed: ./gradlew assembleDebug

## Commit Rule

- Run ./gradlew assembleDebug before committing.
- Show changed file list before commit.
- Commit only after successful build.
- Do NOT run git push.
- Commit message must include phase label:
  feat(android): [B-16] course attempt map overlay and real nearby discovery
```

---

## 10. Output Requirements

**생성/수정된 파일:**
- `docs/final-development-roadmap.md` (신규 생성)

**최종 권장 다음 Phase:**
> **B-16: Course Attempt Map Overlay & Real Nearby Discovery**  
> 브랜치: `feature/android-map-and-discovery`

**즉시 해결이 필요한 Top 10 갭:**

| 순위 | 갭 | 심각도 | 해결 Phase |
|---|---|---|---|
| 1 | 코스 도전 중 코스 경로 지도 없음 (`RouteMapPlaceholder`) | P0 | B-16 |
| 2 | Nearby Discovery 하드코딩 좌표 (`TEST_LATITUDE=36.9706`) | P0 | B-16 |
| 3 | HomeScreen Nearby Courses 빈 섹션 | P0 | B-16 |
| 4 | Privacy Zone 없음 (집 주소 노출 위험) | P0 | B-17 |
| 5 | Global Leaderboard 탭 `DUMMY_ENTRIES` 하드코딩 | P1 | B-17 |
| 6 | Auto-Pause 없음 | P1 | B-18 |
| 7 | 1km Splits 없음 | P1 | B-18 |
| 8 | My Courses 화면 없음 (API는 있음) | P1 | B-19 |
| 9 | 온보딩 없음 | P2 | B-19 |
| 10 | Personal Records 없음 | P2 | B-20 |

**로드맵 요약:**

| Phase | 이름 | 핵심 목적 | Backend | Android |
|---|---|---|---|---|
| B-15 | Tracking Reliability | 크래시 복구, GPS 필터, Room queue | 없음 | **완료** |
| **B-16** | **Map & Real Discovery** | **코스 도전 지도, 실제 위치 탐색** | **없음** | **낮음** |
| B-17 | Privacy & Integrity | 좌표 마스킹, 데이터 무결성 방어, Leaderboard 탭 정리 | 중간 | 낮음 |
| B-18 | Run Analytics | Auto-Pause, 1km Splits, km 마일스톤 알림 | 없음 | 중간 |
| B-19 | My Courses & Onboarding | 내 코스 관리, 온보딩, 코스 신고 | 낮음 | 중간 |
| B-20 | Personal Records & Search | PR API, 코스 검색, 페이스 차트 | 중간 | 중간 |
| B-21 | Share & Leaderboard Integrity | 공유 카드, 이상 기록 감지, 코스 평가 | 중간-높음 | 중간 |

**전제 및 가정:**

1. B-15가 `feature/android-tracking-reliability` 브랜치에 완료 커밋됐으며, main에 merge되기 전이라도 B-16은 동일 브랜치에서 파생한다.
2. `GET /api/courses/{courseId}/points`와 `GET /api/courses/nearby`는 backend에 이미 구현돼 있다. (확인됨)
3. Privacy Zone(B-17)에서 좌표 마스킹은 API 응답 레이어에서 처리하는 것을 전제한다. DB 스키마 변경 최소화.
4. Global Leaderboard 탭의 "제품 정의"는 B-17 착수 전에 결정이 필요하다 — 탭 제거, Featured Courses로 변경, 또는 다른 형태.
5. 이 로드맵은 독립 개발자 1명 또는 소규모 팀 기준이다. 동시 Backend+Android 개발이 가능하면 B-17~B-18 일부는 병렬 진행 가능하다.

**주의:** git push는 명시적으로 요청받기 전까지 실행하지 않는다.
