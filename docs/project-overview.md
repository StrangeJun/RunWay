# PathFinder (RunWay) 프로젝트 종합 설명서

> 작성일: 2026-06-07  
> 기준 버전: main 브랜치 최신 상태  
> 대상 독자: 개발자, 기획자, 팀원

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [백엔드 상세](#3-백엔드-상세)
4. [Android 앱 상세](#4-android-앱-상세)
5. [핵심 기능 상세](#5-핵심-기능-상세)
6. [부가 기능](#6-부가-기능)
7. [데이터베이스 설계](#7-데이터베이스-설계)
8. [인프라 및 배포](#8-인프라-및-배포)
9. [보안 설계](#9-보안-설계)
10. [전문 용어 해설](#10-전문-용어-해설)

---

## 1. 프로젝트 개요

### 서비스 정의

**PathFinder(RunWay)**는 GPS 기반 러닝 경로 공유 및 경쟁 플랫폼이다. 사용자는 직접 뛴 경로를 코스로 등록해 공개하고, 다른 러너들은 그 코스에 도전하여 리더보드에서 기록을 경쟁한다. 여기에 더해 스마트폰 카메라와 인공지능(AI)을 이용한 러닝 자세 분석 기능을 제공한다.

### 핵심 가치

| 가치 | 설명 |
|------|------|
| **Social & Discovery** | 코스를 공유하고 함께 뛰는 커뮤니티 경험 |
| **Precision Tracking** | PostGIS 공간 DB 기반의 정밀한 GPS 경로 관리 |
| **Health & AI** | MediaPipe + 오토인코더 기반 러닝 자세 분석 |
| **Reliability** | ForegroundService + 크래시 복구 시스템으로 달리는 도중 데이터 손실 방지 |

### 사용 흐름 (User Journey)

```
런 시작 → GPS 수집 → 런 완료 → 코스 생성 → 코스 탐색 → 코스 도전 → 리더보드
                        ↓
               AI 자세 분석 (영상 촬영 후 별도 진행)
```

### 레포지토리 구조

```
RunWay/
├── android/          # Kotlin Android 클라이언트 (Jetpack Compose)
├── backend/          # Java Spring Boot REST API 서버
├── docs/             # 기술 명세서, 로드맵 문서
├── design/           # Lovable 기반 UI 프로토타입
└── sample/           # 테스트용 Samsung Health GPX 파일 (17개)
```

---

## 2. 전체 아키텍처

### 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────┐
│                        Android 클라이언트                         │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ Compose  │  │ViewModel │  │Repository│  │ Local Storage │  │
│  │   UI     │◄─│(State)   │◄─│(Domain)  │◄─│ Room / DS     │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘  │
│                                     │                           │
│                              Retrofit2 + OkHttp                 │
└─────────────────────────────────────┼───────────────────────────┘
                                      │ HTTPS / REST API
                                      │
┌─────────────────────────────────────▼───────────────────────────┐
│                     Spring Boot API 서버 (OCI)                   │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │Controller│  │ Service  │  │Repository│  │  PostGIS DB   │  │
│  │  (REST)  │─►│(Business │─►│  (JPA)   │─►│ PostgreSQL 15 │  │
│  │          │  │  Logic)  │  │          │  │ + PostGIS 3.4 │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘  │
│                                                                 │
│  Spring Security (JWT) + Flyway (DB Migration)                  │
└─────────────────────────────────────────────────────────────────┘
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

### 패키지 구조

```
com.runway/
├── common/               # 공통 유틸리티
│   ├── exception/        # 전역 예외 처리 (RunwayException, ErrorCode)
│   ├── response/         # API 응답 형식 (ApiResponse, PageResponse)
│   └── security/         # JWT 필터, 인증 설정 (SecurityConfig)
├── auth/                 # 회원가입 / 로그인 / 토큰 재발급
├── user/                 # 프로필 관리 / 통계 / 업적
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
| PUT | `/me` | 프로필 수정 (닉네임, 소개) |
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
| PATCH | `/{courseId}/publish` | 코스 공개 (메타데이터 필수) |
| PATCH | `/{courseId}/archive` | 코스 보관 처리 |
| POST | `/{courseId}/reports` | 코스 신고 |
| POST | `/{courseId}/ratings` | 코스 평점 등록/수정 |
| POST | `/{courseId}/favorite` | 즐겨찾기 추가 |
| DELETE | `/{courseId}/favorite` | 즐겨찾기 제거 |

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
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": { ... }
}

// 오류 응답
{
  "success": false,
  "message": "에러 메시지",
  "errorCode": "ERROR_CODE"
}

// 페이지네이션 응답
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true
  }
}
```

### 백엔드 핵심 설계 결정

#### 1. PostGIS를 이용한 공간 쿼리

반경 내 코스 탐색, 거리 계산에 PostgreSQL의 지리정보 확장인 PostGIS를 사용한다. 일반 SQL로는 불가능한 "내 위치에서 5km 반경 내 코스 찾기"를 단일 쿼리로 처리한다.

```sql
-- 예시: 현재 위치에서 반경 3km 내 공개된 코스 탐색
SELECT c.* FROM courses c
WHERE ST_DWithin(
  c.start_location,
  ST_SetSRID(ST_MakePoint(127.8675, 36.9674), 4326)::geography,
  3000  -- 미터 단위
)
AND c.status = 'published'
AND c.deleted_at IS NULL
ORDER BY ST_Distance(c.start_location, ...) ASC;
```

#### 2. GPS 경로 저장 전략

- **러닝 중**: GPS 포인트를 `running_points` 테이블에 개별 저장 (상세 기록용, 1초 간격)
- **런 완료 시**: 전체 포인트를 `ST_MakeLine()`으로 하나의 LineString으로 압축해 `running_records.path`에 저장 (지도 표시용)
- **코스 생성 시**: RDP(Ramer-Douglas-Peucker) 알고리즘으로 핵심 좌표만 추출해 `course_points`에 저장 (전송 효율화)

#### 3. 리더보드 설계

별도 랭킹 테이블 없이 `course_attempts` 테이블에서 `RANK() OVER` 윈도우 함수로 실시간 집계. 완주 시 동일 트랜잭션에서 `verification_status = 'verified'`를 자동 설정 (Phase 1 단순화).

#### 4. Privacy Zone (프라이버시 보호)

코스 소유자가 아닌 사람이 코스 경로를 조회할 때, `CoursePrivacyUtils`가 시작점에서 150m ~ 1.1km 구간을 자동으로 마스킹한다. 자택 등 민감한 위치 노출을 방지한다.

#### 5. Soft Delete (소프트 삭제)

사용자 탈퇴, 코스 삭제 시 실제 DB 행을 삭제하지 않고 `deleted_at` 타임스탬프만 기록한다. 연결된 데이터(코스 도전 기록 등)가 고아 상태가 되는 것을 방지하고, 데이터 복구 가능성을 유지한다.

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

### 앱 화면 구성 (28개 화면)

#### 인증 플로우
- **RunwaySplashScreen**: 앱 시작 시 짧은 스플래시 (로그인 상태 판별)
- **OnboardingScreen**: 최초 실행 시 서비스 소개
- **PermissionScreen**: GPS, 알림 권한 요청 안내
- **LoginScreen**: 이메일/비밀번호 로그인
- **SignupScreen**: 회원가입

#### 메인 탭 (BottomNav 5개)

```
홈(Home) | 탐색(Discover) | 코스(Courses) | 자세(Posture) | 프로필(Profile)
```

- **HomeScreen**: 오늘의 날씨 + 최근 런 요약 + 주변 코스 미리보기 + 러닝 시작 버튼
- **DiscoverScreen**: 지도 뷰(마커 클러스터링) + 목록 뷰 + 거리/루프 필터
- **CoursesLibraryScreen**: 내가 만든 코스 / 즐겨찾기 / 참여한 코스 탭
- **PostureHomeScreen**: 자세 분석 이력 목록 + 새 분석 시작
- **ProfileScreen**: 프로필 정보 + 통계 + 업적 + 설정

#### 러닝 플로우
- **RunningTrackingScreen**: 실시간 GPS 추적 + 페이스/거리/시간 + 지도
- **RunResultScreen**: 런 완료 후 결과 (코스 저장, 공유 이미지 생성 진입)
- **RunShareImageScreen**: 결과 카드 템플릿 선택 및 공유
- **MyRunsScreen**: 내 모든 러닝 기록 목록
- **RunDetailScreen**: 특정 런 상세 (GPS 경로 지도 + km 스플릿)

#### 코스 플로우
- **CourseDetailScreen**: 코스 정보 + 경로 지도 + 리더보드 미리보기 + 도전 시작
- **CourseMapDetailScreen**: 코스 경로 전체 지도 (클러스터 마커 + 1km 표시)
- **CourseAttemptTrackingScreen**: 코스 도전 중 실시간 추적 + 이탈 경고
- **CourseLeaderboardScreen**: 코스 완주 기록 순위표
- **MyCoursesScreen**: 내가 만든 코스 관리

#### 자세 분석 플로우
- **PostureCaptureScreen**: CameraX로 영상 촬영
- **PostureAnalyzingScreen**: MediaPipe 분석 진행 중 (로딩)
- **PostureResultScreen**: 분석 결과 (점수 + 카테고리별 피드백 + 경고 문구)

#### 기타 화면
- **StatsScreen**: 주간/월간/연간 러닝 통계 차트
- **AchievementsScreen**: 달성 업적 목록
- **ReminderScreen**: 러닝 알림 시간 설정
- **SettingsScreen**: 테마, 계정 설정

### Clean Architecture 레이어 구조

```
android/app/src/main/java/com/runway/android/
│
├── ui/                          # UI Layer (Compose + ViewModel)
│   ├── auth/                    # 로그인, 회원가입
│   ├── home/                    # 홈 화면
│   ├── running/                 # 러닝 추적, 결과, 기록
│   ├── course/                  # 코스 상세, 라이브러리
│   ├── discover/                # 코스 탐색 지도/목록
│   ├── attempt/                 # 코스 도전 추적
│   ├── posture/                 # 자세 분석
│   ├── profile/                 # 프로필
│   ├── leaderboard/             # 리더보드
│   ├── share/                   # 공유 이미지
│   ├── stats/                   # 통계
│   ├── achievements/            # 업적
│   ├── components/              # 재사용 컴포넌트 (RouteMapView 등)
│   ├── navigation/              # NavGraph, MainScaffold
│   └── theme/                   # 색상, 타이포그래피
│
├── domain/                      # Domain Layer (인터페이스만)
│   ├── auth/AuthRepository.kt
│   ├── course/CourseRepository.kt
│   ├── running/RunningRepository.kt
│   ├── user/UserRepository.kt
│   └── attempt/CourseAttemptRepository.kt
│
├── data/                        # Data Layer (구현체)
│   ├── auth/                    # AuthRepositoryImpl + AuthApi
│   ├── course/                  # CourseRepositoryImpl + CourseApi
│   ├── running/                 # RunningRepositoryImpl + RunningApi
│   ├── user/                    # UserRepositoryImpl + UserApi
│   └── attempt/                 # CourseAttemptRepositoryImpl + CourseAttemptApi
│
├── core/                        # 핵심 인프라 (도메인 무관)
│   ├── tracking/                # GPS 추적 ForegroundService
│   ├── posture/                 # MediaPipe 자세 분석
│   ├── location/                # GPS 위치 추적
│   ├── cadence/                 # 케이던스(걸음수) 추적
│   ├── running/                 # 스플릿, 차트 계산
│   ├── share/                   # 이미지 공유
│   ├── network/                 # Interceptor, TokenAuthenticator
│   ├── datastore/               # TokenDataStore, ThemeDataStore
│   ├── notification/            # 알림 채널, 리마인더
│   ├── map/                     # MapPoint, 지도 유틸
│   ├── wear/                    # Wear OS 연동 (WearRunSessionCoordinator)
│   ├── voice/                   # 음성 안내 (RunningVoiceGuide — TTS)
│   └── model/                   # ApiResponse, NetworkResult
│
└── di/                          # Hilt 의존성 주입 모듈
    ├── NetworkModule.kt
    ├── RepositoryModule.kt
    ├── LocationModule.kt
    └── PostureModule.kt
```

---

## 5. 핵심 기능 상세

### 5.1 GPS 러닝 추적 시스템

**배경**: 러닝 중 앱이 백그라운드로 이동하거나 화면이 꺼져도 GPS 기록이 끊기면 안 된다. 또한 네트워크가 불안정하거나 앱이 강제 종료되어도 달린 거리와 시간이 유실되면 안 된다.

**구현:**

```
ForegroundService (RunTrackingService)
    │
    └─► RunTrackingManager (Singleton, appScope)
            │
            ├─► FusedLocationProvider (3초 간격 GPS)
            │       └─► GpsPointValidator (12m/s 속도, 200m 거리, 1m 필터)
            │
            ├─► CadenceTracker (폰 가속도계 기반 케이던스)
            │
            ├─► Auto-pause (속도 < 0.5m/s × 10회 → 자동 일시정지)
            │   Auto-resume (속도 > 1.0m/s × 3회 → 자동 재개)
            │
            ├─► PendingPointQueue (Room DB, 크래시 안전 GPS 큐)
            │       └─► 5초마다 배치 전송 → 실패 시 큐 보관 → 재전송
            │
            └─► TrackingSessionStore (DataStore, 런 상태 지속 저장)
                    └─► 앱 재시작 시 TrackingRecoveryDialog로 복구 안내
```

**핵심 특징:**
- 앱 강제 종료 → 재실행 시 `"달리던 런을 이어하시겠습니까?"` 팝업 표시
- GPS 이상치(순간 이동 등) 자동 필터링 (12m/s 초과 속도)
- 네트워크 오류 시 로컬 Room DB에 포인트 보관, 복구 후 자동 재전송
- `START_NOT_STICKY` 설정으로 시스템이 서비스를 재시작하지 않음 (불완전 런 방지)

---

### 5.2 코스 생성 및 공개 시스템

**개인 코스 생성 (즉시 가능)**
1. 러닝 완료 후 "코스 저장" 버튼 탭
2. 코스 이름 + 루프 여부만 입력
3. 서버에서 GPS 경로 기반 `course_points` 자동 생성
4. `status = 'draft'` (개인 코스, 탐색에 미노출)

**공개 코스 등록 (10회 완주 조건)**
1. 코스 상세 화면에서 "공개하기" 탭
2. `completionCount < 10` → 잠금 팝업 (현재 N/10회 표시)
3. `completionCount >= 10` → 공개 메타데이터 입력 다이얼로그:
   - 난이도 (쉬움/보통/어려움)
   - 경사도 (평탄/완만/가파름)
   - 위험도 (낮음/보통/높음)
   - 노면 유형 (도로/공원/트레일/혼합)
   - 추천 시간대 + 주의사항
4. `status = 'published'` → 코스 탐색에 노출

**설계 의도**: 직접 여러 번 뛰어본 코스만 공개하도록 강제해 데이터 품질을 유지한다.

---

### 5.3 음성 안내 시스템

`RunningVoiceGuide` — Android 내장 TTS(TextToSpeech) 기반 Singleton. 한국어 여성 음성 자동 선택(오프라인 우선).

**자유 런 · 워치 시작 런 공통 안내:**

| 시점 | 안내 문구 |
|------|----------|
| 런 시작 | "러닝을 시작합니다. 오늘도 힘차게 달려볼까요?" |
| 1km마다 | "N킬로미터 완료. 시간 X분 Y초. 평균 페이스 A분 B초." |
| 자동 일시정지 | "속도가 줄어 자동으로 일시정지합니다." |
| 자동 재개 | "다시 달리기 시작했습니다." |
| 런 완료 | "러닝을 종료합니다. 수고하셨습니다." |

**코스 도전 추가 안내:**

| 시점 | 안내 문구 |
|------|----------|
| 코스 이탈 | "코스를 이탈했습니다. 안전하게 코스로 돌아와 주세요." |
| 코스 복귀 | "코스로 복귀했습니다. 러닝을 계속합니다." |
| 90% 완주 | "코스의 90퍼센트를 완주했습니다. 조금만 더 힘내세요." |

**음성 적용 범위:**
- 폰 앱 자유 런 (`RunningTrackingViewModel`)
- 폰 앱 코스 도전 (`CourseAttemptTrackingViewModel`)
- 워치에서 시작한 런 (`WearRunSessionCoordinator`)

---

### 5.4 Wear OS 연동

폰 앱 안에 Wear OS 연동 레이어가 구현되어 있다. 별도 Wear OS 앱 모듈은 개발 계획 단계(`docs/wear-os-development-plan.md`).

**현재 구현된 구성 요소:**

| 파일 | 역할 |
|------|------|
| `WatchCommandListenerService` | `WearableListenerService` — 워치로부터 명령 수신 |
| `WearRunSessionCoordinator` | 워치 명령을 폰 런 세션으로 변환 (GPS, 배치 업로드, 음성 안내) |
| `WatchStateSender` | 폰 런 상태를 워치로 실시간 전송 (DataClient) |
| `WatchCommand` | 명령 타입 (StartFreeRun, StartTimeGoalRun, StartDistanceGoalRun, StartIntervalRun, PauseRun, ResumeRun, FinishRun, AbandonRun) |
| `WatchRunGoal` | 워치에서 설정한 목표 (Free, Time, Distance, Interval) |

**워치 → 폰 → 서버 데이터 흐름:**
```
워치 명령 (MessageClient)
    └─► WatchCommandListenerService
            └─► WearRunSessionCoordinator
                    ├─ ForegroundService 시작 (GPS 수집)
                    ├─ POST /api/runs/start
                    ├─ 5초마다 배치 GPS 업로드
                    ├─ RunningVoiceGuide (음성 안내)
                    └─ WatchStateSender → 워치로 상태 전송
```

---

### 5.5 주변 코스 탐색 (Discover)

**지도 뷰 (Cluster Map)**

줌 레벨에 따른 그리드 기반 클러스터링:
- 가까운 마커들을 하나의 클러스터 마커로 묶음
- 클러스터 마커: 기존 핀 스타일 + 우하단 숫자 뱃지 (코스 수)
- 클러스터 탭 → 하단 패널에 해당 코스 카드 가로 스크롤
- 단일 마커 탭 → 기존 말풍선 (코스명, 거리, 완주 수)
- 줌 변경 시 클러스터 자동 재계산

```
줌 6 (전국 뷰)        줌 12 (도시 뷰)       줌 15 (동네 뷰)
   [3]  [7]              [A] [B]              [A]  [B]  [C]
   (클러스터)           (개별 마커)          (모두 분리)
```

**목록 뷰 (List)**
- GPS 기반 현재 위치 반경 1km / 3km / 5km 선택
- 거리 필터 (5km 이하 / 5~10km / 10km 이상)
- 루프코스 / 일반코스 필터
- 정렬 (가까운 순 / 인기 순 / 완주율 순)
- 키워드 검색

---

### 5.6 코스 도전 및 리더보드

**도전 플로우:**
1. 코스 상세에서 "도전 시작" 탭
2. 서버에서 `running_record` + `course_attempt` 동시 생성
3. 코스 경로를 배경으로 현재 위치 실시간 표시
4. 코스 이탈 감지 → 진동 + 경고 메시지
5. 완주 → 리더보드 화면으로 자동 이동

**리더보드 구조:**
- `RANK() OVER (ORDER BY duration_seconds ASC)` 윈도우 함수로 실시간 순위
- `verification_status = 'verified'`이고 `status = 'completed'`인 기록만 집계
- 자신의 PR(개인 최고 기록) 달성 시 별도 표시

---

### 5.7 AI 러닝 자세 분석

러닝 자세 분석은 **스마트폰 카메라로 촬영한 영상**을 오프라인으로 분석하는 기능이다. 서버 없이 기기 내에서 모든 처리가 이루어진다.

**분석 파이프라인:**

```
영상 파일 (촬영 or 갤러리)
    │
    ▼
MediaMetadataRetriever
    ├─ 초당 15프레임 추출 (최대 300프레임 / 50초 제한)
    └─ 해상도 자동 조정 (최대 640px, 비율 유지)
    │
    ▼
MediaPipe PoseLandmarker (VIDEO 모드)
    ├─ BlazePose 33개 관절 랜드마크 추출
    ├─ VIDEO 모드: 프레임 간 칼만 필터로 시간축 추적
    └─ 가시성(visibility) 0.30 미만 랜드마크 제외
    │
    ▼
PostureAngleCalculator
    ├─ 좌우 독립 계산 (running-form-analyzer 방식)
    ├─ Aspect Ratio 보정 (세로 영상 왜곡 교정)
    ├─ 12개 각도 계산:
    │   무릎 굴곡(L/R), 팔꿈치(L/R), 고관절 스윙(L/R)
    │   힙-발목 수직각(L/R), 정강이 각도(L/R), 팔 스윙(L/R)
    └─ 신뢰도 높은 쪽(left/right) 자동 선택
    │
    ▼
RunningFormMetricsPipeline
    ├─ 케이던스(SPM): 발목 Y좌표 진동 주기 분석
    ├─ 수직 진폭: 골반 Y좌표 최고-최저점 차이
    └─ One Euro Filter: 실시간 스무딩
    │
    ▼
PostureEvaluator + PostureRuleEngine
    ├─ 각 지표를 기준값과 비교해 점수 산출
    ├─ 카테고리별 피드백 생성 (무릎/상체/팔꿈치/고관절)
    └─ 종합 점수 + 등급 (A/B/C/D) + 전반 피드백
    │
    ▼
PostureAutoencoderInference (TensorFlow Lite)
    └─ 오토인코더 모델로 정상 자세와 비교 (MSE 이상치 감지)
```

**핵심 기술 선택 이유:**
- `RunningMode.VIDEO`: 프레임 간 칼만 필터로 관절 추적 안정성 향상
- Aspect Ratio 보정: 세로 영상(9:16)에서 각도 계산 시 X축 왜곡 보정
- 좌우 독립 계산: 두 다리를 별도 추적해 교차 혼동 방지
- On-Device 처리: 개인 영상을 서버에 전송하지 않아 프라이버시 보호

**분석 결과 화면:**
- 동영상 오버레이 재생 (스켈레톤 선 + 각도 수치 표시)
- 종합 점수 원형 뱃지 (등급 A~D)
- 카테고리별 카드 (무릎 굴곡 / 상체 기울기 / 팔꿈치 / 고관절)
- 케이던스(SPM), 수직 진폭 참고 지표
- 면책 경고문 (의료적 판단 근거 사용 금지)

---

## 6. 부가 기능

### 6.1 러닝 통계

- **주간 통계**: 요일별 거리 막대 그래프
- **월간 통계**: 주차별 누적 거리
- **연간 통계**: 월별 총 거리/횟수
- **전체 통계**: 총 거리, 총 시간, 평균 페이스
- **Personal Records**: 5km, 10km, 하프마라톤(21.1km), 풀마라톤(42.2km) 개인 최고 기록

### 6.2 업적(Achievements) 시스템

서버에서 정의된 업적 기준(누적 거리, 런 횟수, 코스 완주 수 등)에 따라 달성 여부를 계산해 표시한다.

### 6.3 공유 이미지 생성

런 완료 후 결과 카드를 이미지로 생성해 SNS에 공유한다.
- 여러 가지 템플릿 중 선택
- 거리, 시간, 페이스, 날짜 자동 포함
- `ImageCaptureUtil`로 Composable을 Bitmap으로 변환
- 시스템 공유 시트로 공유

### 6.4 1km 스플릿

런 완료 후 상세 화면에서 1km 구간마다 페이스를 계산해 표시한다. `SplitCalculator`가 GPS 포인트 시퀀스를 분석해 각 1km 통과 시각을 역산한다.

### 6.5 러닝 리마인더 알림

사용자가 설정한 요일/시간에 정확한 알림을 보낸다.
- `AlarmManager.setExact()`로 정시 알림
- `BootReceiver`로 기기 재부팅 후에도 알림 복구
- `NotificationChannel` 분리 (러닝 추적용 / 리마인더용)

### 6.6 공식 코스 데이터

실제 GPS 기반 공식 코스 7개 (V14, V15 마이그레이션):

| 코스 | 출처 | 거리 |
|------|------|------|
| 서울 청계천 | OSM Way 368276771 | 8.7km |
| 서울 남산 순환도로 | OSM Way 357958296 | 7.5km |
| 서울 반포 한강공원 | OSM Way 418249072 | 5.0km |
| 서울 양재천 | OSM Way 26505520 | 9.5km |
| 부산 해운대 해변 | OSM Way 107531972 | 2.5km |
| 충주 강변 코스 10K | Samsung Health GPS 실측 | 9.96km |
| 충주 강변 코스 20K | Samsung Health GPS 실측 | 19.84km |

---

## 7. 데이터베이스 설계

### ERD (Entity-Relationship Diagram) 개요

```
users (사용자)
  │
  ├──< running_records (러닝 기록)
  │         └──< running_points (GPS 포인트)
  │
  ├──< courses (코스)
  │         ├──< course_points (코스 경로 포인트)
  │         ├──< course_ratings (평점)
  │         ├──< course_reports (신고)
  │         └──< course_favorites (즐겨찾기)
  │
  └──< course_attempts (코스 도전 기록)
            └── running_record (도전 시 생성된 런)
```

### 주요 테이블

#### users
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID (PK) | 사용자 ID |
| email | VARCHAR(255) UNIQUE | 이메일 |
| password_hash | VARCHAR(255) | BCrypt 해시 |
| nickname | VARCHAR(50) UNIQUE | 닉네임 |
| bio | TEXT | 소개글 |
| refresh_token_hash | VARCHAR(512) | Refresh Token 해시만 저장 |
| deleted_at | TIMESTAMPTZ | Soft Delete 타임스탬프 |

#### running_records
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID (PK) | 런 ID |
| user_id | UUID (FK) | 사용자 |
| status | VARCHAR(30) | in_progress / paused / completed / abandoned |
| started_at | TIMESTAMPTZ | 시작 시각 |
| distance_meters | FLOAT | 총 거리 (미터) |
| duration_seconds | INT | 순 운동 시간 (초) |
| avg_pace_seconds_per_km | INT | 평균 페이스 |
| path | geography(LineString,4326) | 전체 GPS 경로 |

#### courses
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID (PK) | 코스 ID |
| creator_id | UUID (FK) | 생성자 |
| status | VARCHAR(30) | draft / published / archived |
| distance_meters | FLOAT | 코스 거리 |
| is_loop | BOOLEAN | 루프 코스 여부 |
| start_location | geography(Point,4326) | 시작 좌표 |
| path | geography(LineString,4326) | 코스 경로 |
| difficulty | VARCHAR(20) | 난이도 (EASY/NORMAL/HARD) |
| slope_level | VARCHAR(20) | 경사도 |
| risk_level | VARCHAR(20) | 위험도 |
| attempt_count | INT | 도전 횟수 (캐시) |
| completion_count | INT | 완주 횟수 (캐시) |

#### course_attempts
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID (PK) | 도전 ID |
| course_id | UUID (FK) | 코스 |
| user_id | UUID (FK) | 도전자 |
| running_record_id | UUID (FK) | 연결된 런 기록 |
| status | VARCHAR(30) | in_progress / completed / abandoned |
| verification_status | VARCHAR(30) | pending / verified / rejected |
| duration_seconds | INT | 완주 소요 시간 (리더보드 기준) |

### 인덱스 전략

| 테이블 | 인덱스 | 목적 |
|--------|--------|------|
| courses | GiST(start_location) | ST_DWithin() 반경 탐색 O(log N) |
| course_attempts | B-tree(course_id, duration_seconds) WHERE status='completed' | 리더보드 정렬 |
| running_records | B-tree(user_id, started_at DESC) | 내 러닝 목록 |
| running_points | B-tree(running_record_id, sequence) | 경로 순서 조회 |

### 좌표 시스템

모든 GPS 좌표는 **WGS84 (EPSG:4326)** 기준을 사용한다.
- 단위: 경도(longitude), 위도(latitude) — 십진수 도(degree)
- PostGIS 타입: `geography(Point, 4326)`, `geography(LineString, 4326)`
- geography 타입 사용 이유: 구면 좌표계로 미터 단위 거리 계산 정확도 보장

---

## 8. 인프라 및 배포

### 서버 환경

| 항목 | 사양 |
|------|------|
| 클라우드 | Oracle Cloud Infrastructure (OCI) |
| IP | 40.233.98.156 |
| OS | Ubuntu |
| 런타임 | Java 17 (JVM) |
| DB | Docker 컨테이너 (postgis/postgis:15-3.4) |

### 배포 구성

```
GitHub (main 브랜치 push)
    │
    └─► GitHub Actions (CI/CD)
            ├─ ./gradlew build (JAR 빌드)
            ├─ SCP → 서버로 JAR 전송
            └─ systemctl restart runway-api
                    │
                    └─► Spring Boot JAR
                             ├─ 포트 8080 (HTTP)
                             └─ Flyway 자동 마이그레이션 실행
                                    │
                                    └─► Docker: runway-db (PostgreSQL + PostGIS)
                                                포트 5432
```

### 로컬 개발 환경

```bash
# 1. DB 실행
cd backend
docker-compose up -d

# 2. 서버 실행
export JWT_SECRET=local-development-secret
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. API 문서 확인
open http://localhost:8080/swagger-ui.html
```

### Flyway 마이그레이션 이력

| 버전 | 설명 |
|------|------|
| V1 | PostGIS, pgcrypto 확장 초기화 |
| V2~V7 | 핵심 테이블 생성 (users, running_records/points, courses/points, course_attempts) |
| V8~V10 | 보조 테이블 (신고, 평점, 즐겨찾기) |
| V11 | 코스 공개 메타데이터 컬럼 추가 |
| V12~V13 | 데모 코스 시딩 (임의 좌표 → 삭제됨) |
| V14 | OSM 실측 GPS 코스 5개 등록 (서울, 부산) |
| V15 | 충주 Samsung Health GPS 기록 기반 코스 2개 등록 |

---

## 9. 보안 설계

### 인증 방식 (JWT)

```
로그인 성공
    └─► Access Token (1시간, Bearer 헤더)
    └─► Refresh Token (2주, 서버 DB에 해시만 저장)

Access Token 만료 시 (Android 자동 처리)
    └─► OkHttp TokenAuthenticator가 가로챔
    └─► POST /api/auth/reissue 자동 호출
    └─► 새 Access Token 발급 후 원래 요청 재시도

Refresh Token 탈취 대비
    └─► DB에 평문 저장 금지 (BCrypt 해시만 저장)
    └─► 로그아웃 시 DB에서 해시 삭제
    └─► 재발급 성공 시 Refresh Token도 갱신 (Rotation)
```

### 데이터 보호

- **비밀번호**: BCrypt (cost factor 10) 해싱
- **Refresh Token**: BCrypt 해싱 후 저장 (탈취 시 사용 불가)
- **Privacy Zone**: 코스 시작/종료 지점 자동 마스킹 (150m~1.1km)
- **Soft Delete**: 사용자 탈퇴 시 즉각 삭제가 아닌 논리 삭제 (복구 가능)

### API 인증 정책

- 공개 엔드포인트: `/api/auth/**` (로그인, 회원가입, 토큰 재발급)
- 인증 필수: 나머지 모든 `/api/**` 엔드포인트
- 소유자 확인: `course.isOwnedBy(userId)` 등 서비스 레이어에서 재검증

---

## 10. 전문 용어 해설

### 아키텍처 용어

| 용어 | 설명 |
|------|------|
| **Clean Architecture** | UI, 비즈니스 로직, 데이터 접근을 계층으로 분리하는 설계 패턴. 각 계층은 안쪽 계층만 알고 바깥을 모른다. 테스트 용이성과 변경 유연성이 핵심 목적. |
| **MVVM** | Model-View-ViewModel. UI(View)는 상태만 표시하고, ViewModel이 비즈니스 로직과 상태를 관리하며, Model이 데이터를 제공하는 패턴. |
| **Repository 패턴** | 데이터 소스(서버 API, 로컬 DB)를 추상화하는 계층. ViewModel은 데이터가 어디서 오는지 몰라도 된다. |
| **DI (Dependency Injection)** | 의존성 주입. 객체가 필요로 하는 다른 객체를 직접 생성하지 않고 외부에서 제공받는 패턴. Hilt가 담당. |
| **DTO (Data Transfer Object)** | 계층 간 데이터 전달용 순수 데이터 클래스. 비즈니스 로직 없음. |
| **Soft Delete** | 데이터를 실제로 삭제하지 않고 `deleted_at` 타임스탬프만 기록하는 방식. 데이터 복구 가능, 연결된 데이터 무결성 유지. |
| **Layered Architecture** | 프레젠테이션 → 비즈니스 → 데이터 접근 순서로 단방향 호출만 허용하는 백엔드 구조. |

### Android 기술 용어

| 용어 | 설명 |
|------|------|
| **Jetpack Compose** | 코드로 UI를 선언적으로 작성하는 Android 공식 UI 프레임워크. XML 레이아웃 파일 없이 Kotlin 함수로 UI를 구성. |
| **Composable** | Jetpack Compose에서 UI를 구성하는 함수 단위. `@Composable` 어노테이션이 붙은 함수. |
| **ViewModel** | UI 상태(State)를 보관하고 비즈니스 로직을 처리하는 클래스. 화면 회전 등 구성 변경에도 데이터가 유지됨. |
| **StateFlow** | Kotlin 코루틴 기반 상태 홀더. 현재 값을 항상 가지며, 값 변경 시 구독자에게 자동 통지. |
| **ForegroundService** | 사용자에게 알림을 보여주며 백그라운드에서 지속적으로 실행되는 Android 서비스. GPS 추적에 필수. |
| **Room** | Android 공식 로컬 데이터베이스 라이브러리. SQLite 위에 ORM 계층 제공. |
| **DataStore** | SharedPreferences를 대체하는 Android 키-값 저장소. 코루틴/Flow 기반, 타입 안전. |
| **Hilt** | Google이 만든 Android 의존성 주입 프레임워크. Dagger 기반, 보일러플레이트 최소화. |
| **Retrofit** | HTTP API 호출을 인터페이스로 정의하는 Android 네트워크 라이브러리. |
| **OkHttp Interceptor** | HTTP 요청/응답을 가로채 처리하는 미들웨어. JWT 토큰 자동 첨부, 자동 갱신에 사용. |
| **Navigation Compose** | Jetpack Compose용 화면 전환(네비게이션) 라이브러리. BackStack, DeepLink 관리. |
| **BackStack** | 화면 전환 이력 스택. 뒤로 가기 버튼 동작의 기반. |
| **Coil** | Kotlin 코루틴 기반 Android 이미지 로딩 라이브러리. |

### 백엔드/DB 기술 용어

| 용어 | 설명 |
|------|------|
| **Spring Boot** | Java 기반 웹 애플리케이션 프레임워크. 자동 설정과 내장 서버로 빠른 개발 가능. |
| **JPA (Java Persistence API)** | Java 객체와 DB 테이블을 매핑하는 표준 인터페이스. Hibernate가 구현체. |
| **Spring Security** | Spring 기반 인증/인가 프레임워크. JWT 필터, 접근 권한 설정 담당. |
| **JWT (JSON Web Token)** | 서버가 발급하는 자가 검증 가능한 토큰. 세션 없이도 인증 상태 유지 가능. |
| **Access Token** | 짧은 유효 기간(1시간)의 API 접근 토큰. 매 요청 헤더에 포함. |
| **Refresh Token** | 긴 유효 기간(2주)의 토큰 갱신 전용 토큰. Access Token 만료 시 재발급에 사용. |
| **BCrypt** | 비밀번호 해싱 알고리즘. 느린 해싱으로 무차별 대입 공격 방어. |
| **Flyway** | DB 스키마 버전 관리 도구. SQL 마이그레이션 파일을 순서대로 자동 실행. |
| **PostGIS** | PostgreSQL의 지리정보 확장. 좌표, 경로, 공간 연산(거리, 반경 검색) 지원. |
| **geography 타입** | PostGIS의 WGS84 기반 지리 타입. 구면 좌표계로 실제 지구 거리 계산 정확. |
| **ST_DWithin** | PostGIS 함수. 두 지점이 지정된 거리 이내인지 판별. 반경 탐색의 핵심. |
| **ST_MakeLine** | 여러 Point 좌표를 하나의 LineString으로 연결하는 PostGIS 함수. |
| **LineString** | 순서가 있는 좌표의 연속으로 구성된 선. GPS 경로 저장에 사용. |
| **WGS84 (EPSG:4326)** | 전 세계 표준 좌표계. GPS가 사용하는 경위도 시스템. |
| **HikariCP** | Java의 고성능 DB 커넥션 풀 라이브러리. Spring Boot 기본 내장. |
| **RANK() OVER** | SQL 윈도우 함수. 전체 결과에서 각 행의 순위를 계산. 리더보드에 사용. |
| **Swagger / OpenAPI** | REST API 명세를 자동으로 문서화하고 테스트 UI를 제공하는 도구. |

### AI/ML 기술 용어

| 용어 | 설명 |
|------|------|
| **MediaPipe** | Google의 ML 파이프라인 프레임워크. 실시간 포즈 인식, 손 추적 등 제공. |
| **BlazePose** | MediaPipe의 인체 포즈 감지 모델. 33개 관절(랜드마크) 3D 좌표 추출. |
| **Landmark** | 관절 포인트. 무릎, 팔꿈치, 골반 등 신체 핵심 지점의 좌표. |
| **Kalman Filter** | 잡음이 있는 측정값을 기반으로 실제 상태를 추정하는 수학적 필터. 관절 좌표 안정화에 사용. |
| **One Euro Filter** | 실시간 데이터 스무딩 필터. 느린 움직임은 더 많이 스무딩, 빠른 움직임은 덜 스무딩. |
| **Autoencoder** | 비지도학습 신경망. 입력을 압축 후 복원하며 정상 패턴 학습. 이상 자세 감지에 활용. |
| **MSE (Mean Squared Error)** | 평균 제곱 오차. 예측값과 실제값 차이의 제곱 평균. 오토인코더의 이상치 판별 기준. |
| **TensorFlow Lite** | 모바일 기기에서 실행 가능한 경량 ML 모델 런타임. |
| **Aspect Ratio 보정** | 세로 촬영 영상에서 X/Y 축 스케일 차이로 인한 각도 왜곡을 수학적으로 보정하는 처리. |
| **RDP (Ramer-Douglas-Peucker)** | 수많은 좌표 포인트에서 핵심 포인트만 남기는 경로 단순화 알고리즘. GPS 저장 효율화에 사용. |

### GPS/지도 용어

| 용어 | 설명 |
|------|------|
| **FusedLocationProvider** | Google Play Services의 GPS + 네트워크 + 가속도계를 융합한 위치 제공자. 배터리 효율 최적화. |
| **GPX (GPS Exchange Format)** | GPS 궤적 데이터를 저장하는 XML 기반 파일 형식. 러닝 앱 간 경로 공유에 사용. |
| **케이던스 (Cadence, SPM)** | 러닝 중 분당 걸음 수(Steps Per Minute). 엘리트 러너는 보통 170~190 SPM. |
| **페이스 (Pace)** | 1km를 달리는 데 걸리는 시간 (분/km). 낮을수록 빠름. |
| **수직 진폭 (Vertical Oscillation)** | 러닝 중 상하 바운싱 크기(cm). 적을수록 에너지 효율이 좋다. |
| **루프 코스** | 시작점과 종료점이 같은 원형 코스. |
| **Privacy Zone** | 코스 시작/종료 지점 주변을 마스킹하는 개인정보 보호 구간. |
| **Cluster (마커 클러스터링)** | 지도에서 가까운 마커들을 그룹화해 하나로 표시하는 기법. 줌아웃 시 가독성 향상. |
| **ST_Segmentize** | PostGIS 함수. LineString을 지정 거리 간격으로 분할해 좌표 밀도를 높임. |
| **OpenStreetMap (OSM)** | 오픈소스 세계 지도 프로젝트. 전 세계 도로, 경로 데이터를 무료로 제공. |

---

*이 문서는 프로젝트의 현재 상태(2026-06-07)를 기준으로 작성됐습니다. 기능 추가/변경 시 업데이트가 필요합니다.*
