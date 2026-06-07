# PathFinder (RunWay) 프로젝트 종합 설명서

> 작성일: 2026-06-08  
> 기준 버전: main 브랜치 최신 상태  
> 대상 독자: 개발자, 기획자, 팀원

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [백엔드 상세](#3-백엔드-상세)
4. [Android 앱 상세](#4-android-앱-상세)
5. [Wear OS 앱 상세](#5-wear-os-앱-상세)
6. [핵심 기능 상세](#6-핵심-기능-상세)
7. [부가 기능](#7-부가-기능)
8. [데이터베이스 설계](#8-데이터베이스-설계)
9. [인프라 및 배포](#9-인프라-및-배포)
10. [보안 설계](#10-보안-설계)
11. [전문 용어 해설](#11-전문-용어-해설)

---

## 1. 프로젝트 개요

### 서비스 정의

**PathFinder(RunWay)**는 GPS 기반 러닝 경로 공유 및 경쟁 플랫폼이다. 사용자는 직접 뛴 경로를 코스로 등록해 공개하고, 다른 러너들은 그 코스에 도전하여 리더보드에서 기록을 경쟁한다. AI 러닝 자세 분석과 Galaxy Watch 연동 기능도 제공한다.

### 핵심 가치

| 가치 | 설명 |
|------|------|
| **Social & Discovery** | 코스를 공유하고 함께 뛰는 커뮤니티 경험 |
| **Precision Tracking** | PostGIS 공간 DB 기반의 정밀한 GPS 경로 관리 |
| **Health & AI** | MediaPipe + 오토인코더 기반 러닝 자세 분석 |
| **Wearable Integration** | Galaxy Watch 6 독립 실행 Wear OS 앱으로 코스 도전 가능 |
| **Reliability** | ForegroundService + 크래시 복구 시스템으로 달리는 도중 데이터 손실 방지 |

### 사용 흐름 (User Journey)

```
런 시작 → GPS 수집 → 런 완료 → 코스 생성 → 코스 탐색 → 코스 도전 → 리더보드
                        ↓
               AI 자세 분석 (영상 촬영 후 별도 진행)

워치 앱: 코스 동기화 → GPS 준비 → 카운트다운 → 코스 도전 → 경로 이탈 감지 → 완주
```

### 레포지토리 구조

```
RunWay/
├── android/          # Kotlin Android 클라이언트 (Jetpack Compose)
├── backend/          # Java Spring Boot REST API 서버
├── wear-os/          # Kotlin Wear OS 클라이언트 (Galaxy Watch 6)
├── docs/             # 기술 명세서, 로드맵 문서
├── design/           # Lovable 기반 UI 프로토타입
└── sample/           # 테스트용 Samsung Health GPX 파일 (17개)
```

---

## 2. 전체 아키텍처

### 시스템 구성도

```
┌──────────────────────────┐     Wearable DataLayer      ┌──────────────────────────┐
│    Galaxy Watch 6         │◄──────────────────────────►│    Android 클라이언트     │
│                          │                             │                          │
│  HealthServices (GPS)     │                             │  Compose UI + ViewModel  │
│  FusedLocationProvider   │                             │  RunTrackingService      │
│  Offline Course Store    │                             │  WatchCommandListener    │
│  Foreground Service      │                             │                          │
└──────────────────────────┘                             └────────────┬─────────────┘
                                                                       │ HTTPS / REST
                                                         ┌─────────────▼─────────────┐
                                                         │   Spring Boot API (OCI)    │
                                                         │                           │
                                                         │  Controller → Service      │
                                                         │  → Repository → PostGIS   │
                                                         │                           │
                                                         │  Spring Security + JWT    │
                                                         └───────────────────────────┘
```

### 아키텍처 패턴

**백엔드**: Layered Architecture (계층형 아키텍처)
- Presentation Layer: REST Controller (HTTP 요청/응답 변환)
- Business Layer: Service (비즈니스 규칙, 트랜잭션)
- Data Access Layer: JPA Repository + Native Query
- Domain Layer: Entity, Value Object, Enum

**Android**: Clean Architecture + MVVM 패턴
- UI Layer: Composable 화면 + ViewModel (상태 관리)
- Domain Layer: Repository 인터페이스 (비즈니스 규칙 독립)
- Data Layer: Repository 구현체 + Retrofit API + Room DB

**Wear OS**: MVI-like 패턴
- `WatchViewModel` 단일 StateFlow로 전체 앱 상태 관리
- HealthServices (운동 메트릭) + FusedLocationProvider (GPS 위치) 듀얼 소스

---

## 3. 백엔드 상세

### 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 17 |
| 프레임워크 | Spring Boot | 3.x |
| 보안 | Spring Security + JWT | - |
| 데이터베이스 | PostgreSQL + PostGIS | 15 + 3.4 |
| ORM | Spring Data JPA + Hibernate Spatial | - |
| DB 마이그레이션 | Flyway | - |
| 빌드 도구 | Gradle | - |
| API 문서 | SpringDoc OpenAPI (Swagger) | - |
| 컨테이너화 | Docker + Docker Compose | - |
| 파일 스토리지 | 서버 로컬 파일시스템 (`/opt/runway/uploads/`) | - |

### 패키지 구조

```
com.runway/
├── common/               # 공통 유틸리티
│   ├── config/           # WebConfig (정적 파일 서빙 /uploads/**)
│   ├── exception/        # 전역 예외 처리 (RunwayException, ErrorCode)
│   ├── response/         # API 응답 형식 (ApiResponse, PageResponse)
│   └── security/         # JWT 필터, 인증 설정 (SecurityConfig)
├── auth/                 # 회원가입 / 로그인 / 토큰 재발급
├── user/                 # 프로필 관리 / 이미지 업로드 / 통계 / 업적
├── run/                  # 러닝 기록 추적 / GPS 포인트 저장
├── course/               # 코스 CRUD / 탐색 / 평점 / 신고 / 즐겨찾기
└── attempt/              # 코스 도전 / 완주 / 리더보드
```

### REST API 목록

#### 인증 (`/api/auth`)

| 메서드 | 경로 | 기능 |
|--------|------|------|
| POST | `/signup` | 회원가입 |
| POST | `/login` | 로그인 (Access + Refresh 토큰 반환) |
| POST | `/reissue` | Access 토큰 재발급 |
| POST | `/logout` | 로그아웃 (Refresh 토큰 무효화) |

#### 사용자 (`/api/users`)

| 메서드 | 경로 | 기능 |
|--------|------|------|
| GET | `/me` | 내 프로필 조회 |
| PUT | `/me` | 프로필 수정 (닉네임, 소개, 프로필 이미지 URL) |
| POST | `/me/profile-image` | 프로필 이미지 업로드 (multipart/form-data → URL 반환) |
| DELETE | `/me` | 회원 탈퇴 (Soft Delete) |
| GET | `/me/achievements` | 업적 조회 |

#### 러닝 (`/api/runs`)

| 메서드 | 경로 | 기능 |
|--------|------|------|
| POST | `/start` | 런 시작 (running_record 생성) |
| POST | `/{runId}/points` | GPS 포인트 배치 저장 |
| POST | `/{runId}/pause` | 일시정지 |
| POST | `/{runId}/resume` | 재개 |
| POST | `/{runId}/finish` | 런 완료 (경로 LineString 생성) |
| POST | `/{runId}/abandon` | 런 중단 |
| GET | `/me` | 내 러닝 기록 목록 (페이지네이션) |
| GET | `/{runId}` | 러닝 기록 상세 (GPS 경로 포함) |
| GET | `/me/records` | 개인 최고 기록 (5km, 10km, 하프, 풀) |
| GET | `/me/stats` | 러닝 통계 (주간/월간/연간/전체) |
| DELETE | `/{runId}` | 러닝 기록 삭제 |
| PATCH | `/{runId}/trim` | 거리 단축 (준비 구간 제거) |

#### 코스 (`/api/courses`)

| 메서드 | 경로 | 기능 |
|--------|------|------|
| POST | `/from-run/{runId}` | 러닝 기록으로 코스 생성 |
| GET | `/nearby` | 반경 내 코스 탐색 (PostGIS ST_DWithin) |
| GET | `/me` | 내가 만든 코스 목록 |
| GET | `/favorites` | 즐겨찾기 코스 목록 |
| GET | `/participated` | 참여한 코스 목록 |
| GET | `/{courseId}` | 코스 상세 정보 |
| GET | `/{courseId}/points` | 코스 경로 좌표 목록 |
| PUT | `/{courseId}` | 코스 기본 정보 수정 |
| PATCH | `/{courseId}/publish` | 코스 공개 (10회 완주 + 메타데이터 필수) |
| DELETE | `/{courseId}` | 초안(draft) 코스 삭제 — 공개 코스는 삭제 불가 |
| POST | `/{courseId}/reports` | 코스 신고 |
| POST | `/{courseId}/ratings` | 코스 평점 등록/수정 |
| POST | `/{courseId}/favorite` | 즐겨찾기 추가 |
| DELETE | `/{courseId}/favorite` | 즐겨찾기 제거 |

> **정책**: 한번 공개된 코스(`published`)는 보관/삭제 불가. 공개 코스는 영구적으로 다른 사용자에게 유지됨.

#### 코스 도전 (`/api/courses`, `/api/course-attempts`)

| 메서드 | 경로 | 기능 |
|--------|------|------|
| POST | `/courses/{courseId}/attempts/start` | 도전 시작 |
| POST | `/course-attempts/{attemptId}/finish` | 완주 처리 |
| POST | `/course-attempts/{attemptId}/abandon` | 도전 포기 |
| GET | `/courses/{courseId}/leaderboard` | 리더보드 조회 |
| GET | `/courses/{courseId}/attempts/me/best` | 내 최고 기록 |
| GET | `/courses/{courseId}/attempts/me` | 내 도전 이력 |

### 공통 응답 형식

```json
// 성공 응답
{ "success": true, "message": "요청이 성공했습니다.", "data": { ... } }

// 오류 응답
{ "success": false, "message": "에러 메시지", "errorCode": "ERROR_CODE" }
```

### 백엔드 핵심 설계 결정

#### 1. PostGIS를 이용한 공간 쿼리

반경 내 코스 탐색, 거리 계산에 PostgreSQL의 지리정보 확장인 PostGIS를 사용한다. 일반 SQL로는 불가능한 "내 위치에서 5km 반경 내 코스 찾기"를 단일 쿼리로 처리한다.

#### 2. GPS 경로 저장 전략

- **러닝 중**: GPS 포인트를 `running_points` 테이블에 개별 저장 (1초 간격)
- **런 완료 시**: `ST_MakeLine()`으로 LineString 압축 → `running_records.path` 저장
- **코스 생성 시**: RDP 알고리즘으로 핵심 좌표 추출 → `course_points` 저장

#### 3. 프로필 이미지 저장

- `POST /api/users/me/profile-image`로 multipart 이미지 업로드
- 서버 로컬 파일시스템 `/opt/runway/uploads/profiles/` 저장
- Spring Boot `WebMvcConfigurer`로 `/uploads/**` 정적 파일 서빙
- URL 형식: `https://pathfinder.run/uploads/profiles/profile_{userId}.jpg`

#### 4. 코스 공개 불변 정책

공개된 코스는 보관/삭제 불가. 다른 사용자가 이용 중인 코스가 사라지면 데이터 무결성이 깨지기 때문. 초안(draft) 상태 코스만 삭제 가능.

#### 5. 리더보드 설계

별도 랭킹 테이블 없이 `course_attempts`에서 `RANK() OVER` 윈도우 함수로 실시간 집계.

#### 6. Privacy Zone (프라이버시 보호)

코스 소유자가 아닌 사람이 코스 경로 조회 시, 시작점에서 150m~1.1km 구간 자동 마스킹.

#### 7. Soft Delete (소프트 삭제)

사용자 탈퇴, 코스 삭제 시 `deleted_at` 타임스탬프만 기록. 연결 데이터 무결성 유지, 복구 가능성 보존.

---

## 4. Android 앱 상세

### 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| 언어 | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.10.01 |
| 네비게이션 | Navigation Compose | 2.8.4 |
| 의존성 주입 | Hilt | 2.52 |
| 네트워크 | Retrofit2 + OkHttp | 2.11.0 / 4.12.0 |
| 로컬 DB | Room | 2.6.1 |
| 로컬 저장소 | DataStore Preferences | 1.1.1 |
| 지도 | Google Maps SDK + Maps Compose | 19.0.0 / 4.4.1 |
| GPS | Fused Location Provider (Google Play Services) | 21.3.0 |
| AI/ML | MediaPipe Pose Landmarker | 0.10.35 |
| 비디오 | Media3 ExoPlayer | 1.5.1 |
| 카메라 | CameraX | 1.4.1 |
| 이미지 로딩 | Coil | 2.7.0 |
| Wear OS | Play Services Wearable | 19.0.0 |

### 앱 화면 구성

#### 인증 플로우
- **RunwaySplashScreen**: 로그인 상태 판별
- **OnboardingScreen**: 최초 실행 시 서비스 소개
- **LoginScreen** / **SignupScreen**: 이메일 인증

#### 메인 탭 (BottomNav 5개)
```
홈(Home) | 탐색(Discover) | 코스(Courses) | 자세(Posture) | 프로필(Profile)
```

- **HomeScreen**: 날씨 + 최근 런 요약 + 주변 코스 미리보기
- **DiscoverScreen**: 지도 뷰(마커 클러스터링) + 목록 뷰 + 필터
- **CoursesLibraryScreen**: 내 코스 / 즐겨찾기 / 참여한 코스
- **PostureHomeScreen**: 자세 분석 이력 + 새 분석 시작 + AI 면책 경고 배너
- **ProfileScreen**: 프로필(이미지 변경 포함) + 통계 + 업적 + 설정

#### 러닝 플로우
- **RunningTrackingScreen**: 실시간 GPS 추적 + 카운트다운(탭으로 건너뛰기)
- **RunResultScreen**: 런 완료 후 결과
- **MyRunsScreen** / **RunDetailScreen**: 기록 목록 + 상세 (하단 "코스 만들기" 버튼)

#### 코스 플로우
- **CourseDetailScreen**: 코스 정보 + 평점(최초 1회) + 도전 시작 + 초안 삭제
- **CourseMapDetailScreen**: 코스 경로 전체 지도 (줌 시 경로 유지 고정)
- **CourseAttemptTrackingScreen**: 코스 도전 실시간 추적 + 이탈 경고 + 완주 테두리 효과
- **CourseLeaderboardScreen**: 완주 기록 순위표

#### 자세 분석 플로우
- **PostureCaptureScreen**: CameraX 촬영
- **PostureAnalyzingScreen**: MediaPipe 분석 중
- **PostureResultScreen**: 분석 결과 + 동영상 오버레이 + 면책 경고

### Wear OS ↔ Android 연동

```
┌─────────────────────────────────────────────────────┐
│                  WatchCommandListenerService          │
│  (WearableListenerService — 워치 메시지 수신)         │
├─────────────────────────────────────────────────────┤
│ 처리 경로                                             │
│ /runway/watch/command    → WearRunSessionCoordinator │
│ /runway/watch/run-upload → WatchRunUploadCoordinator │
│ /runway/watch/course-req → WatchCourseSyncCoordinator│
│ /runway/watch/open-course→ WatchNavRepository        │
│ /runway/watch/open-run   → WatchNavRepository        │
│ /runway/watch/open-app   → MainActivity 포그라운드   │
│ /runway/watch/auth/req   → sendAuthState(토큰 유무)  │
└─────────────────────────────────────────────────────┘
```

**워치 런 업로드 플로우 (`WatchRunUploadCoordinator`):**
1. 워치가 DataItem으로 런 데이터 전송
2. 폰이 서버에 `startRun` → `savePoints` → `finishRun` 순서로 업로드
3. 업로드 완료 ACK에 서버 `runId` 포함하여 워치로 전송
4. 워치 요약 화면에서 "📱 에서 코스로 등록" 버튼 활성화

---

## 5. Wear OS 앱 상세

### 개요

Galaxy Watch 6 Classic(minSdk 30, Wear OS 4)에서 독립 실행되는 러닝 앱. 폰 앱과 DataLayer로 통신하되, **오프라인(폰 미연결)에서도 런닝 기록 가능**.

### 기술 스택

| 항목 | 기술 |
|------|------|
| 언어 | Kotlin |
| UI | Jetpack Compose Wear Material 3 |
| 운동 추적 | Wear OS HealthServices (`ExerciseClient`) |
| GPS | HealthServices + FusedLocationProvider (듀얼) |
| 음성 | Android TextToSpeech (오프라인 한국어 여성 음성) |
| 데이터 동기화 | Wearable DataLayer (MessageClient + DataClient) |
| 화면 유지 | `FLAG_KEEP_SCREEN_ON` (앱 포그라운드 전 구간) |
| AOD | `AmbientLifecycleObserver` (런닝 중 화면 dim 표시) |
| 서비스 | `RunForegroundService` (FOREGROUND_SERVICE_TYPE_LOCATION) |

### 화면 구성 (`WatchScreen` enum)

```
HOME → PREPARING → COUNTDOWN → TRACKING ↔ PAUSED → SUMMARY
         ↓
     COURSE_LIST → COURSE_DETAIL
         ↓
     GOAL_TYPE → TIME_GOAL / DISTANCE_GOAL / INTERVAL_GOAL
```

### 코스 도전 로직 (핵심)

**핸드폰 앱과 동일한 동작 보장:**

1. **코스 동기화**: 폰 앱에서 주변 코스를 DataLayer로 워치에 오프라인 저장
2. **GPS 준비 화면**: 30m 이내 정확도 확인 후에만 시작 가능
3. **출발 위치 즉시 확인**: `startTracking()` 시 `lastReadyLocation`으로 경로 이탈 체크 → 이탈 시 0.3초 내 자동 정지
4. **경로 이탈 감지 (20m 기준)**:
   - HealthServices GPS 업데이트 → `courseProximity()` 계산
   - 20m 초과 시: 음성 "코스를 이탈했습니다" + 자동 일시정지 + FLP 복귀 모니터 시작
5. **복귀 감지**: 일시정지 중 FLP로 2초마다 위치 확인 → 20m 이내 복귀 시 자동 재개
6. **수동 재개 차단**: 경로 이탈로 정지 중 재생 버튼 무시
7. **완주 조건**: 코스 진행도 100%(경로 투영 거리 기반) **AND** 종착점 30m 이내

**경로 투영 거리 (`courseProximity`):**

단순 이동 거리 대신 코스 경로 위에 투영한 호 길이(arc length)로 진행도 계산. 경로 이탈 중 달린 거리는 완주 진행도에 반영되지 않음. 되돌아가도 감소하지 않음(최대값 유지).

```
현재 위치 X를 경로 선분 AB에 수선을 내려 P를 구함
진행도 = 코스 시작부터 P까지의 거리 / 코스 전체 거리
```

### Ambient Mode (AOD)

런닝 화면에서 손목을 내리면(ambient 진입):
- 검은 배경 + 흰색 시간/거리/페이스만 표시 (저전력)
- 손목을 들면(interactive 복귀) 전체 UI 표시
- `AmbientLifecycleObserver` + `AmbientTrackingScreen` composable

### 오프라인 런 동기화

폰 미연결 시:
- 런 데이터를 `PendingWatchRunStore`(로컬 파일)에 저장
- 폰 연결 감지 시 `WatchDataLayerClient.syncPendingRuns()` 자동 전송

---

## 6. 핵심 기능 상세

### 6.1 GPS 러닝 추적 시스템 (Android)

```
ForegroundService (RunTrackingService)
    └─► RunTrackingManager (Singleton, appScope)
            ├─► FusedLocationProvider (3초 간격 GPS)
            │       └─► GpsPointValidator (12m/s 속도, 200m 거리 필터)
            ├─► CadenceTracker (가속도계 기반 케이던스)
            ├─► Auto-pause (< 0.5m/s × 10회 → 정지, > 1.0m/s × 3회 → 재개)
            ├─► PendingPointQueue (Room DB 크래시 안전 큐)
            │       └─► 5초마다 배치 전송 → 실패 시 재전송
            └─► TrackingSessionStore (DataStore 런 상태 지속 저장)
```

**크래시 복구**: 앱 강제 종료 후 재실행 시 `TrackingRecoveryDialog`로 이어달리기 제안.

---

### 6.2 코스 생성 및 공개 시스템

**개인 코스 생성**: 런 완료 후 하단 "코스 만들기" 버튼 → `status = 'draft'`

**공개 코스 등록 (10회 완주 조건)**:
1. 코스 상세에서 "공개하기" 탭
2. 10회 미만 → 잠금 팝업
3. 10회 이상 → 메타데이터 입력 (난이도/경사도/위험도/노면/추천 시간대)
4. `status = 'published'` → 탐색 노출, 이후 영구 공개 (보관/삭제 불가)

**초안 코스 삭제**: 공개 전 draft 코스는 삭제 가능 (`DELETE /api/courses/{courseId}`).

---

### 6.3 음성 안내 시스템

`RunningVoiceGuide` — Android 내장 TTS, 오프라인 한국어 여성 음성 자동 선택.

**적용 범위**: 폰 자유 런 / 폰 코스 도전 / 워치 자유 런 / 워치 코스 도전

| 시점 | 안내 문구 |
|------|----------|
| 런 시작 | "러닝을 시작합니다. 오늘도 힘차게 달려볼까요?" |
| 1km마다 | "N킬로미터 완료. 시간 X분 Y초. 평균 페이스 A분 B초." |
| 자동 일시정지 | "속도가 줄어 자동으로 일시정지합니다." |
| 자동 재개 | "다시 달리기 시작했습니다." |
| 코스 이탈 | "코스를 이탈했습니다. 안전하게 코스로 돌아와 주세요." |
| 코스 복귀 | "코스로 복귀했습니다. 러닝을 계속합니다." |
| 90% 완주 | "거의 다 왔어요! 조금만 더 달려요!" |
| 코스 완주 | "코스를 완주했습니다! 수고하셨습니다." |
| 런 완료 | "러닝을 종료합니다. 수고하셨습니다." |

---

### 6.4 코스 도전 완주 효과 (Android)

완주(100%) 달성 시:
- 음성 "코스를 완주했습니다! 수고하셨습니다."
- 강한 진동 3회
- 테마 색상 테두리 박동 애니메이션
- 컨피티 이펙트
- 1.5초 후 리더보드 팝업 자동 표시

---

### 6.5 주변 코스 탐색 (Discover)

**지도 뷰**: 줌 레벨 기반 그리드 클러스터링. 클러스터 탭 → 하단 카드 목록.

**목록 뷰**: GPS 기반 반경 1/3/5km 선택, 거리/루프 필터, 키워드 검색, 정렬.

---

### 6.6 코스 평점

- 한 코스에 최초 1회만 평가 가능 (로컬 SharedPreferences로 추적)
- 이미 평가한 코스: "평가 완료" 표시로 대체

---

### 6.7 AI 러닝 자세 분석

영상 → MediaPipe BlazePose → 12개 각도 계산 → 점수/등급 → 오토인코더 이상치 감지

모든 처리가 기기 내(On-Device)에서 이루어져 개인 영상 서버 전송 없음.

**분석 결과**: 동영상 스켈레톤 오버레이 재생 + 카테고리별 피드백 + 면책 경고문(3줄, 의료적 판단 근거 사용 금지).

---

## 7. 부가 기능

### 7.1 러닝 통계
- 주간/월간/연간/전체 통계 차트
- 개인 최고 기록 (5km, 10km, 하프, 풀)

### 7.2 업적(Achievements) 시스템
- 누적 거리, 런 횟수, 코스 완주 수 기준 업적

### 7.3 공유 이미지 생성
- 런 완료 후 결과 카드 → 템플릿 선택 → SNS 공유

### 7.4 1km 스플릿
- `SplitCalculator`로 각 1km 구간 페이스 계산

### 7.5 러닝 리마인더 알림
- `AlarmManager.setExact()` + `BootReceiver` 재부팅 복구

### 7.6 프로필 이미지 변경
- 갤러리 선택 → multipart 업로드 → 서버 저장 → 프로필에 즉시 반영

### 7.7 공식 코스 데이터

실제 GPS 기반 공개 코스 7개:

| 코스 | 거리 |
|------|------|
| 서울 청계천 | 8.7km |
| 서울 남산 순환도로 | 7.5km |
| 서울 반포 한강공원 | 5.0km |
| 서울 양재천 | 9.5km |
| 부산 해운대 해변 | 2.5km |
| 충주 강변 코스 10K | 9.96km |
| 충주 강변 코스 20K | 19.84km |

---

## 8. 데이터베이스 설계

### ERD 개요

```
users
  ├──< running_records
  │         └──< running_points
  ├──< courses
  │         ├──< course_points
  │         ├──< course_ratings
  │         ├──< course_reports
  │         └──< course_favorites
  └──< course_attempts
            └── running_record
```

### 주요 테이블

#### users
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID (PK) | 사용자 ID |
| email | VARCHAR(255) UNIQUE | 이메일 |
| password_hash | VARCHAR(255) | BCrypt 해시 |
| nickname | VARCHAR(50) UNIQUE | 닉네임 |
| profile_image_url | TEXT | 프로필 이미지 URL |
| bio | TEXT | 소개글 |
| refresh_token_hash | VARCHAR(512) | Refresh Token 해시만 저장 |
| deleted_at | TIMESTAMPTZ | Soft Delete |

#### courses
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID (PK) | 코스 ID |
| creator_id | UUID (FK) | 생성자 |
| status | VARCHAR(30) | draft / published (archived 정책 폐지) |
| distance_meters | FLOAT | 코스 거리 |
| is_loop | BOOLEAN | 루프 코스 여부 |
| start_location | geography(Point,4326) | 시작 좌표 |
| path | geography(LineString,4326) | 코스 경로 |
| attempt_count / completion_count | INT | 도전/완주 횟수 캐시 |
| deleted_at | TIMESTAMPTZ | draft 코스 소프트 삭제 |

#### course_attempts
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID (PK) | 도전 ID |
| course_id / user_id / running_record_id | UUID (FK) | 연결 키 |
| status | VARCHAR(30) | in_progress / completed / abandoned |
| verification_status | VARCHAR(30) | pending / verified / rejected |
| duration_seconds | INT | 완주 소요 시간 (리더보드 기준) |

### 인덱스 전략

| 테이블 | 인덱스 | 목적 |
|--------|--------|------|
| courses | GiST(start_location) | ST_DWithin() 반경 탐색 |
| course_attempts | B-tree(course_id, duration_seconds) WHERE status='completed' | 리더보드 |
| running_records | B-tree(user_id, started_at DESC) | 내 러닝 목록 |
| running_points | B-tree(running_record_id, sequence) | 경로 순서 |

### Flyway 마이그레이션 이력

| 버전 | 설명 |
|------|------|
| V1 | PostGIS, pgcrypto 확장 초기화 |
| V2~V7 | 핵심 테이블 생성 |
| V8~V10 | 보조 테이블 (신고, 평점, 즐겨찾기) |
| V11 | 코스 공개 메타데이터 컬럼 추가 |
| V14 | OSM 실측 GPS 코스 5개 (서울, 부산) |
| V15 | 충주 Samsung Health GPS 기록 코스 2개 |

---

## 9. 인프라 및 배포

### 서버 환경

| 항목 | 사양 |
|------|------|
| 클라우드 | Oracle Cloud Infrastructure (OCI) |
| IP | 40.233.98.156 |
| OS | Ubuntu |
| 런타임 | Java 17 |
| DB | Docker (postgis/postgis:15-3.4) |
| 파일 스토리지 | `/opt/runway/uploads/` (Spring 정적 서빙) |

### 배포 구성

```
GitHub (main 브랜치 push)
    └─► GitHub Actions (CI/CD)
            ├─ ./gradlew bootJar (로컬 빌드 후 SCP로 JAR 업로드)
            └─ systemctl restart runway-api

또는 수동 배포:
  로컬에서 bootJar 빌드 → SCP 업로드 → SSH systemctl restart
```

> CI/CD는 Gradle 다운로드 네트워크 이슈로 실패할 수 있음. 이 경우 로컬 빌드 후 SCP 직접 업로드.

---

## 10. 보안 설계

### 인증 방식 (JWT)

```
로그인 성공
    └─► Access Token (1시간) + Refresh Token (2주, DB에 해시만 저장)

Android OkHttp TokenAuthenticator:
    └─► Access Token 만료 시 자동 reissue → 원래 요청 재시도

Refresh Token Rotation:
    └─► 재발급 성공 시 Refresh Token도 갱신
```

### 데이터 보호

- **비밀번호**: BCrypt 해싱
- **Refresh Token**: BCrypt 해싱 후 저장
- **Privacy Zone**: 코스 시작/종료 지점 150m~1.1km 자동 마스킹
- **프로필 이미지**: 서버 로컬 저장, HTTPS로만 서빙
- **자세 분석 영상**: 기기 내 처리, 서버 전송 없음

### API 인증 정책

- 공개: `/api/auth/**`, `/uploads/**` (이미지 파일)
- 인증 필수: 나머지 모든 `/api/**`
- 소유자 확인: 서비스 레이어에서 재검증

---

## 11. 전문 용어 해설

### 아키텍처 용어

| 용어 | 설명 |
|------|------|
| **Clean Architecture** | UI, 비즈니스 로직, 데이터 접근을 계층으로 분리하는 설계 패턴 |
| **MVVM** | Model-View-ViewModel. ViewModel이 상태와 비즈니스 로직 관리 |
| **Repository 패턴** | 데이터 소스를 추상화하는 계층 |
| **DI** | 의존성 주입. Hilt가 담당 |
| **Soft Delete** | `deleted_at` 타임스탬프만 기록하는 논리 삭제 |
| **MVI** | Model-View-Intent. 단방향 데이터 흐름 패턴 (Wear OS 앱에서 유사 적용) |

### Android/Wear OS 기술 용어

| 용어 | 설명 |
|------|------|
| **Jetpack Compose** | 선언형 Android UI 프레임워크 |
| **HealthServices** | Wear OS 운동 추적 API. 심박, GPS, 케이던스 등 운동 데이터 제공 |
| **ExerciseClient** | HealthServices의 운동 세션 관리 클라이언트 |
| **AmbientLifecycleObserver** | Wear OS AOD(Always-On Display) 생명주기 관리 |
| **DataLayer** | 폰-워치 간 통신 API. MessageClient(단방향 명령) + DataClient(동기화) |
| **ForegroundService** | 알림을 보여주며 백그라운드 실행되는 Android 서비스. GPS 추적 필수 |
| **FusedLocationProvider** | GPS + 네트워크 + 가속도계 융합 위치 제공자 |
| **StateFlow** | Kotlin 코루틴 기반 상태 홀더 |
| **Room** | Android 로컬 SQLite ORM |
| **Hilt** | Android 의존성 주입 프레임워크 |

### 백엔드/DB 기술 용어

| 용어 | 설명 |
|------|------|
| **PostGIS** | PostgreSQL 지리정보 확장. 반경 탐색, 거리 계산 지원 |
| **ST_DWithin** | 반경 내 지점 탐색 PostGIS 함수 |
| **geography 타입** | WGS84 구면 좌표계. 실제 지구 거리 계산 정확 |
| **RANK() OVER** | 리더보드 순위 계산 SQL 윈도우 함수 |
| **Flyway** | DB 스키마 버전 관리 도구 |
| **BCrypt** | 비밀번호 해싱 알고리즘 |
| **JWT** | 서버 발급 자가 검증 토큰 |
| **RDP 알고리즘** | GPS 경로 좌표 단순화 (Ramer-Douglas-Peucker) |

### GPS/지도 용어

| 용어 | 설명 |
|------|------|
| **Course Proximity** | 현재 위치에서 코스 경로까지의 최단 거리 + 경로상 투영 거리 |
| **Arc Length (호 길이)** | 코스 경로 선분을 따라 측정한 실제 진행 거리. 직선 거리와 다름 |
| **경로 투영** | 현재 위치를 코스 선분에 수선을 내려 가장 가까운 경로상 지점 계산 |
| **케이던스 (SPM)** | 분당 걸음 수 (Steps Per Minute) |
| **페이스** | 1km 소요 시간 (분/km) |
| **Privacy Zone** | 코스 시작/종료 지점 주변 마스킹 구간 |
| **WGS84 (EPSG:4326)** | 전 세계 표준 GPS 좌표계 |

### AI/ML 기술 용어

| 용어 | 설명 |
|------|------|
| **MediaPipe** | Google ML 파이프라인. BlazePose 33개 관절 추출 |
| **Kalman Filter** | 관절 좌표 노이즈 제거 필터 |
| **One Euro Filter** | 실시간 스무딩 필터 |
| **Autoencoder** | 정상 자세 패턴 학습으로 이상 자세 감지 |
| **TensorFlow Lite** | 모바일 경량 ML 런타임 |
| **Aspect Ratio 보정** | 세로 영상에서 X/Y 축 왜곡 교정 |

---

*이 문서는 2026-06-08 기준 main 브랜치 최신 상태를 반영합니다.*
