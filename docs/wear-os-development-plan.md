# RunWay Wear OS Development Plan

> 작성일: 2026-06-07
> 기준 브랜치: `main` (B-36 이후 현재 상태)
> 작성 근거: 경쟁 앱 리서치 + 현재 코드베이스 직접 분석 + Wear OS 공식 문서

---

## 1. Purpose

### 왜 RunWay에 워치 앱이 필요한가

러닝 중 스마트폰은 손에 들거나 암밴드에 고정해야 한다. 이 물리적 마찰이 사용자 경험을 해치는 핵심 지점이다.

**현재 RunWay의 문제점 세 가지:**

1. **런 시작/정지를 위해 매번 폰을 꺼내야 한다.**
   Auto-pause가 있어도 의도적 pause/finish는 폰 조작이 필요하다.

2. **코스 도전 중 이탈 경고를 확인하려면 폰 화면을 봐야 한다.**
   달리면서 폰을 확인하는 것은 안전 위험이다.

3. **실시간 메트릭(페이스, 거리, 심박수)을 손목에서 바로 볼 수 없다.**
   러닝 특화 앱의 기본 가치 중 하나가 빠져 있다.

**워치 앱이 해결하는 것:**
- 손목에서 런 시작/일시정지/완료
- **런 전 목표 설정 (시간/거리/인터벌) — 폰 꺼내지 않고 바로 설정**
- 현재 페이스·거리·시간·심박수 실시간 표시
- 런 중 목표 달성률/잔여량 표시
- 코스 이탈 시 진동(haptic) 알림
- 폰 조작 없이 코스 도전 진행 상황 확인

**무엇을 목표로 하지 않는가:**
- 코스 탐색, 리더보드 검색, 프로필 편집 등 "탐색형" 기능은 폰 앱에 유지한다.
- 워치를 폰의 대체재가 아니라 런 세션 전용 인터페이스로 포지셔닝한다.

---

## 2. Competitor Watch App Analysis

| App | Watch Strength | Weakness / Limitation | What RunWay Should Learn | What RunWay Should Avoid |
|---|---|---|---|---|
| **Nike Run Club** | Audio Guided Runs — 코치 목소리로 동기부여. Apple Watch standalone GPS 지원. Always-On 지원. | Wear OS 앱이 Apple Watch 대비 기능 열세. 코스 내비게이션 없음. 장기 retention 낮음. | 손목에서 바로 런을 시작하는 경험이 핵심 마찰 제거 포인트. 간결한 3메트릭(pace/distance/time) 레이아웃. | 오디오 코칭이 없으면 감성적 동기부여 공백이 생김 — 런 완료 요약에서 텍스트 보상으로 보완 가능. |
| **Strava** | 세계 최대 커뮤니티. Segment 경쟁. Apple Watch 오프라인 지도 라우트(2026.01 출시). | Wear OS 앱은 Apple Watch 대비 기능 열세. 오프라인 라우트는 유료 구독 전용. | 라우트를 시계로 가져와 GPS 오버레이로 표시하는 방향이 장기 목표. Data Layer API로 폰에서 워치로 코스 경로 전송 가능. | 핵심 내비게이션 기능을 구독 뒤에 잠그는 것. |
| **Samsung Health** | Dual-band GPS로 도심 정확도 향상. Running Dynamics 6종(좌우 불균형, 접지 시간, 수직 진동 등). Daily Cardio Load. | 타사 폰에서 일부 기능 제한. 전문 러닝 앱보다 범용 헬스 앱. | 러닝 폼 데이터(수직 진동, 접지 시간)는 RunWay의 AI 자세 분석과 연계 가능한 방향. | 즉시 범용 피트니스 플랫폼으로 확장하는 것 — 러닝에 집중해야 한다. |
| **Garmin** | 완전 독립형(standalone). 데이터 스크린 완전 커스터마이징. ClimbPro(등반 가이드). 배터리 최상위(40시간 GPS). Race Screen(목표 대비 현재 상태). | 전용 OS(Wear OS 아님). 스마트워치 앱 생태계 약함. Connect+ 구독 도입으로 기존 사용자 반발. | 다중 데이터 스크린 스와이프, 큰 1개 메트릭 + 작은 보조 메트릭 레이아웃이 가독성 표준. 코스 안내(Back-to-Start) 자체가 주요 기능. | 하드웨어 의존 생태계. 구독 모델로 기존 무료 기능 잠금. |
| **Adidas Running** | Wear OS Tiles 2개(통계, 빠른 시작) + Complications 3개로 런처 통합. 신발 트래킹. | Interval Training·Training Plans·Voice Coach는 폰 연결 필수. 기능 깊이 부족. | Tile에서 "빠른 런 시작" 버튼은 최소 마찰로 런을 시작하게 하는 좋은 패턴. | 주요 기능을 폰 연결 없이는 불가능하게 설계하는 것. |
| **Runkeeper** | Apple Watch standalone GPS. Apple Health/Workouts 자동 연동. 초보자 친화적 단순 UI. | Wear OS 기능 깊이 부족. GPS 거리 과소 측정 지속 보고. 워치 동기화 실패율 높음. | 폰과 연결 없이도 런 데이터가 로컬 저장 → 재연결 시 자동 동기화되는 흐름이 필수. | 동기화 실패율이 높으면 사용자 신뢰 붕괴 — 재시도/재전송 로직 견고하게 설계해야 함. |

### 핵심 관찰

- **Wear OS 앱은 대부분 Apple Watch 앱 대비 기능이 뒤처진다.** RunWay가 Wear OS에서 기능 완성도를 보장한다면 경쟁 우위가 될 수 있다.
- **코스 내비게이션(라우트 오버레이)은 Strava Apple Watch 앱이 2026.01에서야 유료로 출시했다.** RunWay는 이것을 무료로 제공할 수 있다.
- **독립형 GPS(standalone) 지원 여부가 사용자 경험을 가른다.** Companion 모드로 시작하더라도 아키텍처는 standalone 확장을 막지 않도록 설계해야 한다.

---

## 3. RunWay Current App Capabilities

현재 코드베이스에서 Wear OS와 직접 관련된 구현 상태를 정리한다.

### 러닝 추적 핵심 구조

| 컴포넌트 | 파일 | Wear OS 연관성 |
|---|---|---|
| `RunTrackingManager` | `core/tracking/RunTrackingManager.kt` | Singleton StateFlow — 워치가 상태를 구독하거나 명령을 보낼 대상 |
| `RunTrackingService` | `core/tracking/RunTrackingService.kt` | `ForegroundService` — 워치 명령 수신 후 이 서비스에 action 전달 |
| `RunTrackingState` | `core/tracking/RunTrackingState.kt` | 워치로 전송할 메트릭의 source of truth |
| `PendingPointQueue` | `core/tracking/PendingPointQueue.kt` | Room 기반 크래시 안전 큐 — 워치에는 별도 큐 불필요 (폰이 처리) |
| `DefaultLocationTracker` | `core/location/DefaultLocationTracker.kt` | 3초 간격 FusedLocationProvider — Companion 모드에서 폰 GPS 유지 |
| `GpsPointValidator` | `core/location/GpsPointValidator.kt` | 12 m/s, 200 m, 1 m 필터 — 이미 구현됨 |
| `CadenceTracker` | `core/cadence/CadenceTracker.kt` | 폰 가속도계 기반 케이던스 — 워치 가속도계로 대체 가능 |
| `TrackingSessionStore` | `core/datastore/TrackingSessionStore.kt` | 크래시 복구 DataStore — Companion 모드에서 폰 측만 사용 |

### 코스 도전 관련

| 컴포넌트 | 파일 | 설명 |
|---|---|---|
| `CourseAttemptTrackingViewModel` | `ui/attempt/CourseAttemptTrackingViewModel.kt` | 이탈 거리 계산, 코스 진행률 — 워치 화면에 표시할 데이터 |
| `CourseAttemptTrackingScreen` | `ui/attempt/CourseAttemptTrackingScreen.kt` | 현재 `RouteMapPlaceholder` — 실제 지도 미표시 상태 |
| `CourseAttemptApi` | `data/attempt/remote/CourseAttemptApi.kt` | `POST /api/course-attempts/{id}/finish` 등 — 폰이 호출 유지 |

### Auto-pause 동작

`RunTrackingManager`의 Auto-pause 로직: 속도 < 0.5 m/s 가 10회 연속이면 자동 일시정지, 속도 > 1.0 m/s 가 3회 연속이면 자동 재개. 워치에서 manual pause/resume 명령이 오면 이 카운터를 리셋해야 한다.

### 메트릭 현재 상태 (`RunTrackingState`)

워치로 전송해야 할 기존 상태 필드:
- `distanceMeters: Double`
- `durationSeconds: Long`
- `currentPaceMinPerKm: Float`
- `isRunning: Boolean`, `isPaused: Boolean`
- `runId: String?` (서버 측 런 ID)
- 케이던스(cadence): `CadenceTracker`에서 별도 StateFlow로 제공

---

## 4. Wear OS Product Direction

### 권장 포지셔닝

> **RunWay Watch = 런 세션 전용 경량 컨트롤러**

워치는 "런을 더 편하게 실행하는 입력 장치"다. 코스 발견, 소셜 기능, 프로필, 설정은 모두 폰에 남긴다.

### 역할 분담

```
┌─────────────────────────────────────┐  ┌─────────────────────────────────┐
│            폰 앱 (Phone)             │  │          워치 앱 (Watch)          │
├─────────────────────────────────────┤  ├─────────────────────────────────┤
│ • 코스 탐색 / 탐색 화면               │  │ • 런 시작 / 일시정지 / 완료       │
│ • 코스 상세 / 리더보드                │  │ • 실시간 메트릭 표시               │
│ • GPS 수집 (FusedLocationProvider)  │  │ • 코스 이탈 진동 알림             │
│ • 배치 업로드 / PendingPointQueue     │  │ • 코스 진행률 표시                │
│ • JWT 인증 / 백엔드 API 호출          │  │ • 런 완료 요약                   │
│ • Run Result / 공유 이미지            │  │ • (나중) 심박수 실시간             │
│ • 자세 분석 / AI                     │  │ • (나중) Tile / Complication     │
│ • 프로필 / 설정 / 온보딩              │  │                                 │
└─────────────────────────────────────┘  └─────────────────────────────────┘
            ↑↓ Wear OS Data Layer API
```

---

## 5. MVP Scope

Wear OS MVP에서 구현할 기능:

### W-MVP-0: 런 전 목표 설정

워치에서 런 시작 전 목표를 설정한다. 폰 앱의 `GoalSetupSheet`와 동일한 세 가지 목표 타입을 지원하되, 워치 화면(원형, 소형)에 맞게 UX를 재설계한다.

**지원 목표 타입:**

| 타입 | 워치 설정 방식 | 폰 앱과 차이점 |
|---|---|---|
| **없음** | 홈에서 바로 시작 | 목표 없이 자유 런 |
| **시간 목표** | 크라운/베젤로 분 선택 (1~360분) | 폰은 시간+분 2단, 워치는 분 단일 휠 |
| **거리 목표** | 크라운/베젤로 km 선택 (0.5km 단위, 0.5~50km) | 폰은 km+소수점 2단, 워치는 단계별 선택 |
| **인터벌** | 운동/휴식/세트 3단계 순서 설정 | 폰의 준비운동·쿨다운 생략, 핵심만 |

**인터벌 목표 — 워치 전용 단순화:**
- 폰의 `IntervalGoal` (준비운동 + 운동 + 회복 + 쿨다운 + 세트)은 워치 화면에서 설정하기 너무 복잡하다.
- 워치에서는 **`WorkDuration + RestDuration + Sets`** 3개 파라미터만 설정한다.
- 폰에서 보내는 `IntervalGoal`로 매핑 시: warmup=None, cooldown=None, paceTarget=None으로 기본값 처리.

**목표 진행 표시 (런 중):**
- `WatchMetricSnapshot`에 `goalProgressPercent: Int?`, `goalRemaining: String?` 추가
- 시간 목표: 남은 시간 표시 ("08:23 남음")
- 거리 목표: 남은 거리 표시 ("2.3km 남음") + 진행바
- 인터벌: 현재 세그먼트 표시 ("운동 3/5", "회복 중") + 세그먼트 남은 시간 + 자동 전환 진동

### W-MVP-1: 자유 런 시작 및 실시간 메트릭

- 워치에서 목표 설정 후 또는 목표 없이 런 시작 → 폰에 `START_RUN(goal)` 명령 전송
- 폰이 `RunTrackingService` 시작, GPS 수집, 서버에 런 등록
- 폰이 `RunTrackingManager`에 목표 주입 → 목표 진행 계산 시작
- 워치 화면에 실시간 표시:
  - 경과 시간 (대형 주 메트릭)
  - 현재 거리 (km)
  - 현재 페이스 (분/km)
  - 목표 진행률 또는 잔여량
  - (W-6에서) 심박수 (bpm)

### W-MVP-2: 일시정지 / 재개 / 완료

- 워치에서 pause → 폰에 `PAUSE_RUN` 명령 → `RunTrackingManager.pause()`
- 워치에서 resume → 폰에 `RESUME_RUN` 명령 → `RunTrackingManager.resume()`
- 워치에서 finish → 폰에 `FINISH_RUN` 명령 → 폰이 서버 `finish()` API 호출
- 각 명령 후 폰이 상태를 워치로 재전송 (확인)

### W-MVP-3: 코스 도전 Companion Mode

- 폰에서 코스 도전 시작 → 폰이 `RUN_STATE`(코스 정보 포함)를 워치로 전송
- 워치 화면: 코스명, 진행률(%), 남은 거리, 이탈 경고
- 코스 이탈 감지 시 폰 → 워치로 `DEVIATION_WARNING` 전송 → 워치 진동

### W-MVP-4: 런 완료 요약

- 런 완료 후 워치에 기본 요약 표시:
  - 총 거리
  - 총 시간
  - 평균 페이스
- "폰에서 자세히 보기" 안내 텍스트 (상세 분석은 폰)

---

## 6. Out of Scope for MVP

MVP에서 구현하지 않는 것:

| 기능 | 이유 |
|---|---|
| 워치에서 코스 탐색 / 주변 코스 목록 | 작은 화면에서 탐색 UX 복잡도 높음, 폰에서 충분 |
| 워치에서 지도 렌더링 | 배터리 소모 크고 복잡도 높음, 차후 단계 |
| 소셜 피드 / 리더보드 브라우징 | 워치 = 실행 전용 |
| 워치에서 계정 로그인 | 폰 앱 연동으로 토큰 공유 처리 |
| 폰 앱의 인터벌 전체 옵션 (준비운동/쿨다운/페이스 목표) | 워치는 단순화된 3파라미터 인터벌로 제공 |
| 공유 이미지 / 런 결과 상세 | 폰에서 처리 |
| Tiles / Complications | W-9로 분리 |
| 자세 분석 연동 | 워치 자원으로는 불가, 폰 AI 기능 유지 |

---

## 7. Technical Architecture Options

### Option A: Watch as Companion Controller (기존 초안)

```
워치 → (MessageClient) → 폰 → GPS 수집 + 서버 API 호출
폰   → (DataClient)    → 워치 → 메트릭 표시
```

- 폰이 GPS와 배터리 소모 작업 모두 담당
- 워치는 UI + 명령 전송만
- **단점: 폰이 Bluetooth 범위 밖이거나 주머니 안에서 연결이 불안정하면 워치 앱이 멈춤**
- 실제 러닝 환경에서 폰을 가져가지 않는 경우를 전혀 처리 못함

### Option B: Watch Standalone + Sync (권장 방향으로 변경)

```
런 중:  워치 → Health Services (GPS + 심박수 + 케이던스) → 워치 로컬 저장
런 후:  워치 ↔ 폰 연결 시 → 워치 데이터를 폰으로 전송 → 폰이 서버 업로드
```

- 폰 없이 독립 실행 (런 중 폰 불필요)
- 런 후 또는 연결 복원 시 자동 동기화
- GPS, 심박수, 케이던스는 워치 Health Services로 수집
- 목표 관리 로직은 워치 내부에서 실행
- **단점: 워치 배터리 소모 증가, 개발 복잡도 높음**

### Option C: Hybrid — 기본 Standalone + 폰 실시간 연동 (최종 권장)

```
폰 있음:  워치 센서 수집 + 폰에 실시간 메트릭 미러링 + 폰이 서버 실시간 업로드
폰 없음:  워치 센서 수집 + 워치 로컬 저장 → 재연결 시 폰으로 전송 → 서버 업로드
```

- **워치가 항상 독립적으로 GPS·심박수·케이던스를 수집한다.**
- 폰이 연결된 경우: 실시간으로 폰에 데이터 미러링 → 폰이 서버에 배치 업로드
- 폰이 없거나 연결 불안정한 경우: 워치 내장 스토리지에 저장 → 재연결 후 동기화
- 서버 API 호출(start/finish)은 여전히 폰이 담당 (JWT 토큰은 폰에 있음)
- 완전 offline 상태에서도 런이 중단되지 않음

### 권장: Option C (Hybrid Standalone)

> **핵심 원칙:** 워치는 런 중 항상 독립적으로 데이터를 수집한다. 폰 연결은 "업로드 채널"이지 "전제 조건"이 아니다.

**이유:**
- 러닝 중 폰을 집에 두거나 암밴드에서 분리된 상태는 매우 흔함
- Strava, Garmin, NRC 모두 standalone GPS를 핵심 기능으로 제공
- 워치에서 수집한 GPS는 폰 GPS보다 손목 위치에서 더 정확할 수 있음
- Option A는 폰-워치 Bluetooth가 끊기면 런 추적 자체가 실패함

**구현 순서:**
- W-1~W-3: 워치 모듈 + 목표 설정 화면 (폰 연결 상태에서도 테스트)
- W-4: 워치 Health Services로 GPS + 심박수 수집 시작 (핵심 변경)
- W-5: 워치 로컬 저장 + 폰 동기화 프로토콜
- W-6: 폰과 연결 시 실시간 미러링, 분리 시 로컬 버퍼
- W-7+: 고도화 (오프라인 목표 추적, Tile)

---

## 8. Android/Wear OS Technical Stack

| 영역 | 기술 | 목적 |
|---|---|---|
| **Wear OS 모듈** | `com.android.application` (wear) | 워치 APK — `:wear` 모듈로 `settings.gradle.kts`에 추가 |
| **언어** | Kotlin 2.0.x (기존과 동일) | 폰 앱과 코드 공유 용이 |
| **UI** | Jetpack Compose for Wear OS (`androidx.wear.compose`) | Wear OS 전용 컴포넌트: `ScalingLazyColumn`, `Scaffold`, `Button` |
| **Material** | `androidx.wear.compose:compose-material` | Wear OS Material 3 디자인 시스템 |
| **Health Services** | `androidx.health.services.client` | ExerciseClient (운동 세션 상태 머신), 심박수, 케이던스 |
| **Data Layer** | `com.google.android.gms:play-services-wearable` | MessageClient (명령), DataClient (상태 동기화) |
| **Hilt** | 워치 모듈에서도 동일하게 사용 | DI 일관성 유지 |
| **공유 모듈** | `:shared` (선택적) | 폰-워치 공통 데이터 모델 (WatchRunCommand, WatchMetricSnapshot) |
| **Retrofit** | MVP에서 제외 | Companion 모드에서는 폰이 모든 API 호출 담당 |
| **Room** | MVP에서 제외 | Companion 모드에서는 폰이 큐 관리 |
| **DataStore** | 워치 연결 상태 설정 저장 | 경량 설정 (단순 키-값) |
| **Tiles / Complications** | W-8에서 추가 | 워치 홈화면 위젯 |

### Gradle 모듈 구조 (예상)

```
RunWay/
├── android/
│   ├── settings.gradle.kts      ← include(":app"), include(":wear"), include(":shared") 추가
│   ├── app/                     ← 기존 폰 앱 모듈 (변경 최소화)
│   ├── wear/                    ← 신규 Wear OS 모듈
│   └── shared/                  ← 신규 공통 데이터 모델 모듈 (선택)
```

> **주의:** `:shared` 모듈은 순수 Kotlin 라이브러리여야 하며, Android 의존성을 포함하면 안 된다. WatchRunCommand, WatchMetricSnapshot 등 데이터 클래스만 포함한다.

---

## 9. Phone-Watch Communication Design

### 메시지 경로 (`MessageClient`)

#### 워치 → 폰 (명령)

```kotlin
// 경로: /runway/watch/command
// 폰에서 WearableListenerService로 수신

// START_RUN에 목표 정보를 포함한 sealed class로 설계
sealed class WatchRunCommand {
    // 목표 없이 자유 런
    object StartFreeRun : WatchRunCommand()

    // 시간 목표 런 (분 단위)
    data class StartTimeGoalRun(val targetMinutes: Int) : WatchRunCommand()

    // 거리 목표 런 (미터 단위)
    data class StartDistanceGoalRun(val targetMeters: Int) : WatchRunCommand()

    // 인터벌 런 (워치 전용 단순화 버전)
    data class StartIntervalRun(
        val workSeconds: Int,        // 운동 구간 (초)
        val restSeconds: Int,        // 휴식 구간 (초)
        val sets: Int,               // 반복 횟수
    ) : WatchRunCommand()

    object PauseRun    : WatchRunCommand()
    object ResumeRun   : WatchRunCommand()
    object FinishRun   : WatchRunCommand()
    object AbandonRun  : WatchRunCommand()
}
```

> **폰 측 처리:** `WearableListenerService`에서 `StartTimeGoalRun(targetMinutes)` 수신 시 폰의 `RunGoal.TimeGoal(targetMinutes)`로 변환해 `RunTrackingManager`에 주입한다. `StartIntervalRun`은 폰의 `RunGoal.IntervalGoal`로 매핑 (warmup=None, cooldown=None, paceTarget=null).

#### 폰 → 워치 (상태)

```kotlin
// 경로: /runway/watch/state
// DataClient.putDataItem() 권장: 연결 끊겨도 재연결 시 자동 최신 상태 동기화

data class WatchMetricSnapshot(
    val isRunning: Boolean,
    val isPaused: Boolean,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val paceMinPerKm: Float,         // 0이면 정지 상태
    val heartRateBpm: Int?,          // null이면 미지원
    val cadenceSpm: Int?,            // 분당 걸음수
    val gpsStatus: String,           // "GOOD" | "POOR" | "SEARCHING"

    // 목표 진행 (null이면 목표 없음)
    val goalType: String?,           // "TIME" | "DISTANCE" | "INTERVAL" | null
    val goalProgressPercent: Int?,   // 0~100
    val goalRemainingLabel: String?, // "08:23 남음" | "2.3km 남음" | "운동 3/5"
    val intervalSegment: String?,    // "WARMUP" | "WORK" | "REST" | "COOLDOWN" | null

    // 코스 도전 (null이면 자유 런)
    val courseProgressPercent: Int?,
    val deviationWarning: Boolean,
    val runId: String?,
)
```

#### 폰 → 워치 (코스 시작 알림)

```kotlin
// 경로: /runway/watch/course-start
data class WatchCourseInfo(
    val courseId: String,
    val courseName: String,
    val distanceMeters: Double,
    val isLoop: Boolean,
)
```

### 메시지 경로 상수 (`:shared` 모듈에 정의)

```kotlin
object WatchPaths {
    const val COMMAND      = "/runway/watch/command"
    const val STATE        = "/runway/watch/state"
    const val COURSE_START = "/runway/watch/course-start"
}
```

### 폰 측 수신 (`WearableListenerService`)

```kotlin
// 기존 app 모듈에 추가
class RunwayWearListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            WatchPaths.COMMAND -> {
                val cmd = deserialize(event.data) as WatchRunCommand
                // RunTrackingService에 Intent action 전달
            }
        }
    }
}
```

### 업데이트 주기

| 데이터 | 방식 | 주기 |
|---|---|---|
| 메트릭(거리/시간/페이스) | `DataClient.putDataItem` | 2초마다 (배터리 절충) |
| 이탈 경고, GPS 상태 변화 | `MessageClient.sendMessage` | 이벤트 발생 시 즉시 |
| 코스 시작 정보 | `MessageClient.sendMessage` | 코스 도전 시작 1회 |

---

## 10. UX Design for Watch

### WatchHomeScreen

**목표:** 런 시작 진입점. 워치 앱을 열면 즉시 선택할 수 있어야 한다.

**레이아웃:**
```
┌─────────────────────────┐
│   ● PathFinder          │
│                         │
│   ▶ 목표 없이 시작       │  ← 큰 원형 버튼 (녹색)
│   ⚑ 목표 설정 후 시작   │  ← 보조 버튼 (작게)
│                         │
│   최근 런: 5.2km        │
│   📱 연결됨             │
└─────────────────────────┘
```

**인터랙션:**
- "목표 없이 시작" → 폰에 `StartFreeRun` 전송 → `WatchRunTrackingScreen` 이동
- "목표 설정 후 시작" → `WatchGoalTypeScreen` 이동
- 폰 연결 안 됨 상태에서 탭 → "폰과 연결하세요" 토스트

**엣지 케이스:**
- 폰이 런 중인 상태로 워치 앱 재실행 → `WatchRunTrackingScreen`으로 자동 이동

---

### WatchGoalTypeScreen

**목표:** 목표 타입 선택 (시간 / 거리 / 인터벌).

**레이아웃:**
```
┌─────────────────────────┐
│      목표 선택           │
│                         │
│   ⏱  시간               │  ← 탭하면 WatchTimeGoalScreen
│   📍 거리               │  ← 탭하면 WatchDistanceGoalScreen
│   🔄 인터벌             │  ← 탭하면 WatchIntervalGoalScreen
└─────────────────────────┘
```

**인터랙션:**
- 각 항목 탭 → 해당 목표 설정 화면으로 이동
- 물리 Back 버튼 → `WatchHomeScreen`으로 복귀

---

### WatchTimeGoalScreen

**목표:** 목표 시간 설정. Wear OS `Picker`(크라운/베젤 입력)로 분 선택.

**레이아웃:**
```
┌─────────────────────────┐
│      시간 목표           │
│                         │
│        ▲                │
│       30 분             │  ← 크라운 회전으로 값 변경
│        ▼                │
│                         │
│   30분 동안 달리기       │  ← 요약 텍스트
│   [   시작하기   ]       │  ← 확인 버튼
└─────────────────────────┘
```

**선택 범위:** 5분 ~ 360분 (5분 단위)
**인터랙션:**
- 크라운/베젤 회전 → 분 값 변경
- "시작하기" 탭 → `StartTimeGoalRun(targetMinutes)` 전송 → `WatchRunTrackingScreen`

---

### WatchDistanceGoalScreen

**목표:** 목표 거리 설정. 0.5km 단위로 선택.

**레이아웃:**
```
┌─────────────────────────┐
│      거리 목표           │
│                         │
│        ▲                │
│      5.0 km             │  ← 크라운 회전으로 값 변경
│        ▼                │
│                         │
│   5.0km 달리기           │
│   [   시작하기   ]       │
└─────────────────────────┘
```

**선택 범위:** 0.5km ~ 50.0km (0.5km 단위)
**주요 프리셋 강조:** 5km, 10km, 21.1km, 42.2km는 굵게 표시
**인터랙션:**
- "시작하기" 탭 → `StartDistanceGoalRun(targetMeters)` 전송

---

### WatchIntervalGoalScreen

**목표:** 인터벌 설정. 폰의 복잡한 4-세그먼트 방식을 워치용으로 단순화. **3단계 순서 화면**.

**Step 1 — 운동 구간:**
```
┌─────────────────────────┐
│   인터벌 (1/3)           │
│   운동 구간              │
│        ▲                │
│       5 분              │
│        ▼                │
│   [      다음      ]    │
└─────────────────────────┘
```

**Step 2 — 휴식 구간:**
```
┌─────────────────────────┐
│   인터벌 (2/3)           │
│   휴식 구간              │
│        ▲                │
│       2 분              │
│        ▼                │
│   [      다음      ]    │
└─────────────────────────┘
```

**Step 3 — 반복 횟수 + 요약:**
```
┌─────────────────────────┐
│   인터벌 (3/3)           │
│   반복 횟수              │
│        ▲                │
│       4 회              │
│        ▼                │
│ 운동5분 / 휴식2분 × 4회  │
│   [   시작하기   ]       │
└─────────────────────────┘
```

**선택 범위:**
- 운동: 1~60분 (1분 단위)
- 휴식: 1~30분 (1분 단위)
- 세트: 1~20회

**인터랙션:**
- 크라운/베젤 회전 → 값 변경
- "다음" → 다음 Step
- 물리 Back → 이전 Step 또는 `WatchGoalTypeScreen`
- Step 3 "시작하기" → `StartIntervalRun(workSeconds, restSeconds, sets)` 전송

**폰 측 매핑 (WorkWearableListenerService):**
```kotlin
WatchRunCommand.StartIntervalRun(workSec, restSec, sets) → RunGoal.IntervalGoal(
    warmup   = IntervalSegment(IntervalDuration.None),
    work     = IntervalSegment(IntervalDuration.ByTime(workSec / 60)),
    recovery = IntervalSegment(IntervalDuration.ByTime(restSec / 60)),
    cooldown = null,
    sets     = sets,
)
```

---

### WatchRunTrackingScreen

**목표:** 런 중 핵심 메트릭을 한눈에 파악하고 최소 조작으로 제어.

**레이아웃 (스와이프 2페이지):**
```
페이지 1 (기본 + 목표 진행)       페이지 2 (보조 메트릭)
┌──────────────────────────┐   ┌─────────────────────┐
│       25:42              │   │  페이스  거리         │
│       (시간)             │   │  5'32"   3.2km       │
│                          │   │  케이던스  심박수      │
│  페이스      거리         │   │  172 spm  148 bpm    │
│  5'32"     3.2km        │   │                     │
│                          │   │  목표 진행률: 64%    │
│ ████████░░░ 64%          │   │  "1.8km 남음"        │
│  "1.8km 남음"            │   │  (코스/목표 시)      │
│ ━━━━━━━━━━━━━━━━━━━━━━  │   │                     │
│  II   ⬛                 │   │                     │
└──────────────────────────┘   └─────────────────────┘

인터벌 모드:
┌──────────────────────────┐
│  운동 중 — 3/5회          │  ← 세그먼트 상태
│       02:34              │  ← 세그먼트 남은 시간 (대형)
│  페이스   거리            │
│  4'55"  2.1km           │
│  ██████████░░ 완주 60%   │  ← 전체 진행바
│  ━━━━━━━━━━━━━━━━━━━━━  │
│   II   ⬛                │
└──────────────────────────┘
```

**인터랙션:**
- 하단 Pause 버튼 탭 → `WatchPausedScreen` 이동 + `PAUSE_RUN` 전송
- 왼쪽 스와이프 → 페이지 2(보조 메트릭)
- 물리 버튼(있는 경우) → Pause/Resume 토글
- 코스 이탈 감지 → 화면 빨간 테두리 + 진동 2회

**엣지 케이스:**
- GPS 신호 약함 → 페이스 표시 대신 "GPS 약함" 표시
- 폰 연결 끊김 → "폰 연결 끊김" 배너 + 마지막 메트릭 유지 표시

---

### WatchPausedScreen

**목표:** 일시정지 상태에서 이어달리기/완료/포기 선택.

**레이아웃:**
```
┌─────────────────────┐
│     일시정지         │
│     25:42 / 3.2km  │
│                     │
│ ▶ 계속하기          │  ← 녹색
│ ■ 런 완료           │  ← 파란색
│ × 포기              │  ← 회색 (작게)
└─────────────────────┘
```

**인터랙션:**
- 계속하기 → `RESUME_RUN` 전송 + `WatchRunTrackingScreen`으로 복귀
- 런 완료 → `FINISH_RUN` 전송 + `WatchRunSummaryScreen`으로 이동
- 포기 → 확인 다이얼로그 → `ABANDON_RUN` 전송 + `WatchHomeScreen`으로 이동

---

### WatchCourseAttemptScreen (WatchRunTrackingScreen의 코스 모드)

**목표:** 코스 도전 중 진행 상황과 이탈 경고를 직관적으로 표시.

**레이아웃:**
```
┌─────────────────────┐
│  [≡] 한강 양재천    │  ← 코스명 (스크롤 가능)
│                     │
│  진행률             │
│  ████████░░ 72%    │  ← 진행바
│  남은 거리: 1.4km  │
│                     │
│ 현재 페이스: 5'21"  │
└─────────────────────┘

이탈 시:
┌─────────────────────┐
│ ⚠ 코스 이탈!       │  ← 빨간 테두리 + 진동
│ 경로로 돌아가세요  │
└─────────────────────┘
```

---

### WatchRunSummaryScreen

**목표:** 런 완료 직후 기본 결과 확인 및 폰 앱으로의 자연스러운 전환.

**레이아웃:**
```
┌─────────────────────┐
│  완료! 🎉           │
│  5.2 km            │  ← 거리 (대형)
│  26분 14초         │
│  페이스 5'03"/km   │
│                     │
│  (PR이면) 🏆 신기록! │
│                     │
│  폰에서 자세히 보기 │
└─────────────────────┘
```

---

## 11. Data and Backend Impact

### MVP (Option C Hybrid): 백엔드 변경 없음

오프라인 수집 후 폰을 통한 동기화 방식에서 폰이 기존 API를 그대로 호출한다. 워치는 서버에 직접 접근하지 않는다.

| API | 호출 주체 | 변경 여부 |
|---|---|---|
| `POST /api/runs/start` | 폰 (워치 명령 수신 후) | 없음 |
| `POST /api/runs/{runId}/points` | 폰 (배치 전송) | 없음 |
| `POST /api/runs/{runId}/pause` | 폰 | 없음 |
| `POST /api/runs/{runId}/resume` | 폰 | 없음 |
| `POST /api/runs/{runId}/finish` | 폰 | 없음 |
| `POST /api/courses/{courseId}/attempts/start` | 폰 | 없음 |
| `POST /api/course-attempts/{attemptId}/finish` | 폰 | 없음 |

### 나중 Standalone 모드 (Option C 확장): 백엔드 변경 필요

| 변경 사항 | 내용 |
|---|---|
| 워치용 JWT 토큰 공유 | DataClient로 폰에서 워치로 Access Token 전달 (보안 주의) |
| `POST /api/runs/start` 워치 직접 호출 | 동일 엔드포인트, 추가 변경 없음 |
| `POST /api/runs/{runId}/points` 워치 직접 호출 | 동일 엔드포인트, 추가 변경 없음 |
| 오프라인 큐 워치 측 구현 | Room 또는 DataStore 기반 경량 큐 |

---

## 12. Battery and Reliability Plan

### 배터리 비용 분석 (Option C — Hybrid Standalone 기준)

| 소스 | 소비 주체 | 영향 |
|---|---|---|
| **GPS (Health Services ExerciseClient)** | **워치** | 주요 배터리 소모. 3초 간격 수집. |
| **심박수 센서 (PPG)** | **워치** | Health Services가 OS 수준에서 배치 최적화 |
| **케이던스 (가속도계)** | **워치** | Health Services가 함께 처리 — 추가 부하 적음 |
| Bluetooth Data Layer (폰 연결 시) | 폰 + 워치 | 2초 간격 DataItem 동기화 — 경량 |
| Always-On Display | 워치 | MVP에서 비활성 기본값 권장 |
| 워치 로컬 스토리지 쓰기 | 워치 | 오프라인 중 RunPoint를 DataStore 또는 Room에 버퍼링 |

**배터리 예상 수명 (Health Services GPS 연속 사용):**
- Pixel Watch 3 기준: GPS 모드 약 12~16시간
- Galaxy Watch 7 기준: GPS 모드 약 18~22시간
- 일반 러닝(1~2시간)에는 충분, 마라톤(4시간+)은 AOD 끄고 사용 권장

**권장 Health Services 설정:**
```kotlin
// ExerciseConfig — 필요한 DataType만 요청해 배터리 절약
ExerciseConfig.builder()
    .setExerciseType(ExerciseType.RUNNING)
    .setDataTypes(setOf(
        DataType.HEART_RATE_BPM,      // 심박수
        DataType.LOCATION,            // GPS
        DataType.DISTANCE,            // 누적 거리 (Health Services가 계산)
        DataType.SPEED,               // 속도/페이스
        DataType.STEPS_PER_MINUTE,    // 케이던스
    ))
    .build()
```

### 오프라인 데이터 저장 전략

```
런 중 (워치 로컬 버퍼):
GPS포인트 → WatchRunPointEntity (Room 또는 DataStore)
             { sequence, lat, lng, altitude, timestamp, heartRate, cadence }

재연결 시 (폰으로 전송):
ChannelClient 또는 DataClient → WatchRunData (전체 run 데이터)
→ 폰 WearableListenerService에서 수신
→ 폰 PendingPointQueue에 주입
→ 기존 배치 업로드 로직으로 서버 전송
```

### 신뢰성 시나리오

| 시나리오 | 동작 |
|---|---|
| **폰 없이 런 시작** | 워치 단독으로 Health Services 수집 + 로컬 저장. 런 시작 API(POST /api/runs/start)는 폰 재연결 시 호출하거나, 폰 없이는 런 완료 후 한 번에 업로드. |
| **런 중 폰 연결 끊김** | 워치는 수집 중단 없이 계속 로컬 저장. 워치 화면에 "오프라인 저장 중" 표시. |
| **런 완료 후 폰 연결** | 워치가 런 데이터 전송 시작. 폰이 서버에 run 생성 + 포인트 배치 업로드. 완료 후 워치에서 요약 표시. |
| **워치 앱 화면 닫힘 (런 중)** | ForegroundService가 Health Services 세션 유지. 화면 재진입 시 진행 중 상태 복원. |
| **워치 크래시 (런 중)** | Health Services 세션이 살아있으면 데이터 계속 버퍼링. 앱 재시작 시 세션 재연결. |
| **워치 배터리 5% 이하** | 로컬 저장된 데이터를 즉시 폰으로 전송 시도. 실패 시 로컬 보존. |
| **폰 없이 런 후 폰 앱 없음** | 로컬 RunPoint 유지. 다음 폰 연결 시 자동 동기화. 최대 7일치 보관 (용량 제한). |

---

## 13. Permission Plan

### 워치 모듈 (`wear/AndroidManifest.xml`)

```xml
<!-- GPS — W-3부터 필수 (Standalone 수집) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- 심박수 센서 — W-3부터 -->
<uses-permission android:name="android.permission.BODY_SENSORS" />

<!-- ExerciseClient 필수 -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />

<!-- 진동 피드백 -->
<uses-permission android:name="android.permission.VIBRATE" />

<!-- 포그라운드 서비스 — 런 세션 중 Health Services 유지 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
```

### 폰 모듈에 추가 (기존 모듈)

```xml
<!-- WearableListenerService 등록 — 추가 권한 불필요 -->
<!-- play-services-wearable 의존성만 추가하면 됨 -->
```

### 런타임 권한 요청 시점

| 권한 | 요청 시점 | Phase |
|---|---|---|
| `ACCESS_FINE_LOCATION` | 첫 런 시작 전 | W-3 |
| `BODY_SENSORS` | 첫 런 시작 전 | W-3 |
| `ACTIVITY_RECOGNITION` | 첫 런 시작 전 | W-3 |

---

## 14. Implementation Roadmap

**아키텍처 방향:** Option C (Hybrid Standalone). 워치가 항상 Health Services로 독립 수집. 폰은 업로드 채널.

| Phase | 목표 | 범위 | 변경될 파일 | 위험도 | 검수 기준 |
|---|---|---|---|---|---|
| **W-1** | Wear OS 모듈 설정 + WatchHomeScreen | Gradle 멀티모듈, `WatchHomeScreen` (목표 없이/목표 설정 2버튼), 폰 연결 상태 감지 | `settings.gradle.kts`, `wear/build.gradle.kts`, `wear/.../WatchHomeScreen.kt` | **높음** | `./gradlew :wear:assembleDebug` 성공. `:app` 빌드 영향 없음. |
| **W-2** | 워치 목표 설정 화면 | `WatchGoalTypeScreen`, `WatchTimeGoalScreen`, `WatchDistanceGoalScreen`, `WatchIntervalGoalScreen` (3단계) | `wear/.../WatchGoalTypeScreen.kt`, `wear/.../WatchTimeGoalScreen.kt`, `wear/.../WatchDistanceGoalScreen.kt`, `wear/.../WatchIntervalGoalScreen.kt` | 낮음 | 크라운 회전으로 값 변경. 인터벌 3단계 완료. |
| **W-3** | Health Services + 오프라인 GPS 수집 | ExerciseClient (GPS, 심박수, 케이던스, 페이스), `WatchRunPointBuffer` (로컬 저장), ForegroundService `HEALTH` | `wear/.../WatchExerciseManager.kt` (신규), `wear/.../WatchRunPointBuffer.kt` (신규), `wear/AndroidManifest.xml` | **높음** | 폰 없이 워치 단독으로 GPS + 심박수 수집. 로컬 저장 확인. |
| **W-4** | 워치 런 추적 화면 + 목표 진행 | `WatchRunTrackingScreen` (목표 진행바 포함), `WatchPausedScreen`, `WatchRunSummaryScreen` | `wear/.../WatchRunTrackingScreen.kt`, `wear/.../WatchPausedScreen.kt`, `wear/.../WatchRunSummaryScreen.kt`, `wear/.../WatchGoalEngine.kt` (신규) | 중간 | 오프라인 런 중 거리·페이스·심박수·목표 진행 표시. 인터벌 세그먼트 전환 진동. |
| **W-5** | 폰-워치 데이터 동기화 | `WearableListenerService` (폰), `ChannelClient` 전송, 폰 `PendingPointQueue` 주입, 서버 업로드 | `app/.../RunwayWearListenerService.kt` (신규), `shared/.../WatchRunData.kt` (신규), `wear/.../WatchSyncManager.kt` (신규) | **높음** | 오프라인 런 완료 → 폰 연결 → 서버에 런 기록 + 포인트 자동 업로드 확인. |
| **W-6** | Pause/Resume/Finish + 요약 화면 | 워치 Pause → ExerciseClient.pauseExercise, Finish → 폰에 결과 전송 → 서버 finish API 호출 | `wear/.../WatchExerciseManager.kt`, `app/.../RunwayWearListenerService.kt`, `wear/.../WatchRunSummaryScreen.kt` | 낮음 | Finish → 서버에 런 완료 기록. 워치 요약 화면 표시. |
| **W-7** | 코스 도전 Companion 모드 | 코스 시작 시 `WatchCourseInfo` 전송, 이탈 경고 진동, 코스 진행률 표시 | `app/.../CourseAttemptTrackingViewModel.kt`, `wear/.../WatchRunTrackingScreen.kt` | 낮음 | 코스 도전 중 진행률 표시. 이탈 진동 확인. |
| **W-8** | Tiles / Complications | `TileService`, `ComplicationDataSourceService`, 빠른 런 시작 + 최근 목표 재실행 | `wear/.../RunwayTileService.kt`, `wear/.../RunwayComplicationService.kt` | 낮음 | Tile에서 런 시작 확인. |

---

## 15. Recommended First Phase

### W-1: Wear OS 모듈 설정 및 WatchHomeScreen

**브랜치:** `feature/wear-os-module-setup`

**목표:**
- `:wear` Gradle 모듈을 안드로이드 프로젝트에 추가
- 기존 `:app` 모듈 빌드에 영향을 주지 않는 독립 모듈로 설정
- 워치 에뮬레이터에서 `WatchHomeScreen` 표시 (런 시작 버튼, 연결 상태)
- 폰 연결 여부를 감지하는 기초 Data Layer 연결

**변경될 파일:**
- `android/settings.gradle.kts` — `include(":wear")` 추가
- `android/wear/build.gradle.kts` — 신규 (Wear OS application 모듈)
- `android/wear/src/main/AndroidManifest.xml` — 신규
- `android/wear/src/main/java/.../MainActivity.kt` — 신규
- `android/wear/src/main/java/.../WatchHomeScreen.kt` — 신규
- `android/wear/src/main/java/.../WatchAppTheme.kt` — 신규

**빌드 명령어:**
```bash
# 폰 앱 기존 빌드 확인 (regression 없음)
./gradlew :app:assembleDebug

# 워치 모듈 빌드
./gradlew :wear:assembleDebug

# 전체 빌드
./gradlew assembleDebug
```

**검수 기준:**
1. `./gradlew :app:assembleDebug` — 기존 폰 앱 빌드 성공 (W-1 이후에도 동일)
2. `./gradlew :wear:assembleDebug` — 워치 모듈 빌드 성공
3. Wear OS 에뮬레이터에서 앱 실행 → `WatchHomeScreen` 표시 확인
4. 폰 에뮬레이터와 쌍을 이룬 상태에서 "폰 연결됨" 상태 표시 확인
5. 폰 에뮬레이터 연결 해제 후 "폰 연결 안 됨" 상태 표시 확인

**커밋 메시지:**
```
feat(wear): add Wear OS module and WatchHomeScreen

- Add :wear Gradle module with Wear OS application configuration
- Implement WatchHomeScreen with run start button and phone connection status
- Add basic Data Layer connection detection
- Existing :app module build is unaffected
```

---

## 16. Implementation Prompt for W-1

아래는 W-1 구현을 위한 Claude Code 프롬프트다.

---

```
Task: Implement W-1 — Add Wear OS module and WatchHomeScreen for RunWay Android project.

Branch: feature/wear-os-module-setup
Base branch: main

## Goal
Add a new :wear Gradle module to the existing RunWay Android project.
The module should contain a minimal Wear OS app with:
1. WatchHomeScreen — shows a "런 시작" (Start Run) button and phone connection status
2. Basic phone connection detection via Data Layer API (CapabilityClient)
3. The existing :app module build must NOT be broken

## Out of Scope
- Do NOT implement actual run tracking logic
- Do NOT add MessageClient command sending yet (that is W-2)
- Do NOT add Retrofit, Room, or backend API calls
- Do NOT add GPS permissions or Health Services
- Do NOT implement standalone GPS tracking
- Do NOT modify existing :app module source files (only settings.gradle.kts)
- Do NOT require external API keys

## Project Context
- android/settings.gradle.kts currently: rootProject.name = "RunWay", include(":app")
- android/app/build.gradle.kts: compileSdk = 35, minSdk = 26, Kotlin 2.0.x, Hilt 2.52, Compose BOM 2024.10.01
- Package: com.runway.android (phone). Use com.runway.wear for the watch module.
- The project uses Hilt for DI. If Hilt in :wear is complex, use manual injection for W-1.

## Files to Create or Modify

### android/settings.gradle.kts
Add: include(":wear")

### android/wear/build.gradle.kts (NEW)
- Plugin: com.android.application
- Plugin: org.jetbrains.kotlin.android
- Plugin: org.jetbrains.kotlin.plugin.compose
- namespace: com.runway.wear
- compileSdk: 35
- minSdk: 30 (Wear OS 3 minimum)
- targetSdk: 35
- Dependencies:
  - androidx.wear.compose:compose-material3:1.0.0-alpha26 (or latest stable)
  - androidx.wear.compose:compose-foundation:1.4.1 (or latest stable)
  - androidx.activity:activity-compose:1.9.3
  - androidx.core:core-ktx:1.15.0
  - com.google.android.gms:play-services-wearable:18.2.0
  - androidx.compose:compose-bom:2024.10.01 (same BOM as :app)
  - androidx.wear:wear:1.3.0

### android/wear/src/main/AndroidManifest.xml (NEW)
- uses-feature: android.hardware.type.watch (required=true)
- MainActivity as launcher
- VIBRATE permission

### android/wear/src/main/.../MainActivity.kt (NEW)
- ComponentActivity
- Sets content to WatchApp() composable

### android/wear/src/main/.../WatchApp.kt (NEW)
- Root composable with Wear OS Scaffold
- ThemeWrapper around WatchHomeScreen

### android/wear/src/main/.../WatchHomeScreen.kt (NEW)
- Composable showing:
  - App name "PathFinder" at top
  - Large circular Button "런 시작" (disabled if phone not connected)
  - Phone connection status text: "📱 연결됨" or "📱 연결 안 됨"
  - Last run placeholder: "최근 런 없음" (static, no real data in W-1)
- Uses Wear Compose components: Button, Text, ScalingLazyColumn or Column

### android/wear/src/main/.../PhoneConnectionState.kt (NEW)
- Simple data class or sealed class: Connected / Disconnected
- ViewModel or simple StateFlow wrapper that uses CapabilityClient to check
  if the phone-side app node is available
- Node capability name: "runway_phone_app"

## Build Commands
./gradlew :app:assembleDebug    # must still pass
./gradlew :wear:assembleDebug   # new module must build
./gradlew assembleDebug         # both modules

## Commit Rule
Single commit after all files are created and both builds pass.
Message format:
feat(wear): add Wear OS module and WatchHomeScreen

## Report Format
After implementation, report:
1. Files created or modified (with full paths)
2. Build result for :app:assembleDebug
3. Build result for :wear:assembleDebug
4. Screenshot description of WatchHomeScreen on emulator (if possible)
5. Any risks or known issues found during implementation
6. git status output

## Constraints
- Do not break the existing phone app
- Keep WatchHomeScreen simple — no animations, no complex layouts
- If Wear Compose dependency versions conflict, document the exact version used
- If Hilt integration in :wear causes build errors, skip Hilt for W-1 and note it
- Prefer using the latest stable Wear Compose version, not alpha unless necessary
```

---

*이 문서는 구현 시작 전 승인이 필요한 결정 사항들이 있다. Section 17 참고.*

---

## 17. Decisions Needed from User

구현 시작 전 결정이 필요한 사항:

| 번호 | 결정 사항 | 옵션 A | 옵션 B | 권장 |
|---|---|---|---|---|
| D-1 | Wear OS 최소 버전 | Wear OS 3 (minSdk 30) | Wear OS 2 (minSdk 26) | **Wear OS 3** — Health Services, Compose 풀 지원 |
| D-2 | `:shared` 공통 모듈 생성 여부 | 생성 (WatchRunCommand 등 공유) | :app과 :wear에 각각 복사 | **`:shared` 생성** — 장기적으로 유지보수 용이 |
| D-3 | Hilt 워치 모듈 적용 | W-1부터 Hilt 사용 | W-1은 수동 DI, 나중에 추가 | **W-1은 수동 DI** — Gradle 복잡도 낮춤 |
| D-4 | 심박수 포함 시점 | W-3 (GPS와 함께) | 별도 Phase | **W-3에 포함** — Health Services ExerciseConfig에서 동일 설정으로 함께 수집 가능 |
| D-5 | 폰-워치 메트릭 업데이트 주기 | 1초 (배터리 소모 높음) | 2초 (균형) | **2초** — GPS가 3초 간격이므로 충분 |
| D-6 | 목표 Wear OS 기기 | Galaxy Watch (One UI Watch) | 범용 Wear OS 3 기기 | **범용 Wear OS 3** — Galaxy Watch도 포함 |
```
