# RunWay — Technical Specification

> 작성일: 2026-05-13  
> 상태: Phase 1 MVP 설계 확정

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [MVP Phase 계획](#2-mvp-phase-계획)
3. [기술 스택](#3-기술-스택)
4. [MVP Phase 1 기능 범위](#4-mvp-phase-1-기능-범위)
5. [데이터베이스 설계 요약](#5-데이터베이스-설계-요약)
6. [Entity Relationship 요약](#6-entity-relationship-요약)
7. [Enum 정의](#7-enum-정의)
8. [핵심 설계 결정 사항](#8-핵심-설계-결정-사항)
9. [API 설계 방향](#9-api-설계-방향)
10. [개발 순서](#10-개발-순서)
11. [구현 규칙 (Claude Code 참고용)](#11-구현-규칙-claude-code-참고용)

---

## 1. 프로젝트 개요

### 프로젝트명
**RunWay**

### 프로젝트 방향
러닝을 단순한 운동 기록을 넘어, 코스 기반의 도전과 경쟁으로 확장하는 모바일 러닝 앱.  
사용자는 자신의 러닝 경로를 코스로 등록하고, 다른 사용자들은 그 코스에 도전하며 기록을 남긴다.

### 핵심 개념
- 사용자가 직접 달린 경로를 **코스(course)**로 저장하고 공개한다.
- 다른 사용자들이 해당 코스를 선택해 도전할 수 있다.
- 코스별 **리더보드**를 통해 완주 기록을 비교한다.
- GPS 기반으로 현재 위치 주변의 코스를 탐색할 수 있다.

### 핵심 플로우
```
런 시작 → GPS 포인트 수집 → 런 완료 → 러닝 기록 저장
    └→ 코스 생성 (선택) → 코스 공개
                              ↓
다른 사용자가 인근 코스 탐색 → 코스 선택 → 코스 기반 런 시작
    → 완주 또는 포기 → course_attempts 기록 → 리더보드 반영
```

### 주요 우선순위
1. 러닝 기록의 정확한 GPS 추적 및 저장
2. 코스 등록과 인근 코스 탐색의 안정적인 동작
3. 코스별 리더보드의 단순하고 정확한 집계
4. Spring Boot + Android Kotlin 기반의 실용적인 구현

---

## 2. MVP Phase 계획

### MVP Phase 1 — 핵심 기능

| 기능 | 설명 |
|---|---|
| 회원가입 / 로그인 | 이메일 기반 인증, JWT 발급 |
| 기본 프로필 | 닉네임, 프로필 이미지 |
| 러닝 활동 추적 | GPS 포인트 수집, 러닝 기록 저장 |
| 완료된 런 저장 | `running_records`, `running_points` 저장 |
| 코스 수동 생성 | 저장된 러닝 기록을 기반으로 코스 등록 |
| 인근 코스 탐색 | 현재 위치 기준 반경 내 공개 코스 조회 |
| 코스 기반 런 시작 | 코스를 선택하여 런 시작 |
| 코스 시도 기록 | 완주 / 포기 기록 저장 (`course_attempts`) |
| 코스 리더보드 | 코스별 완주 기록 기반 순위 표시 |

### MVP Phase 2 — 확장 기능

| 기능 | 설명 |
|---|---|
| GPS 경로 검증 | `verification_status` 필드를 활용한 경로 매칭 검증 |
| 완주 인증 이미지 | 완주 시 인증 이미지 생성 및 저장 (`certification_images`) |
| 반복 경로 분석 | 동일 경로 반복 감지 및 코스 자동 제안 |
| 코스 사용 통계 | 코스별 조회수, 평균 완주 시간 캐싱 (`course_usage_stats`) |
| 웨어러블 연동 | 심박수 등 외부 센서 데이터 수집 |

### 이후 확장 (Post-MVP)

| 기능 | 설명 |
|---|---|
| 소셜 기능 | 팔로우, 피드, 응원 |
| 그룹 런 | 실시간 위치 공유 기반 같이 달리기 |
| 코스 추천 알고리즘 | 유저 성향 기반 코스 추천 |

### Phase를 분리한 이유

- **GPS 경로 검증(Phase 2):** 구현 복잡도가 높고 Phase 1 MVP 동작에 필수가 아님. Phase 1에서는 완주 시 자동으로 `verified` 처리하여 리더보드를 운영한다.
- **완주 인증 이미지(Phase 2):** 이미지 생성 인프라(S3, 이미지 합성 로직)가 별도로 필요하며, 핵심 기능이 아님.
- **코스 사용 통계(Phase 2):** Phase 1 트래픽 규모에서 `course_attempts` 집계로 충분. 실제 성능 문제가 발생한 이후 도입.
- **소셜 기능(Post-MVP):** 소셜 그래프 설계는 독립적인 복잡도를 가짐. 핵심 러닝/코스 기능이 안정화된 이후 설계.

---

## 3. 기술 스택

### 프로젝트 구조
```
RunWay/
├── backend/       # Spring Boot 백엔드
├── android/       # Kotlin Android 앱
├── docs/          # 기술 문서
└── CLAUDE.md
```

### Android
| 항목 | 선택 |
|---|---|
| 언어 | Kotlin |
| 지도 | Google Maps SDK 또는 Kakao Map SDK |
| GPS 수집 | Android Location API / Fused Location Provider |
| HTTP 통신 | Retrofit2 + OkHttp |
| 직렬화 | kotlinx.serialization 또는 Gson |
| 의존성 주입 | Hilt |
| 비동기 처리 | Coroutines + Flow |
| 로컬 저장소 | DataStore (설정), Room (오프라인 캐시, Phase 2) |

### Backend
| 항목 | 선택 |
|---|---|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.x |
| ORM | Spring Data JPA + Hibernate Spatial |
| 빌드 도구 | Gradle |
| 인증 | JWT (Access Token + Refresh Token) |
| API 형식 | REST (JSON) |
| 문서화 | SpringDoc OpenAPI (Swagger UI) |

### Database
| 항목 | 선택 |
|---|---|
| RDBMS | PostgreSQL 15+ |
| 공간 확장 | PostGIS 3.x |
| 좌표계 | EPSG:4326 (WGS84) |
| 공간 타입 | `geography(Point, 4326)`, `geography(LineString, 4326)` |
| 마이그레이션 | Flyway |

### 인프라 (Phase 1 기준)
| 항목 | 선택 |
|---|---|
| 배포 환경 | 로컬 개발 우선, 이후 클라우드 검토 |
| 이미지 저장소 | S3 또는 동등한 오브젝트 스토리지 (Phase 2) |
| 컨테이너 | Docker Compose (로컬 PostgreSQL + PostGIS) |

---

## 4. MVP Phase 1 기능 범위

### 4-1. 사용자 계정 기능

- 이메일 + 비밀번호 회원가입
- 로그인 시 JWT Access Token + Refresh Token 발급
- Refresh Token으로 Access Token 재발급
- 닉네임, 프로필 이미지 URL, 자기소개 조회 및 수정
- 회원 탈퇴 (soft delete — `users.deleted_at` 설정)

### 4-2. 러닝 활동 추적

- 런 시작: `running_records` 생성, `status = 'in_progress'`
- GPS 포인트 전송: `running_points` 순서대로 저장 (`sequence` 기준)
- 런 일시 정지 / 재개: `status` 전환
- 런 완료: `status = 'completed'`, `ended_at` / `distance_meters` / `duration_seconds` 기록, `path` LineString 생성
- 런 중단: `status = 'abandoned'`
- 내 런 기록 목록 조회 (최신 순)
- 런 기록 상세 조회 (경로 포함)

### 4-3. 코스 수동 생성

- 완료된 러닝 기록을 선택하여 코스 생성
- `courses.source_record_id` → 원본 `running_records.id` 연결
- `running_points` 다운샘플링 → `course_points` 저장
- `courses.path` LineString 생성 (`ST_MakeLine()`)
- 코스 이름 (`name`), 설명 (`description`) 입력
- 코스 공개: `status = 'draft'` → `'published'`
- 코스 수정, 보관 처리 (`status = 'archived'`)
- 내가 만든 코스 목록 조회

### 4-4. 인근 코스 탐색

- 현재 위치(위도/경도)를 기준으로 반경 내 공개 코스 조회
- `ST_DWithin(courses.start_location, :point, :radius_meters)` 사용
- `courses.start_location`의 GiST 인덱스로 성능 확보
- 정렬 기준: 거리 가까운 순 (`ST_Distance`)
- 필터: 거리 범위 (`distance_meters`), 루프 여부 (`is_loop`)
- 코스 상세 조회 (경로, 완주 통계 포함)

### 4-5. 코스 시도 추적

**코스 시도 시작 플로우:**

```
1. Android가 Course Attempt 시작 API를 호출한다.
2. Backend가 running_records 행을 먼저 생성한다.
3. Backend가 course_attempts 행을 생성하고 running_record_id로 연결한다.
4. Backend가 runningRecordId와 courseAttemptId를 함께 반환한다.
5. Android는 runningRecordId를 사용하여 GPS 포인트를 전송한다.
6. Android는 courseAttemptId를 사용하여 완주 또는 포기를 처리한다.
```

**상태 전환:**
- 완주: `course_attempts.status = 'completed'`, `verification_status = 'verified'` 자동 설정 (동일 트랜잭션)
- 포기: `course_attempts.status = 'abandoned'`
- `courses.attempt_count` / `completion_count` 완주/포기 처리 시 동일 트랜잭션에서 업데이트
- 내 코스 시도 이력 조회

### 4-6. 코스 리더보드

- 코스별 완주 기록 기반 순위 조회
- 집계 기준: 유저별 최단 완주 시간(`MIN(duration_seconds)`)
- 대상: `status = 'completed' AND verification_status = 'verified'` 인 기록만 포함
- `RANK() OVER (ORDER BY MIN(duration_seconds) ASC)` 로 순위 계산
- 결과: 순위, 닉네임, 최단 완주 시간, 완주 횟수
- 최대 50위까지 표시 (Phase 1 기준)

---

## 5. 데이터베이스 설계 요약

### Phase 1 테이블

#### `users`
사용자 인증 및 기본 프로필.

| Column | Type | Nullable | Default |
|---|---|---|---|
| `id` | `UUID` | NOT NULL | `gen_random_uuid()` |
| `email` | `VARCHAR(255)` | NOT NULL | — |
| `password_hash` | `VARCHAR(255)` | NOT NULL | — |
| `nickname` | `VARCHAR(50)` | NOT NULL | — |
| `profile_image_url` | `TEXT` | NULL | — |
| `bio` | `TEXT` | NULL | — |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `NOW()` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `NOW()` |
| `deleted_at` | `TIMESTAMPTZ` | NULL | — |

- PK: `id`
- UNIQUE: `email`, `nickname`
- INDEX: `email`, `nickname`, `deleted_at`

---

#### `running_records`
개별 러닝 세션 기록. 코스 생성의 원본 소스.

| Column | Type | Nullable | Default |
|---|---|---|---|
| `id` | `UUID` | NOT NULL | `gen_random_uuid()` |
| `user_id` | `UUID` | NOT NULL | — |
| `status` | `running_record_status` | NOT NULL | `'in_progress'` |
| `started_at` | `TIMESTAMPTZ` | NOT NULL | — |
| `ended_at` | `TIMESTAMPTZ` | NULL | — |
| `distance_meters` | `FLOAT` | NOT NULL | `0` |
| `duration_seconds` | `INT` | NOT NULL | `0` |
| `avg_pace_seconds_per_km` | `INT` | NULL | — |
| `avg_heart_rate_bpm` | `INT` | NULL | — |
| `calories_burned` | `INT` | NULL | — |
| `path` | `geography(LineString, 4326)` | NULL | — |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `NOW()` |

- PK: `id`
- FK: `user_id` → `users.id` ON DELETE CASCADE
- INDEX: `(user_id, started_at DESC)`, `(status)`, GiST `(path)`
- **`course_id`를 직접 저장하지 않음.** 코스 기반 런은 `course_attempts.running_record_id`로 연결.

---

#### `running_points`
런 중 수집된 GPS 좌표 원본. 경로 시각화 및 `path` 생성에 사용.

| Column | Type | Nullable | Default |
|---|---|---|---|
| `id` | `UUID` | NOT NULL | `gen_random_uuid()` |
| `running_record_id` | `UUID` | NOT NULL | — |
| `sequence` | `INT` | NOT NULL | — |
| `location` | `geography(Point, 4326)` | NOT NULL | — |
| `altitude_meters` | `FLOAT` | NULL | — |
| `speed_mps` | `FLOAT` | NULL | — |
| `recorded_at` | `TIMESTAMPTZ` | NOT NULL | — |

- PK: `id`
- FK: `running_record_id` → `running_records.id` ON DELETE CASCADE
- UNIQUE: `(running_record_id, sequence)`
- INDEX: B-tree `(running_record_id, sequence)` — 경로 순서 조회

---

#### `courses`
사용자가 러닝 기록 기반으로 생성한 공개 코스.

| Column | Type | Nullable | Default |
|---|---|---|---|
| `id` | `UUID` | NOT NULL | `gen_random_uuid()` |
| `creator_id` | `UUID` | NOT NULL | — |
| `source_record_id` | `UUID` | NULL | — |
| `name` | `VARCHAR(100)` | NOT NULL | — |
| `description` | `TEXT` | NULL | — |
| `status` | `course_status` | NOT NULL | `'draft'` |
| `distance_meters` | `FLOAT` | NOT NULL | — |
| `is_loop` | `BOOLEAN` | NOT NULL | `FALSE` |
| `start_location` | `geography(Point, 4326)` | NOT NULL | — |
| `end_location` | `geography(Point, 4326)` | NOT NULL | — |
| `path` | `geography(LineString, 4326)` | NULL | — |
| `attempt_count` | `INT` | NOT NULL | `0` |
| `completion_count` | `INT` | NOT NULL | `0` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `NOW()` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `NOW()` |
| `deleted_at` | `TIMESTAMPTZ` | NULL | — |

- PK: `id`
- FK: `creator_id` → `users.id` ON DELETE RESTRICT
- FK: `source_record_id` → `running_records.id` ON DELETE SET NULL
- INDEX: GiST `(start_location)` — **인근 코스 탐색 핵심 인덱스**
- INDEX: B-tree `(creator_id)`, `(status)`, `(deleted_at)`
- `name`은 `VARCHAR(100)`. 코스 이름은 충분한 길이를 허용해야 함.

---

#### `course_points`
코스 경로를 구성하는 다운샘플링된 웨이포인트.

| Column | Type | Nullable | Default |
|---|---|---|---|
| `id` | `UUID` | NOT NULL | `gen_random_uuid()` |
| `course_id` | `UUID` | NOT NULL | — |
| `sequence` | `INT` | NOT NULL | — |
| `location` | `geography(Point, 4326)` | NOT NULL | — |
| `altitude_meters` | `FLOAT` | NULL | — |

- PK: `id`
- FK: `course_id` → `courses.id` ON DELETE CASCADE
- UNIQUE: `(course_id, sequence)`
- INDEX: B-tree `(course_id, sequence)` — 경로 순서 조회

---

#### `course_attempts`
코스 시도 및 완주 기록. **리더보드의 유일한 데이터 소스.**

| Column | Type | Nullable | Default |
|---|---|---|---|
| `id` | `UUID` | NOT NULL | `gen_random_uuid()` |
| `course_id` | `UUID` | NOT NULL | — |
| `user_id` | `UUID` | NOT NULL | — |
| `running_record_id` | `UUID` | NULL | — |
| `status` | `course_attempt_status` | NOT NULL | `'in_progress'` |
| `verification_status` | `attempt_verification_status` | NOT NULL | `'pending'` |
| `started_at` | `TIMESTAMPTZ` | NOT NULL | — |
| `completed_at` | `TIMESTAMPTZ` | NULL | — |
| `duration_seconds` | `INT` | NULL | — |
| `distance_meters` | `FLOAT` | NULL | — |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `NOW()` |

- PK: `id`
- FK: `course_id` → `courses.id` ON DELETE CASCADE
- FK: `user_id` → `users.id` ON DELETE CASCADE
- FK: `running_record_id` → `running_records.id` ON DELETE SET NULL
- INDEX (partial): `(course_id, duration_seconds)` WHERE `status = 'completed' AND verification_status = 'verified'` — **리더보드 조회 핵심 인덱스**
- INDEX: B-tree `(course_id)`, `(user_id)`, `(course_id, user_id)`

---

### Phase 2 테이블 (설계 예정)

| 테이블 | 목적 |
|---|---|
| `course_usage_stats` | 코스별 조회수, 평균 완주 시간 등 집계 캐시 |
| `certification_images` | 완주 인증 이미지 저장 (`course_attempts`와 1:1) |
| `route_candidates` 또는 `route_analyses` | 반복 경로 분석 및 자동 코스 제안 데이터 |

---

## 6. Entity Relationship 요약

```
users (1) ──────────────────────────── (N) running_records
  │                                             │
  │                                        (1) ─┤
  │                                             │
  │                                        (N) running_points
  │
  ├──────────────────────── (N) courses
  │                              │  ↑
  │                              │  └── source_record_id ──(N:1)── running_records
  │                              │
  │                         (1) ─┤
  │                              │
  │                         (N) course_points
  │
  └──────────────────────── (N) course_attempts
                                 ├── course_id ──(N:1)── courses
                                 ├── user_id ──(N:1)── users
                                 └── running_record_id ──(N:1, optional)── running_records
```

| 관계 | 설명 |
|---|---|
| `users` 1:N `running_records` | 한 유저가 여러 러닝 기록 보유 |
| `running_records` 1:N `running_points` | 한 러닝 기록에 여러 GPS 포인트 |
| `users` 1:N `courses` | 한 유저가 여러 코스 생성 (`creator_id`) |
| `running_records` 1:N `courses` | 하나의 런 기록으로 여러 코스 생성 가능 (`source_record_id`, optional) |
| `courses` 1:N `course_points` | 한 코스에 여러 웨이포인트 |
| `users` 1:N `course_attempts` | 한 유저가 여러 코스 시도 |
| `courses` 1:N `course_attempts` | 한 코스에 여러 시도 기록 |
| `running_records` 1:N `course_attempts` | 코스 기반 런은 `running_record_id`로 연결 (optional) |

---

## 7. Enum 정의

### `running_record_status`
| 값 | 설명 |
|---|---|
| `in_progress` | 런 진행 중 |
| `paused` | 일시 정지 |
| `completed` | 정상 완료 |
| `abandoned` | 중단됨 |

### `course_status`
| 값 | 설명 |
|---|---|
| `draft` | 생성 중, 비공개 |
| `published` | 공개, 탐색 가능 |
| `archived` | 보관됨, 탐색 불가 |

### `course_attempt_status`
| 값 | 설명 |
|---|---|
| `in_progress` | 코스 도전 중 |
| `completed` | 완주 |
| `abandoned` | 중도 포기 |

### `attempt_verification_status`
| 값 | 설명 |
|---|---|
| `pending` | 완주했으나 검증 전 (Phase 2에서 검증 대기 상태로 사용) |
| `verified` | 검증 완료 (Phase 1: 완주 시 자동 설정) |
| `rejected` | 검증 실패 (Phase 2: GPS 경로 불일치 시 설정) |

**Phase 1 전환 규칙:**  
`course_attempt_status`가 `'completed'`로 전환될 때, `attempt_verification_status`를 `'verified'`로 함께 설정한다. 서비스 레이어에서 동일 트랜잭션으로 처리.

---

## 8. 핵심 설계 결정 사항

### PostgreSQL + PostGIS를 선택한 이유
GPS 좌표 기반의 인근 코스 탐색과 경로 저장에는 공간 데이터 처리가 필수다. PostGIS의 `geography` 타입은 위도/경도 좌표를 그대로 사용하면서 `ST_DWithin()` 등의 함수가 미터 단위로 동작하여 별도 단위 변환이 필요 없다. GiST 인덱스를 통한 공간 인덱싱으로 인근 코스 탐색 성능을 확보한다.

### 코스 리더보드를 `course_attempts`에서 집계하는 이유
별도의 ranking 테이블을 두면 `course_attempts`와 동기화 버그가 발생할 수 있다. `(course_id, duration_seconds) WHERE status = 'completed' AND verification_status = 'verified'` partial index가 있으면 코스당 수만 건의 완주 기록에서도 집계가 충분히 빠르다. 실제 성능 문제가 발생하면 그때 materialized view로 전환한다.

### `running_records`에 `course_id`를 직접 저장하지 않는 이유
하나의 런 기록이 항상 코스와 연결되는 것은 아니다. 자유 러닝(코스 없이 달리는 경우)은 `course_id`가 없으며, 코스 기반 런과 자유 러닝을 동일한 테이블로 관리한다. 코스와의 연결은 `course_attempts.running_record_id`로 표현하는 것이 관계를 명확히 분리한다.

### `course_usage_stats`를 Phase 2로 미룬 이유
Phase 1 트래픽 규모에서 탐색 화면의 코스 리스트는 `courses` 테이블의 denormalized 카운터(`attempt_count`, `completion_count`)로 충분히 표시할 수 있다. 조회수 캐싱, 평균 완주 시간 등 상세 통계는 실제 사용자 유입 이후 필요성을 확인하고 도입한다.

### `certification_images`를 Phase 2로 미룬 이유
완주 인증 이미지 생성은 S3 연동, 이미지 합성 로직, 비동기 처리 인프라가 필요하다. 이는 독립적인 기술적 복잡도를 가지며 Phase 1 핵심 기능과 분리된다. `course_attempts` 테이블과 1:1로 연결되는 구조는 이미 설계되어 있으므로 Phase 2 구현 시 추가가 쉽다.

### `running_points`와 `path` LineString을 모두 저장하는 이유
`running_points`는 원본 GPS 데이터로, 페이스 분석, 경로 재생, Phase 2 GPS 검증의 기반이 된다. `path` LineString은 지도 표시 및 공간 쿼리용 요약 데이터로, 매번 `running_points`에서 `ST_MakeLine()`을 실행하는 비용을 피하기 위해 런 완료 시 한 번 생성하여 저장한다.

### `courses.start_location`에 GiST 인덱스가 필요한 이유
`ST_DWithin()` 함수는 GiST 인덱스를 사용할 때만 효율적으로 동작한다. 일반 B-tree 인덱스로는 공간 범위 탐색을 지원하지 않아 전체 테이블 스캔이 발생한다. JPA `@Index` 어노테이션은 GiST 타입을 지원하지 않으므로 Flyway 마이그레이션 SQL에서 직접 생성해야 한다.

---

## 9. API 설계 방향

Phase 1에서 구현할 API 그룹. 상세 스펙(request/response body, 에러 코드)은 각 그룹 구현 시점에 별도로 정의한다.

### 공통 응답 형식

모든 API는 아래 형식으로 응답한다.

**성공 응답:**
```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

**에러 응답:**
```json
{
  "success": false,
  "message": "에러 메시지",
  "errorCode": "ERROR_CODE"
}
```

- `data`는 응답 내용에 따라 객체, 배열, 또는 `null`이 될 수 있다.
- `errorCode`는 클라이언트가 에러 유형을 식별하기 위한 고정 문자열이다 (예: `USER_NOT_FOUND`, `INVALID_TOKEN`).
- HTTP 상태 코드와 `errorCode`를 함께 사용하여 에러를 처리한다.

---

### Auth API
- 회원가입, 로그인, Access Token 재발급, 로그아웃
- JWT 기반 인증 (Bearer Token)

### User API
- 내 프로필 조회 / 수정
- 회원 탈퇴

### Running API
- 런 시작, 일시정지, 재개, 완료, 중단
- GPS 포인트 배치 전송
- 내 런 기록 목록 조회
- 런 기록 상세 조회 (경로 포함)

### Course API
- 코스 생성 (런 기록 기반)
- 코스 수정, 공개, 보관
- 내가 만든 코스 목록 조회
- 코스 상세 조회
- 인근 코스 탐색 (위도/경도 + 반경)

### Course Attempt API
- 코스 시도 시작
- 코스 시도 완주 / 포기
- 내 코스 시도 이력 조회
- 코스별 리더보드 조회

---

## 10. 개발 순서

### Phase 1 구현 순서

| 단계 | 작업 | 설명 |
|---|---|---|
| 1 | Backend 프로젝트 초기화 | Spring Boot 3.x, Gradle 설정, 패키지 구조 |
| 2 | PostgreSQL + PostGIS 설정 | Docker Compose, Flyway 마이그레이션, `CREATE EXTENSION postgis` |
| 3 | 공통 응답 / 예외 구조 | `ApiResponse<T>`, `GlobalExceptionHandler`, 에러 코드 정의 |
| 4 | User / Auth / JWT | 회원가입, 로그인, JWT 발급, Security 설정 |
| 5 | Running Record API | 런 시작/완료/중단, `running_records` CRUD |
| 6 | Running Point API | GPS 포인트 배치 저장, `path` LineString 생성 |
| 7 | Course Creation API | 코스 생성, `course_points` 저장, `courses.path` 생성 |
| 8 | Nearby Course API | `ST_DWithin()` 기반 인근 코스 탐색 |
| 9 | Course Attempt API | 코스 시도 시작/완주/포기, `courses` 카운터 업데이트 |
| 10 | Leaderboard API | 코스별 완주 기록 기반 순위 집계 |
| 11 | Android 연동 | Kotlin Android 클라이언트 구현, API 연동 |

---

## 11. 구현 규칙 (Claude Code 참고용)

### 기본 원칙
- 모든 기능을 한 번에 구현하지 않는다. 위 개발 순서의 단계별로 진행한다.
- 각 단계 시작 시 **전체 파일 경로와 전체 코드**를 제공한다.
- 부분 코드(일부만 보여주는 방식)는 사용하지 않는다.

### 프로젝트 분리 원칙
- Backend 코드는 반드시 `RunWay/backend/` 하위에 구현한다.
- Android 코드는 반드시 `RunWay/android/` 하위에 구현한다.
- 두 프로젝트는 독립적으로 관리한다. 공용 코드를 두 프로젝트 사이에 직접 공유하지 않는다.

### Backend 구현 규칙
- 패키지 구조는 `com.runway` 기준으로 기능별로 분리한다.
- Entity, Repository, Service, Controller를 기능 단위로 함께 구현한다.
- Flyway 마이그레이션 파일은 `V{번호}__{설명}.sql` 형식으로 작성한다.
- GiST 인덱스는 JPA 어노테이션이 아닌 Flyway SQL에서 직접 생성한다.
- `geography` 컬럼은 `@Column(columnDefinition = "geography(Point,4326)")` 형식으로 명시한다.
- Enum은 `@Enumerated(EnumType.STRING)`으로 매핑한다.
- 모든 timestamp는 `Instant` 또는 `OffsetDateTime`으로 매핑한다 (`TIMESTAMPTZ` 대응).

### Android 구현 규칙
- GPS 수집은 Background Service 또는 Foreground Service로 구현한다.
- 포인트 전송은 배치로 묶어 서버에 전송한다 (실시간 단건 전송 지양).
- JWT Access Token 만료 시 Refresh Token으로 자동 재발급하는 OkHttp Interceptor를 구현한다.

### Phase 경계 준수
- Phase 2 기능(`certification_images`, `course_usage_stats`, GPS 검증 로직 등)은 Phase 1 구현 중 미리 구현하지 않는다.
- `attempt_verification_status`는 Phase 1에서 완주 시 `'verified'`로 자동 설정하는 단순 로직만 구현한다.
- `running_records.path` GiST 인덱스는 생성하되, 경로 분석 쿼리는 Phase 2에서 추가한다.
