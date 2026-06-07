# PathFinder — 아키텍처 다이어그램 가이드 (draw.io)

> 작성일: 2026-06-07  
> 기준: main 브랜치 최신 구현 상태  
> 도구: draw.io (diagrams.net)

이 문서는 draw.io에서 PathFinder 아키텍처를 그릴 때 사용하는 **박스 레이블·화살표 레이블·색상·배치** 기준서다. 총 5개 다이어그램으로 구성된다.

---

## 공통 스타일 기준

| 항목 | 설정값 |
|------|--------|
| 기본 폰트 | `Noto Sans KR` |
| 코드·경로 | `JetBrains Mono` |
| 박스 모서리 radius | `8px` |
| 화살표 | 곡선(`curved`) + `open` 끝 |
| 점선 화살표 | 선택적 관계·비동기 통신 |
| 그림자 | 주요 컴포넌트 박스에만 `dropShadow=1` |
| 레이어 컨테이너 | `container=1` (자식 박스 포함) |

### 색상 팔레트

| 의미 | 색상 코드 | 사용 위치 |
|------|----------|----------|
| Android UI | `#DBEAFE` | Android 앱·UI 박스 |
| Android 내부 | `#EFF6FF` | ViewModel·Repository 서브 박스 |
| Wear OS | `#EDE9FE` | 워치 연동 박스 |
| Wear OS 배경 | `#F5F3FF` | Wear 컨테이너 |
| Core 인프라 | `#D1FAE5` | ForegroundService·VoiceGuide |
| Core 배경 | `#ECFDF5` | Core 컨테이너 |
| 서버 | `#FFEDD5` | Controller·Service·Repository |
| 서버 배경 | `#FFF7ED` | Spring Boot 컨테이너 |
| 데이터베이스 | `#F0FDF4` | PostgreSQL·Room |
| 음성 안내 | `#FEF9C3` | VoiceGuide 이벤트 박스 |
| CI/CD | `#F1F5F9` | GitHub Actions |
| 경고·강조 | `#FEF3C7` | 자동일시정지·면책 |

---

## 다이어그램 1 — 전체 시스템 아키텍처

**제목:** `PathFinder — 전체 시스템 아키텍처`  
**방향:** Top → Bottom  
**캔버스:** 1400 × 900

### 박스 목록

| ID | 레이블 | 형태 | 색상 |
|----|--------|------|------|
| A | `Android 앱` | 컨테이너 (둥근 사각형) | `#DBEAFE` |
| A1 | `Jetpack Compose UI\n28개 화면` | 사각형 | `#EFF6FF` |
| A2 | `ViewModel + StateFlow\n상태 관리` | 사각형 | `#EFF6FF` |
| A3 | `Repository (Domain)\n인터페이스 계층` | 사각형 | `#EFF6FF` |
| A4 | `Room DB\nPendingPointQueue` | 원통형 | `#EFF6FF` |
| A5 | `DataStore\nJWT 토큰 저장` | 원통형 | `#EFF6FF` |
| B | `Wear OS 연동` | 컨테이너 (점선 테두리) | `#F5F3FF` |
| B1 | `WatchCommandListenerService\n워치 명령 수신` | 사각형 | `#EDE9FE` |
| B2 | `WearRunSessionCoordinator\nGPS · 배치업로드 · 음성안내` | 사각형 | `#EDE9FE` |
| B3 | `WatchStateSender\n상태 → 워치 전송` | 사각형 | `#EDE9FE` |
| C | `핵심 인프라 (Core)` | 컨테이너 | `#ECFDF5` |
| C1 | `RunTrackingService\nForegroundService` | 사각형 | `#D1FAE5` |
| C2 | `RunTrackingManager\nGPS 수집 · Auto-pause` | 사각형 | `#D1FAE5` |
| C3 | `RunningVoiceGuide\nTTS 음성 안내` | 사각형 | `#D1FAE5` |
| C4 | `MediaPipe 자세 분석\nOn-Device AI` | 사각형 | `#D1FAE5` |
| D | `Spring Boot API 서버\nOracle Cloud — 40.233.98.156` | 컨테이너 | `#FFF7ED` |
| D1 | `Controller\n(REST Endpoints)` | 사각형 | `#FFEDD5` |
| D2 | `Service\n(Business Logic)` | 사각형 | `#FFEDD5` |
| D3 | `Repository\n(JPA + Native Query)` | 사각형 | `#FFEDD5` |
| D4 | `Spring Security\nJWT Filter` | 사각형 | `#FED7AA` |
| E | `PostgreSQL 15 + PostGIS 3.4\nDocker Container` | 원통형 | `#F0FDF4` |
| F | `GitHub Actions CI/CD\ngradlew build → SCP → restart` | 마름모 | `#F1F5F9` |

### 화살표 목록

| 출발 | 도착 | 레이블 | 스타일 |
|------|------|--------|--------|
| A (Android) | D (API 서버) | `HTTPS REST API\nBearer {accessToken}` | 실선 →, `#3B82F6` |
| B1 (CommandListener) | B2 (Coordinator) | `WatchCommand\n(StartRun · Pause · Finish)` | 실선 → |
| B2 (Coordinator) | C2 (RunTrackingManager) | `GPS 추적 제어` | 실선 → |
| B2 (Coordinator) | C3 (VoiceGuide) | `음성 안내 호출` | 실선 →, `#7C3AED` |
| B3 (StateSender) | A (Android) | `DataClient\n런 상태 실시간 전송` | 점선 ←, `#7C3AED` |
| C1 (ForegroundService) | C2 (RunTrackingManager) | `수명 주기 관리` | 실선 → |
| C2 (RunTrackingManager) | A4 (Room) | `GPS 포인트 버퍼` | 실선 → |
| A4 (Room) | D (API 서버) | `5초 배치 업로드\nPOST /api/runs/{id}/points` | 점선 →, `#EA580C` |
| D3 (Repository) | E (DB) | `JPA / JDBC\nHikariCP` | 실선 → |
| F (CI/CD) | D (API 서버) | `main 브랜치 push\n→ 자동 배포` | 점선 →, `#6B7280` |

---

## 다이어그램 2 — Android Clean Architecture

**제목:** `Android — Clean Architecture & MVVM`  
**방향:** Left → Right  
**캔버스:** 1600 × 700

### 4개 수직 레이어

#### UI Layer (맨 왼쪽, `#DBEAFE`)

```
레이블: UI Layer
─────────────────────────────
Composable 화면 (28개)

HomeScreen
RunningTrackingScreen
CourseDetailScreen
DiscoverScreen
PostureResultScreen … 등

─────────────────────────────
ViewModel
• StateFlow 상태 보유
• 이벤트 → Repository 호출
• hiltViewModel()로 주입
```

#### Domain Layer (중앙 왼쪽, `#F5F3FF`)

```
레이블: Domain Layer
─────────────────────────────
Repository 인터페이스
(구현 없음 — 계약만)

AuthRepository
RunningRepository
CourseRepository
CourseAttemptRepository
UserRepository

─────────────────────────────
⚠ 이 계층은 Android·서버
  의존성이 없다
```

#### Data Layer (중앙 오른쪽, `#FFF7ED`)

```
레이블: Data Layer
─────────────────────────────
RepositoryImpl
(인터페이스 구현)

[Remote — Retrofit]
  AuthApi
  RunningApi
  CourseApi
  CourseAttemptApi

[Local — Room / DataStore]
  PendingPointQueue
  TrackingSessionStore
  PostureAnalysisDao
```

#### Core Layer (맨 오른쪽, `#ECFDF5`)

```
레이블: Core Layer
(도메인 무관 인프라)
─────────────────────────────
tracking/
  RunTrackingManager
  RunTrackingService
  PendingPointQueue

voice/
  RunningVoiceGuide (TTS)

posture/
  MediaPipe Pipeline
  PostureEvaluator

location/
  FusedLocationProvider
  GpsPointValidator

wear/
  WearRunSessionCoordinator
  WatchStateSender

network/
  TokenAuthenticator (JWT)
  AuthInterceptor
```

#### 하단 공통 박스 — Hilt DI (`#F8FAFC`, 전체 너비)

```
레이블: Hilt DI Modules
NetworkModule · RepositoryModule · LocationModule · PostureModule
```

### 화살표 목록

| 출발 | 도착 | 레이블 | 규칙 |
|------|------|--------|------|
| UI Layer | Domain Layer | `호출` | 실선 →, 단방향 |
| Domain Layer | Data Layer | `구현 바인딩 (Hilt)` | 실선 →, 단방향 |
| Data Layer | 외부 API | `Retrofit HTTPS` | 실선 →, 단방향 |
| Data Layer | Local DB | `Room / DataStore` | 실선 ↔ |
| UI Layer | Core Layer | `직접 의존\n(Tracking · Voice)` | 점선 → |
| Core Layer | Data Layer | `Repository 사용` | 점선 → |

### 우하단 원칙 텍스트 박스 (`#F8FAFC`)

```
의존 방향 규칙
─────────────
UI → Domain ← Data
안쪽 계층은 바깥쪽을 모른다.
Core는 도메인과 무관하게 독립.
```

---

## 다이어그램 3 — GPS 러닝 추적 + 음성 안내 흐름

**제목:** `GPS 러닝 추적 — 데이터 흐름 · 음성 안내 · 크래시 복구`  
**방향:** Top → Bottom  
**캔버스:** 1200 × 1200

### 노드 배치 (위→아래)

#### Row 1 — 진입점 (2개 나란히)

| 박스 | 레이블 | 색상 |
|------|--------|------|
| START_PHONE | `📱 폰에서 시작\nRunningTrackingScreen\n또는 CourseAttemptTrackingScreen` | `#DBEAFE` |
| START_WATCH | `⌚ 워치에서 시작\nWatchCommandListenerService` | `#EDE9FE` |

#### Row 2 — 공통 처리

| 박스 | 레이블 | 색상 |
|------|--------|------|
| COORDINATOR | `WearRunSessionCoordinator\n또는 RunningTrackingViewModel` | `#F0FDF4` |
| VOICE_START | `🔊 "러닝을 시작합니다.\n오늘도 힘차게 달려볼까요?"` | `#FEF9C3` |

> COORDINATOR 오른쪽에 VOICE_START 배치. COORDINATOR → VOICE_START 화살표: `voiceGuide.start()`

#### Row 3 — ForegroundService

| 박스 | 레이블 | 색상 |
|------|--------|------|
| SERVICE | `RunTrackingService\nForegroundService\n알림 표시 (Android 8+)` | `#ECFDF5` |

#### Row 4 — 핵심 추적 루프 (큰 컨테이너 박스, `#F0FDF4`)

```
레이블: RunTrackingManager (Singleton · appScope)
─────────────────────────────────────────────────
[FusedLocationProvider]        [CadenceTracker]
  3초 간격 GPS 수집              폰 가속도계 기반
  PRIORITY_HIGH_ACCURACY         분당 걸음수 측정
         ↓
[GpsPointValidator]
  ❌ 속도 > 12 m/s  → 제거
  ❌ 거리 > 200 m   → 제거
  ❌ 이동 < 1 m     → 제거
         ↓
[Auto-pause 판단]
  속도 < 0.5 m/s × 10회 → 일시정지
  속도 > 1.0 m/s × 3회  → 재개
```

#### Row 5 — 음성 안내 이벤트 (오른쪽에 세로 배치, `#FEF9C3`)

| 박스 | 레이블 | 연결 트리거 |
|------|--------|------------|
| VOICE_PAUSE | `🔊 "속도가 줄어\n자동으로 일시정지합니다."` | isAutoPaused 전환 감지 (true) |
| VOICE_RESUME | `🔊 "다시 달리기\n시작했습니다."` | isAutoPaused 전환 감지 (false) |
| VOICE_KM | `🔊 "N킬로미터 완료.\n시간 X분 Y초.\n평균 페이스 A분 B초."` | milestoneFlow emit (1km마다) |

#### Row 6 — 코스 도전 전용 (점선 박스, `#FEF3C7`)

| 박스 | 레이블 |
|------|--------|
| VOICE_OFFCOURSE | `🔊 "코스를 이탈했습니다.\n안전하게 코스로 돌아와 주세요."` |
| VOICE_ONCOURSE | `🔊 "코스로 복귀했습니다.\n러닝을 계속합니다."` |
| VOICE_90PCT | `🔊 "코스의 90퍼센트를 완주했습니다.\n조금만 더 힘내세요."` |

#### Row 7 — 저장 (2개 병렬)

| 박스 | 레이블 | 색상 |
|------|--------|------|
| QUEUE | `PendingPointQueue\n(Room DB)\n크래시 안전 GPS 큐` | `#F0FDF4` |
| SESSION | `TrackingSessionStore\n(DataStore)\nrunId · 거리 · 시간 실시간 저장` | `#F0FDF4` |

#### Row 8 — 배치 업로드

| 박스 | 레이블 | 색상 |
|------|--------|------|
| UPLOAD | `5초마다 배치 전송\nPOST /api/runs/{id}/points\n(최대 50개 묶음)` | `#FFF7ED` |
| RETRY | `네트워크 실패 시\n큐 보관 → 자동 재시도` | `#FEF3C7` |

#### Row 9 — 완료

| 박스 | 레이블 | 색상 |
|------|--------|------|
| FINISH | `런 완료\nPOST /api/runs/{id}/finish` | `#DBEAFE` |
| VOICE_FINISH | `🔊 "러닝을 종료합니다.\n수고하셨습니다."` | `#FEF9C3` |

#### Row 10 — 크래시 복구 (점선 컨테이너 박스, `#F8FAFC`)

```
레이블: 크래시 복구 (Crash Recovery)
─────────────────────────────────────
앱 강제 종료 → 재실행

TrackingSessionStore에
runId · 거리 · 시간 남아있음
         ↓
TrackingRecoveryDialog
"달리던 런을 이어하시겠습니까?"
  [이어달리기]    [종료]
```

### 화살표 목록

| 출발 | 도착 | 레이블 |
|------|------|--------|
| START_PHONE | COORDINATOR | `ViewModel 생성` |
| START_WATCH | COORDINATOR | `WatchCommand 전달` |
| COORDINATOR | VOICE_START | `voiceGuide.start()` |
| COORDINATOR | SERVICE | `startForegroundService()` |
| SERVICE | RunTrackingManager | `수명 주기 관리` |
| Auto-pause ON | VOICE_PAUSE | `isAutoPaused 전환 (true)` |
| Auto-pause OFF | VOICE_RESUME | `isAutoPaused 전환 (false)` |
| 1km 통과 | VOICE_KM | `milestoneFlow emit` |
| 코스 이탈 | VOICE_OFFCOURSE | `이탈 거리 초과` |
| 코스 복귀 | VOICE_ONCOURSE | `이탈 해제` |
| 90% 지점 | VOICE_90PCT | `진행률 ≥ 90%` |
| RunTrackingManager | QUEUE | `GPS 포인트 누적` |
| RunTrackingManager | SESSION | `5초마다 스냅샷 저장` |
| QUEUE | UPLOAD | `dequeue(50)` |
| UPLOAD | RETRY | `NetworkError 시` |
| FINISH | VOICE_FINISH | `voiceGuide.finish()` |
| SESSION | 크래시복구 | `앱 재시작 시 감지` |

---

## 다이어그램 4 — Wear OS 연동 흐름

**제목:** `Wear OS 연동 — 워치 ↔ 폰 ↔ 서버 통신`  
**방향:** Left → Right  
**캔버스:** 1400 × 650

### 3개 컬럼

#### Column 1 — 워치 (`#F5F3FF`, 왼쪽)

```
레이블: ⌚ Wear OS 앱 (개발 예정)
─────────────────────────────
WatchHomeScreen
WatchGoalTypeScreen
  ├ WatchTimeGoalScreen
  ├ WatchDistanceGoalScreen
  └ WatchIntervalGoalScreen
WatchRunTrackingScreen
WatchPausedScreen
WatchRunSummaryScreen
```

#### Column 2 — 폰 핵심 (`#ECFDF5`, 중앙)

```
레이블: 📱 Android 앱 — Wear 연동 레이어
─────────────────────────────────────
WatchCommandListenerService
(WearableListenerService 상속)
         ↓
WearRunSessionCoordinator
  ├ RunTrackingManager   (GPS 수집)
  ├ PendingPointQueue    (크래시 안전 큐)
  ├ RunningVoiceGuide 🔊 (TTS 음성 안내)
  └ WatchStateSender     (상태 → 워치)
```

#### Column 3 — 서버 (`#FFF7ED`, 오른쪽)

```
레이블: Spring Boot API
─────────────────────────
POST /api/runs/start
POST /api/runs/{id}/points
POST /api/runs/{id}/pause
POST /api/runs/{id}/resume
POST /api/runs/{id}/finish
POST /api/runs/{id}/abandon
```

### 화살표 목록

| 출발 | 도착 | 레이블 | 색상 |
|------|------|--------|------|
| 워치 | WatchCommandListenerService | `MessageClient\n/runway/watch/command` | `#7C3AED` → |
| WearRunSessionCoordinator | Spring Boot API | `HTTPS REST API\n(JWT 자동 첨부)` | `#EA580C` → |
| WatchStateSender | 워치 | `DataClient\n/runway/watch/state\n거리 · 시간 · 페이스 · 상태` | `#7C3AED` ← |
| RunningVoiceGuide | 스피커 아이콘 | `TTS 출력\n폰 스피커 또는 BT 이어폰` | `#16A34A` → |

### 하단 — 워치 명령 매핑 테이블 박스 (`#F8FAFC`)

```
레이블: 워치 명령 → 폰 동작 매핑
──────────────────────────────────────────────────────
StartFreeRun              GPS 시작, POST /runs/start      🔊 러닝을 시작합니다…
StartTimeGoalRun(N분)     GPS + 시간 목표 설정             🔊 러닝을 시작합니다…
StartDistanceGoalRun(Nm)  GPS + 거리 목표 설정             🔊 러닝을 시작합니다…
StartIntervalRun(운동/휴식/세트)  GPS + 인터벌 목표 설정    🔊 러닝을 시작합니다…
PauseRun                  manager.pause() + POST /pause   —
ResumeRun                 manager.resume() + POST /resume —
FinishRun                 배치 flush + POST /finish        🔊 러닝을 종료합니다…
AbandonRun                POST /abandon + 세션 정리        —
```

---

## 다이어그램 5 — AI 자세 분석 파이프라인

**제목:** `AI 러닝 자세 분석 — On-Device MediaPipe 파이프라인`  
**방향:** Top → Bottom  
**캔버스:** 900 × 1300

### Step 박스 (순서대로)

#### 입력 (`#F8FAFC`)

```
레이블: 📹 영상 입력
─────────────────────────────
카메라 촬영  or  갤러리 선택
CameraX (PostureCaptureScreen)
```

#### Step 1 (`#DBEAFE`)

```
레이블: Step 1 — 프레임 추출
MediaMetadataRetriever
─────────────────────────────
• 초당 15프레임 추출
• 최대 300프레임 (약 50초)
• 최대 640px 리사이즈 (비율 유지)
```

#### Step 2 (`#EDE9FE`)

```
레이블: Step 2 — 포즈 감지
MediaPipe PoseLandmarker
─────────────────────────────
• RunningMode: VIDEO
• 33개 관절 랜드마크 추출
• 칼만 필터 프레임 간 추적
• visibility < 0.30 랜드마크 제외
```

#### Step 3 (`#F0FDF4`)

```
레이블: Step 3 — 각도 계산
PostureAngleCalculator
─────────────────────────────
• Aspect Ratio 보정 (세로 영상 왜곡 교정)
• 좌우 독립 계산 → 신뢰도 높은 쪽 선택

측정 항목 (6가지):
  무릎 굴곡      팔꿈치 각도
  고관절 스윙    상체 기울기
  힙-발목 수직각 정강이 각도
```

#### Step 4 (`#FFF7ED`)

```
레이블: Step 4 — 메트릭 계산
RunningFormMetricsPipeline
─────────────────────────────
• 케이던스 (SPM)
  발목 Y좌표 진동 주기 분석
• 수직 진폭
  골반 최고-최저점 차이 (cm)
• One Euro Filter 실시간 스무딩
```

#### Step 5 (`#FEF3C7`, 두 박스 나란히)

```
레이블 A: Step 5 — 평가 · 피드백
PostureEvaluator + RuleEngine
─────────────────────────────
• 카테고리별 점수 0~100
• 종합 점수 + 등급 A / B / C / D
• 항목별 피드백 문장 생성

레이블 B: Step 5b — 이상 자세 감지
PostureAutoencoderInference
─────────────────────────────
• TFLite 오토인코더
• 정상 자세 패턴과 MSE 비교
• 이상치 → 점수 하향 보조
```

#### 출력 (`#DBEAFE`, 3분할 가로 나란히)

```
레이블 1: 동영상 오버레이
스켈레톤 선 + 각도 수치
ExoPlayer 재생

레이블 2: 종합 점수 뱃지
A / B / C / D 등급
0~100점

레이블 3: 카테고리 카드
무릎 굴곡 · 상체 기울기
팔꿈치 각도 · 고관절 스윙
─────────────
케이던스 · 수직진폭
(참고 지표 — 점수 미반영)
```

#### 하단 강조 배너 2개 (`#F1F5F9`)

```
배너 1 (왼쪽):
🔒  모든 분석은 기기 내에서만 처리됩니다.
    영상이 서버로 전송되지 않습니다.

배너 2 (오른쪽):
⚠  본 분석은 러닝 자세 이해를 위한 참고 자료입니다.
   촬영 환경·카메라 각도·의류·조명에 따라 정확도가
   달라질 수 있으며, 의료적 판단의 근거로 사용하지
   마세요.
```

### 화살표

| 출발 | 도착 | 레이블 |
|------|------|--------|
| 영상 입력 | Step 1 | `파일 경로 전달` |
| Step 1 | Step 2 | `Bitmap 프레임 배열` |
| Step 2 | Step 3 | `NormalizedLandmark 33개` |
| Step 3 | Step 4 | `PostureFrameAngles` |
| Step 4 | Step 5A | `각도 + 메트릭 배열` |
| Step 4 | Step 5B | `각도 + 메트릭 배열` |
| Step 5A | 출력 | `PostureResult` |
| Step 5B | Step 5A | `MSE 이상치 보조` |

---

## 전체 다이어그램 요약

| # | 다이어그램 | 목적 |
|---|-----------|------|
| 1 | 전체 시스템 아키텍처 | Android · Wear OS · 서버 · DB 전체 관계 |
| 2 | Android Clean Architecture | UI → Domain → Data → Core 레이어 의존 방향 |
| 3 | GPS 추적 + 음성 안내 흐름 | 런 중 데이터 흐름 · 음성 안내 시점 · 크래시 복구 |
| 4 | Wear OS 연동 흐름 | 워치 명령 → 폰 처리 → 서버 업로드 · 음성 안내 |
| 5 | AI 자세 분석 파이프라인 | MediaPipe → 각도 계산 → 평가 → 결과 출력 |
