# CLAUDE.md

## 1. 프로젝트 개요

**RunWay** — GPS 기반 러닝 코스 공유 및 경쟁 플랫폼.

- 사용자가 GPS로 러닝 경로를 기록하고, 이를 코스로 등록해 공개한다.
- 다른 사용자는 주변 코스를 탐색하고, 해당 코스에 도전하여 완주 기록을 남긴다.
- 코스별 리더보드에서 완주 기록을 비교한다.
- Galaxy Watch 6용 Wear OS 앱으로 폰 없이 독립 실행 가능.
- MediaPipe + TensorFlow Lite 기반 On-Device AI 러닝 자세 분석 제공.
- 핵심 플로우: `런 시작 → GPS 수집 → 런 완료 → 코스 생성 → 코스 탐색 → 코스 도전 → 리더보드`

---

## 2. Repository 구조

```
RunWay/
├── backend/       # Spring Boot 백엔드
├── android/       # Kotlin Android 클라이언트
├── wear-os/       # Kotlin Wear OS 클라이언트 (Galaxy Watch 6)
├── docs/          # 기술 명세서, API 명세서, 로드맵
├── design/        # Lovable 기반 UI 프로토타입
├── sample/        # 테스트용 Samsung Health GPX 파일
└── CLAUDE.md
```

- `backend/` — Spring Boot API 서버. Phase 1 MVP 완료 + 확장 API 추가. 모든 백엔드 코드는 이 디렉토리 안에서만 다룬다.
- `android/` — Kotlin Android 클라이언트. 인증·러닝·코스·도전·AI 자세 분석 전체 구현 완료.
- `wear-os/` — Galaxy Watch 6용 Wear OS 앱. 독립 실행, 코스 도전, 폰 연동 구현 완료.
- `docs/` — 설계 문서. 구현 전 반드시 참고해야 한다.
- `design/` — Lovable 기반 React/Vite UI 프로토타입. Android UI 설계 참고용.

---

## 3. Source of Truth 문서

- `docs/technical-specification.md` — 데이터베이스 설계, 기능 범위, 구현 규칙
- `docs/api-specification.md` — REST API 전체 스펙 (request/response body, error code, sequence)

**구현 변경 전에 반드시 이 두 문서를 읽는다.**
문서와 충돌하는 설계 변경이 필요하면 구현 전에 반드시 먼저 확인을 받는다.

---

## 4. 개발 원칙

- 모든 기능을 한 번에 구현하지 않는다. Phase 순서를 지킨다.
- Phase 1 구현 중 Phase 2 기능을 미리 추가하지 않는다.
- backend와 android는 독립적으로 관리한다. 공용 코드를 디렉토리 간에 직접 공유하지 않는다.
- 패키지 이름, 디렉토리 구조 변경은 먼저 확인을 받는다.
- 파일 생성/수정 시 항상 전체 파일 경로를 명시한다.
- 작은 단위로 구현하고 검증한다. 부분 코드(생략 표시)는 사용하지 않는다.

---

## 5. 현재 Phase 및 구현 순서

### 완료된 작업

**Backend (Phase 1 완료 + 확장)**

| 단계 | 작업 |
|------|------|
| Phase 1-1A | Spring Boot 초기화, Docker PostgreSQL + PostGIS, Flyway V1~V7, 공통 응답/예외 구조 |
| Phase 1-1B | User Entity, UserRepository, enum 클래스, Security/JWT 기반 클래스 |
| Phase 1-2 | Auth API — 회원가입, 로그인, 토큰 재발급, 로그아웃 |
| Phase 1-3 | Running API — 런 시작/일시정지/재개/완료/중단, GPS 포인트 배치 저장, 기록 조회 |
| Phase 1-4 | Course API — 코스 생성, 수정, 공개, 인근 코스 탐색, 코스 상세 조회 |
| Phase 1-5 | Course Attempt API — 코스 시도 시작/완주/포기, 리더보드 조회 |
| 확장 | 프로필 이미지 업로드, 러닝 통계/최고기록, 업적, 코스 평점/신고/즐겨찾기, Privacy Zone |

**Android (Phase B — 완료)**

| 단계 | 작업 |
|------|------|
| Phase B-3 | 인증 UI — LoginScreen, SignupScreen, Hilt + Retrofit + DataStore 연동 |
| Phase B-4 | 홈 + 메인 네비게이션 — MainScaffold (BottomNav 5탭), HomeScreen |
| Phase B-5 | 러닝 추적 UI — RunningTrackingScreen, RunResultScreen, RouteMapView |
| Phase B-6 | Running API 백엔드 연동 — GPS 배치 전송, pause/resume/finish API |
| Phase B-7~B-10 | 코스 생성, 주변 탐색, 코스 상세, 코스 도전 + 리더보드 |
| Phase B-11~B-14 | Google Maps 연동, 자세 분석(MediaPipe + TFLite), 프로필·통계·업적 |
| Phase B-15 | GPS 필터링, ForegroundService, 크래시 복구(PendingPointQueue + TrackingSessionStore) |
| Phase B-16 | 코스 도전 지도 오버레이, 실제 GPS 기반 Nearby Discovery, HomeScreen 연동 |

**Wear OS (완료)**

| 단계 | 작업 |
|------|------|
| Wear Phase 1 | 기본 러닝 추적 — HealthServices, FusedLocationProvider, ForegroundService |
| Wear Phase 2 | 코스 도전 — DataLayer 동기화, 경로 투영 진행도, 이탈 감지/자동 일시정지 |
| Wear Phase 3 | AOD(Ambient Mode), 오프라인 런 저장 및 폰 업로드, 음성 안내 |

### 다음 작업

| 단계 | 작업 |
|------|------|
| B-17 | Privacy Zone 좌표 마스킹, Backend 데이터 무결성 방어, Global Leaderboard 탭 정리 |
| B-18 | Auto-Pause, 1km Splits, km 마일스톤 알림 |
| B-19 | My Courses 화면, 온보딩, 코스 신고 |
| B-20 | Personal Records, 코스 키워드 검색, 페이스 차트 |

---

## 6. Backend 규칙

- 모든 backend 코드는 `backend/` 하위에만 구현한다.
- 메인 패키지: `com.runway`
- 언어: Java 17
- 프레임워크: Spring Boot 3.x
- 빌드: Gradle
- DB: PostgreSQL 15 + PostGIS 3.x
- 마이그레이션: Flyway
- ORM: Spring Data JPA + Hibernate Spatial
- 인증: Spring Security + JWT

### 코드 규칙

- 모든 API 응답은 `ApiResponse<T>`를 사용한다.
- 애플리케이션 에러는 `RunwayException`과 `ErrorCode`를 사용한다.
- `passwordHash`, `refreshTokenHash`는 API 응답에 직접 노출하지 않는다.
- refresh token은 hash만 저장한다. 평문은 절대 DB에 저장하지 않는다.
- PK는 UUID를 사용한다 (`gen_random_uuid()`).
- timestamp는 `Instant` 또는 `OffsetDateTime`으로 매핑한다 (`TIMESTAMPTZ` 대응).
- `geography` 컬럼은 `@Column(columnDefinition = "geography(Point,4326)")` 형식으로 명시한다.
- Enum 매핑 시 DB status 값이 소문자(`in_progress`, `completed`, `verified` 등)임에 주의한다. Java enum 상수가 대문자(`IN_PROGRESS`)인 경우 `@Enumerated(EnumType.STRING)`을 사용하면 DB의 `CHECK constraint`와 충돌한다. 이 경우 `AttributeConverter`나 다른 명시적 매핑 전략을 사용한다.

---

## 7. Database 규칙

- 이미 적용된 Flyway 마이그레이션 파일은 수정하지 않는다.
- 새로운 스키마 변경은 반드시 새 `V{번호}__{설명}.sql` 파일로 추가한다.
- UUID 생성은 `gen_random_uuid()` (`pgcrypto`)를 사용한다. `uuid-ossp`는 사용하지 않는다.
- Phase 1에서 PostgreSQL native enum 타입을 사용하지 않는다. `VARCHAR(30) + CHECK constraint`를 사용한다.
- 좌표 타입: `geography(Point, 4326)`, `geography(LineString, 4326)` (EPSG:4326, WGS84)
- GiST 인덱스는 JPA 어노테이션이 아닌 Flyway SQL에서 직접 생성한다.

### Phase 1 테이블

`users`, `running_records`, `running_points`, `courses`, `course_points`, `course_attempts`

### Phase 2에서 추가할 테이블 (Phase 1에서 생성하지 않는다)

`course_usage_stats`, `certification_images`, `route_candidates`, `route_analyses`

### 주요 인덱스 (설계 요약)

| 테이블 | 인덱스 | 목적 |
|--------|--------|------|
| `courses` | GiST `(start_location)` | `ST_DWithin()` 인근 코스 탐색 |
| `course_attempts` | partial `(course_id, duration_seconds)` WHERE `status='completed' AND verification_status='verified'` | 리더보드 집계 |
| `running_records` | B-tree `(user_id, started_at DESC)` | 내 러닝 기록 목록 |
| `running_points` | B-tree `(running_record_id, sequence)` | 경로 순서 조회 |

---

## 8. API 규칙

전체 스펙은 `docs/api-specification.md`를 따른다.

- 모든 API는 `/api` prefix를 사용한다.
- 인증이 필요한 API: `Authorization: Bearer {accessToken}` header 사용
- 시간 형식: ISO-8601 (`"2026-05-13T08:30:00Z"`)
- 좌표 형식: 클라이언트는 `latitude`, `longitude`로 전달. 서버 내부에서는 PostGIS `geography` 타입 사용
- 목록 조회: `page` (기본 0), `size` (기본 20) query parameter 사용

### 공통 응답 형식

```json
// 성공
{ "success": true, "message": "요청이 성공했습니다.", "data": {} }

// 에러
{ "success": false, "message": "에러 메시지", "errorCode": "ERROR_CODE" }
```

### Phase 1 API 목록

| 그룹 | Method | URI |
|------|--------|-----|
| **Auth** | POST | `/api/auth/signup` |
| | POST | `/api/auth/login` |
| | POST | `/api/auth/reissue` |
| | POST | `/api/auth/logout` |
| **User** | GET | `/api/users/me` |
| | PUT | `/api/users/me` |
| | DELETE | `/api/users/me` |
| **Running** | POST | `/api/runs/start` |
| | POST | `/api/runs/{runId}/points` |
| | POST | `/api/runs/{runId}/pause` |
| | POST | `/api/runs/{runId}/resume` |
| | POST | `/api/runs/{runId}/finish` |
| | POST | `/api/runs/{runId}/abandon` |
| | GET | `/api/runs/me` |
| | GET | `/api/runs/{runId}` |
| | DELETE | `/api/runs/{runId}` |
| **Course** | POST | `/api/courses/from-run/{runId}` |
| | GET | `/api/courses/nearby` |
| | GET | `/api/courses/me` |
| | GET | `/api/courses/{courseId}` |
| | GET | `/api/courses/{courseId}/points` |
| | PUT | `/api/courses/{courseId}` |
| | PATCH | `/api/courses/{courseId}/publish` |
| | PATCH | `/api/courses/{courseId}/archive` |
| **Course Attempt** | POST | `/api/courses/{courseId}/attempts/start` |
| | POST | `/api/course-attempts/{attemptId}/finish` |
| | POST | `/api/course-attempts/{attemptId}/abandon` |
| | GET | `/api/courses/{courseId}/leaderboard` |
| | GET | `/api/courses/{courseId}/attempts/me` |

명세서에 없는 API는 먼저 확인을 받고 구현한다.

### Phase 1 핵심 설계 결정

- `running_records`에 `course_id`를 직접 저장하지 않는다. 코스와의 연결은 `course_attempts.running_record_id`로 표현한다.
- 리더보드는 별도 ranking 테이블 없이 `course_attempts`에서 `RANK() OVER`로 집계한다.
- 완주 시 `verification_status = 'verified'`를 동일 트랜잭션에서 자동 설정한다 (Phase 1 단순화).
- 코스 시도 시작 시 `running_records` 행을 먼저 생성하고, `course_attempts`를 생성하여 연결한다.

---

## 9. Android 규칙

- Android 코드는 `android/` 하위에만 구현한다.
- 구현 전에 반드시 기존 코드와 `docs/api-specification.md`를 확인한다.

### 현재 스택

- Kotlin 2.0.21, Jetpack Compose (Material 3), Navigation Compose 2.8.4
- Retrofit2 2.11.0, OkHttp 4.12.0, Gson
- Room 2.6.1, DataStore Preferences 1.1.1, Hilt 2.52 (KSP)
- Coroutines + Flow, `NetworkResult<T>`, `safeApiCall`
- GPS: Fused Location Provider + ForegroundService (`RunTrackingService`)
- 지도: Google Maps SDK 19.0.0 + Maps Compose 4.4.1
- AI/ML: MediaPipe Pose Landmarker 0.10.35, TensorFlow Lite
- 카메라: CameraX 1.4.1, Media3 ExoPlayer 1.5.1
- 이미지: Coil 2.7.0
- Wear OS 연동: Play Services Wearable 19.0.0

### Android 연동 핵심 규칙

- GPS 포인트는 5초마다 batch 전송. 단건 실시간 전송하지 않는다.
- 네트워크 실패 시 Room `PendingPointQueue`에 보관 후 재전송한다.
- JWT Access Token 만료 시 `TokenAuthenticator`(OkHttp)에서 자동 재발급한다.
- 코스 기반 러닝 시 서버에서 `runningRecordId`와 `courseAttemptId`를 함께 반환한다. GPS 포인트 저장은 `runningRecordId`로, 완주/포기 처리는 `courseAttemptId`로 한다.
- 앱 강제 종료 후 재시작 시 `TrackingSessionStore`(DataStore)로 런 상태를 복구한다.

---

## 9-1. Wear OS 규칙

- Wear OS 코드는 `wear-os/` 하위에만 구현한다.
- android/와 wear-os/ 간 코드를 직접 공유하지 않는다. DataLayer(MessageClient, DataClient)로 통신한다.

### 현재 스택

- Kotlin, Jetpack Compose Wear Material 3
- HealthServices (`ExerciseClient`) + FusedLocationProvider (듀얼 GPS)
- Wearable DataLayer (MessageClient + DataClient)
- Android TextToSpeech (오프라인 한국어 음성)
- `AmbientLifecycleObserver` (AOD)
- `RunForegroundService` (FOREGROUND_SERVICE_TYPE_LOCATION)

### Wear OS 핵심 규칙

- 코스 동기화는 폰에서 DataClient로 워치에 오프라인 저장 (`OfflineCourseStore`).
- 경로 이탈 감지 기준: 20m. 이탈 시 자동 일시정지 + FLP 복귀 모니터 시작.
- 수동 재개는 경로 이탈 중 차단한다.
- 완주 조건: 진행도 100% AND 종착점 30m 이내.
- 폰 미연결 시 런 데이터를 `PendingWatchRunStore`에 저장, 연결 시 자동 업로드.

---

## 10. Git 및 Repository 위생

커밋해서는 안 되는 파일:

- `.DS_Store` — macOS 메타데이터
- `.idea/` — IDE 로컬 설정
- `.claude/` — Claude Code 로컬 설정
- `application-local.yml` — 로컬 DB/JWT 설정
- `.env`, `*.env` — 환경변수/시크릿
- `build/`, `.gradle/` — 빌드 산출물

추가 규칙:

- 중첩 Git 레포지토리를 만들지 않는다. `backend/.git`은 존재해서는 안 된다.
- 명시적으로 요청받지 않으면 `git push`를 실행하지 않는다.

---

## 11. 로컬 실행 명령어

### Backend 실행

```bash
# DB 실행
cd backend
docker-compose up -d

# 서버 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

`application.yml`에서 `JWT_SECRET`을 환경변수로 요구하는 경우, 다음 두 방법 중 하나를 사용한다.

방법 1 — 환경변수로 직접 설정:

```bash
export JWT_SECRET=local-development-secret-must-be-long-enough
./gradlew bootRun --args='--spring.profiles.active=local'
```

방법 2 — `application-local.yml`에 로컬 값 정의 (Git에 커밋하지 않는다):

```yaml
jwt:
  secret: local-development-secret-must-be-long-enough
```

### DB 확인

```bash
# 테이블 목록
docker exec -it runway-db psql -U runway -d runway -c '\dt'

# Flyway 마이그레이션 이력
docker exec -it runway-db psql -U runway -d runway \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

### Swagger UI

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/swagger-ui/index.html
```

---

## 12. 커뮤니케이션 규칙

- 설명은 한국어로 작성한다. 단, technical terms, file names, directory names, package names, API paths, table names, column names, commands는 영어 그대로 유지한다.
- 구현 전에 변경될 파일 목록과 주요 변경 사항을 먼저 요약한다.
- 구현 후에는 다음 내용을 함께 제공한다:
  1. 생성/수정된 파일 목록
  2. 실행할 명령어
  3. 검증 방법
  4. 주의사항이나 경고

---

## 13. Git Strategy

이 프로젝트는 경량화된 GitHub Flow를 사용한다. backend, android, docs, design이 단일 repository에서 관리된다.

### 기본 원칙

- `main` 브랜치는 항상 빌드 가능하고 안정적인 상태를 유지한다.
- 구현 작업은 반드시 feature 브랜치에서 시작한다. 명시적으로 요청받지 않으면 `main`에 직접 커밋하거나 push하지 않는다.
- 명시적으로 요청받지 않으면 `git push`를 실행하지 않는다.
- 구현 완료 후에는 `git status`, 변경 파일 목록, 빌드 결과, 권장 커밋 메시지를 함께 제공한다.

### 브랜치 명명 규칙

| 접두사 | 용도 |
|--------|------|
| `feature/backend-*` | 백엔드 기능 구현 |
| `feature/android-*` | Android 기능 구현 |
| `fix/*` | 버그 수정 |
| `docs/*` | 문서 업데이트 |
| `chore/*` | 설정 변경 또는 정리 작업 |

**예시:**

- `feature/android-course-create`
- `feature/android-discover`
- `feature/android-course-detail`
- `feature/android-attempt-leaderboard`
- `fix/android-login-error`
- `fix/backend-jwt-config`
- `docs/update-readme`
- `chore/gitignore-cleanup`

### 커밋 메시지 형식

Conventional Commits를 사용한다.

```
type(scope): message
```

허용되는 type:

| type | 용도 |
|------|------|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `chore` | 설정, 빌드, 의존성 변경 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `test` | 테스트 추가 또는 수정 |
| `style` | 포맷팅, 공백 등 (로직 변경 없음) |

**예시:**

- `feat(android): connect course creation from run result`
- `feat(android): implement nearby course discovery`
- `feat(backend): implement course leaderboard API`
- `fix(android): handle login error state`
- `docs(readme): update backend MVP status`
- `chore(git): update gitignore`

### 커밋 전 빌드 확인

**Backend:**

```bash
cd backend
./gradlew build
```

**Android:**

```bash
cd android
./gradlew assembleDebug
```

### 권장 작업 흐름

```bash
# 1. main 최신화
cd ~/Developer/RunWay
git checkout main
git pull origin main

# 2. feature 브랜치 생성
git checkout -b feature/android-course-create

# 3. 구현 후 상태 확인 및 커밋
git status
git add .
git commit -m "feat(android): connect course creation from run result"

# 4. 명시적으로 요청받은 경우에만 push
git push origin feature/android-course-create
```
