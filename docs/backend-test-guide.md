# RunWay Backend — 통합 테스트 가이드

> 작성일: 2026-05-13  
> 대상 브랜치: `main`  
> 기준 문서: `docs/technical-specification.md`, `docs/api-specification.md`

---

## 목차

1. [사전 준비](#1-사전-준비)
2. [서버 실행](#2-서버-실행)
3. [전체 통합 테스트 시나리오](#3-전체-통합-테스트-시나리오)
4. [PostgreSQL 데이터 검증](#4-postgresql-데이터-검증)
5. [각 단계별 기대 응답](#5-각-단계별-기대-응답)
6. [자주 발생하는 오류](#6-자주-발생하는-오류)
7. [Phase 1 설계 참고사항](#7-phase-1-설계-참고사항)

---

## 1. 사전 준비

### 필수 환경

| 항목 | 요구 사항 |
|---|---|
| Docker Desktop | 실행 중이어야 함 |
| Java | 17 이상 |
| 포트 | `5432` (PostgreSQL), `8080` (Spring Boot) |

### JWT_SECRET 설정

`application-local.yml` 파일을 `backend/src/main/resources/`에 생성한다. 이 파일은 Git에 커밋하지 않는다.

```yaml
# backend/src/main/resources/application-local.yml
jwt:
  secret: local-development-secret-must-be-at-least-32-characters-long
```

또는 환경변수로 직접 설정할 수 있다.

```bash
export JWT_SECRET=local-development-secret-must-be-at-least-32-characters-long
```

### 확인 명령어

```bash
# Docker Desktop 실행 확인
docker info

# Java 버전 확인
java -version

# 포트 충돌 확인 (아무 결과도 없어야 정상)
lsof -i :8080
lsof -i :5432
```

---

## 2. 서버 실행

### 1단계 — PostgreSQL + PostGIS 컨테이너 시작

```bash
cd backend
docker-compose up -d
```

컨테이너가 정상 기동되었는지 확인한다.

```bash
docker-compose ps
```

`runway-db` 컨테이너의 `State`가 `Up`이어야 한다.

### 2단계 — Flyway 마이그레이션 확인

```bash
docker exec -it runway-db psql -U runway -d runway \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

모든 마이그레이션의 `success` 컬럼이 `t` 이어야 한다.

### 3단계 — Spring Boot 서버 시작

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

아래 로그가 출력되면 서버가 정상 기동된 것이다.

```
Started RunwayApplication in X.XXX seconds
Tomcat started on port(s): 8080
```

### Swagger UI 접속

서버 기동 후 아래 URL에서 API 목록을 확인할 수 있다.

```
http://localhost:8080/swagger-ui/index.html
```

---

## 3. 전체 통합 테스트 시나리오

아래 시나리오는 RunWay의 핵심 플로우 전체를 순서대로 검증한다.

```
회원가입 → 로그인 → 프로필 조회 → 런 시작 → GPS 저장 → 런 완료
  → 코스 생성 → 인근 코스 탐색 → 코스 상세 → 코스 포인트
  → 코스 도전 시작 → GPS 저장 → 완주 → 리더보드 → 내 도전 이력
```

**테스트 전 변수 초기화:**

각 단계 실행 후 응답에서 ID를 추출하여 shell 변수에 저장한다. 아래 변수들을 사용한다.

```bash
ACCESS_TOKEN=""
REFRESH_TOKEN=""
RUN_ID=""
COURSE_ID=""
ATTEMPT_RUN_ID=""    
ATTEMPT_ID=""       
```

---

### Step 1 — 회원가입

```bash
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "runner@example.com",
    "password": "password1234!",
    "nickname": "runner01"
  }' | jq .
```

**응답에서 확인할 항목:** `data.userId`, `data.email`, `data.nickname`

---

### Step 2 — 로그인 및 accessToken 추출

```bash
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "runner@example.com",
    "password": "password1234!"
  }')

echo $LOGIN_RESPONSE | jq .

# 토큰 추출
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.refreshToken')

echo "ACCESS_TOKEN: $ACCESS_TOKEN"
echo "REFRESH_TOKEN: $REFRESH_TOKEN"
```

---

### Step 3 — 내 프로필 조회

```bash
curl -s -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

---

### Step 4 — 런 시작

```bash
RUN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/runs/start \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startedAt": "2026-05-13T08:30:00Z"
  }')

echo $RUN_RESPONSE | jq .

RUN_ID=$(echo $RUN_RESPONSE | jq -r '.data.runId')
echo "RUN_ID: $RUN_ID"
```

---

### Step 5 — GPS 포인트 배치 저장

충주 탄금대 근처 좌표를 사용한다. `running_record_id` 기준으로 저장된다.

```bash
curl -s -X POST http://localhost:8080/api/runs/$RUN_ID/points \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "points": [
      {
        "sequence": 0,
        "latitude": 36.970600,
        "longitude": 127.871800,
        "altitudeMeters": 72.5,
        "speedMps": 2.8,
        "recordedAt": "2026-05-13T08:30:01Z"
      },
      {
        "sequence": 1,
        "latitude": 36.971050,
        "longitude": 127.872200,
        "altitudeMeters": 73.1,
        "speedMps": 3.0,
        "recordedAt": "2026-05-13T08:30:10Z"
      },
      {
        "sequence": 2,
        "latitude": 36.971500,
        "longitude": 127.872700,
        "altitudeMeters": 73.8,
        "speedMps": 3.1,
        "recordedAt": "2026-05-13T08:30:20Z"
      },
      {
        "sequence": 3,
        "latitude": 36.972000,
        "longitude": 127.873400,
        "altitudeMeters": 74.2,
        "speedMps": 2.9,
        "recordedAt": "2026-05-13T08:30:30Z"
      },
      {
        "sequence": 4,
        "latitude": 36.972800,
        "longitude": 127.874100,
        "altitudeMeters": 74.9,
        "speedMps": 3.0,
        "recordedAt": "2026-05-13T08:30:40Z"
      }
    ]
  }' | jq .
```

**응답에서 확인할 항목:** `data.savedCount`가 `5`이어야 한다.

---

### Step 6 — 런 완료

```bash
curl -s -X POST http://localhost:8080/api/runs/$RUN_ID/finish \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "endedAt": "2026-05-13T09:05:00Z",
    "distanceMeters": 1250.5,
    "durationSeconds": 2100,
    "avgPaceSecondsPerKm": 404,
    "caloriesBurned": 310
  }' | jq .
```

**응답에서 확인할 항목:** `data.status`가 `"completed"`, `data.pathCreated`가 `true` (GPS 포인트가 2개 이상이면 path LineString이 생성됨)

---

### Step 7 — 완료된 런 기반 코스 생성

`publish: true`로 설정하면 생성 즉시 공개 상태가 된다.

```bash
COURSE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/courses/from-run/$RUN_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "탄금대 아침 러닝 코스",
    "description": "강변을 따라 달리기 좋은 코스입니다.",
    "isLoop": false,
    "publish": true
  }')

echo $COURSE_RESPONSE | jq .

COURSE_ID=$(echo $COURSE_RESPONSE | jq -r '.data.courseId')
echo "COURSE_ID: $COURSE_ID"
```

**응답에서 확인할 항목:** `data.status`가 `"published"`, `data.startPoint`와 `data.endPoint`에 좌표가 있어야 한다.

---

### Step 8 — 인근 코스 탐색

Step 7에서 저장한 코스의 시작 지점 근방에서 검색한다.

```bash
curl -s -X GET \
  "http://localhost:8080/api/courses/nearby?latitude=36.9706&longitude=127.8718&radiusMeters=3000" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

거리 필터를 추가할 수도 있다.

```bash
curl -s -X GET \
  "http://localhost:8080/api/courses/nearby?latitude=36.9706&longitude=127.8718&radiusMeters=3000&minDistanceMeters=500&maxDistanceMeters=5000" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

**응답에서 확인할 항목:** `data.content` 배열에 방금 생성한 코스가 포함되고, `distanceFromMeMeters` 값이 있어야 한다.

---

### Step 9 — 코스 상세 조회

```bash
curl -s -X GET http://localhost:8080/api/courses/$COURSE_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

**응답에서 확인할 항목:** `data.creator.nickname`, `data.attemptCount`, `data.completionCount`

---

### Step 10 — 코스 경로 포인트 조회

```bash
curl -s -X GET http://localhost:8080/api/courses/$COURSE_ID/points \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

**응답에서 확인할 항목:** `data.points` 배열에 다운샘플링된 경로 포인트가 있어야 한다. 원본 GPS 포인트가 200개 이하이면 전체가 저장된다.

---

### Step 11 — 코스 도전 시작

이 단계에서 `running_records` 행이 먼저 생성된 뒤 `course_attempts` 행이 연결된다.  
반환된 두 ID를 반드시 별도 변수로 저장한다.

```bash
ATTEMPT_START_RESPONSE=$(curl -s -X POST http://localhost:8080/api/courses/$COURSE_ID/attempts/start \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startedAt": "2026-05-13T10:30:00Z"
  }')

echo $ATTEMPT_START_RESPONSE | jq .

# GPS 포인트 저장에 사용할 ID
ATTEMPT_RUN_ID=$(echo $ATTEMPT_START_RESPONSE | jq -r '.data.runningRecordId')

# 완주/포기 처리에 사용할 ID
ATTEMPT_ID=$(echo $ATTEMPT_START_RESPONSE | jq -r '.data.courseAttemptId')

echo "ATTEMPT_RUN_ID (GPS 저장용): $ATTEMPT_RUN_ID"
echo "ATTEMPT_ID (완주/포기 처리용): $ATTEMPT_ID"
```

**응답에서 확인할 항목:** `data.status`가 `"in_progress"`, `data.verificationStatus`가 `"pending"`

---

### Step 12 — 코스 도전 중 GPS 포인트 저장

Step 11에서 반환받은 `runningRecordId` (`ATTEMPT_RUN_ID`)를 사용한다.  
코스 기반 러닝에서도 GPS 저장 endpoint는 `/api/runs/{runId}/points`를 그대로 사용한다.

```bash
curl -s -X POST http://localhost:8080/api/runs/$ATTEMPT_RUN_ID/points \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "points": [
      {
        "sequence": 0,
        "latitude": 36.970600,
        "longitude": 127.871800,
        "altitudeMeters": 72.5,
        "speedMps": 2.8,
        "recordedAt": "2026-05-13T10:30:01Z"
      },
      {
        "sequence": 1,
        "latitude": 36.971050,
        "longitude": 127.872200,
        "altitudeMeters": 73.1,
        "speedMps": 3.1,
        "recordedAt": "2026-05-13T10:30:10Z"
      },
      {
        "sequence": 2,
        "latitude": 36.971500,
        "longitude": 127.872700,
        "altitudeMeters": 73.8,
        "speedMps": 3.0,
        "recordedAt": "2026-05-13T10:30:20Z"
      },
      {
        "sequence": 3,
        "latitude": 36.972000,
        "longitude": 127.873400,
        "altitudeMeters": 74.2,
        "speedMps": 3.0,
        "recordedAt": "2026-05-13T10:30:30Z"
      },
      {
        "sequence": 4,
        "latitude": 36.972800,
        "longitude": 127.874100,
        "altitudeMeters": 74.9,
        "speedMps": 2.9,
        "recordedAt": "2026-05-13T10:30:40Z"
      }
    ]
  }' | jq .
```

---

### Step 13 — 코스 도전 완주

Step 11에서 반환받은 `courseAttemptId` (`ATTEMPT_ID`)를 사용한다.  
완주 시 `running_records`도 `completed`로 동시에 업데이트된다.

```bash
curl -s -X POST http://localhost:8080/api/course-attempts/$ATTEMPT_ID/finish \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "endedAt": "2026-05-13T11:05:00Z",
    "distanceMeters": 1250.5,
    "durationSeconds": 2100,
    "avgPaceSecondsPerKm": 404,
    "caloriesBurned": 310
  }' | jq .
```

**응답에서 확인할 항목:**
- `data.status`가 `"completed"`
- `data.verificationStatus`가 `"verified"` (Phase 1 자동 설정)
- `data.durationSeconds`가 `2100`
- `data.completedAt`에 종료 시각이 있어야 한다

---

### Step 14 — 코스 리더보드 조회

완주 기록이 반영되었는지 확인한다.

```bash
curl -s -X GET \
  "http://localhost:8080/api/courses/$COURSE_ID/leaderboard?page=0&size=50" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

**응답에서 확인할 항목:**
- `data.items[0].rank`가 `1`
- `data.items[0].nickname`이 `"runner01"`
- `data.items[0].bestTimeSeconds`가 `2100`
- `data.items[0].completionCount`가 `1`

---

### Step 15 — 내 코스 도전 이력 조회

```bash
curl -s -X GET \
  "http://localhost:8080/api/courses/$COURSE_ID/attempts/me?page=0&size=20" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

**응답에서 확인할 항목:** `data.content` 배열에 완주한 도전 기록이 포함되어야 한다.

---

### 부가 시나리오 — 코스 도전 포기

Step 11의 완주 대신 포기를 테스트하려면 `abandonAttempt`를 호출한다.  
먼저 새 도전을 시작한 뒤 포기한다.

```bash
# 새 도전 시작
ABANDON_ATTEMPT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/courses/$COURSE_ID/attempts/start \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}')

ABANDON_ATTEMPT_ID=$(echo $ABANDON_ATTEMPT_RESPONSE | jq -r '.data.courseAttemptId')

# 포기
curl -s -X POST http://localhost:8080/api/course-attempts/$ABANDON_ATTEMPT_ID/abandon \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "endedAt": "2026-05-13T10:45:00Z"
  }' | jq .
```

**응답에서 확인할 항목:**
- `data.status`가 `"abandoned"`
- `data.verificationStatus`가 `"pending"` (포기 시에는 verified로 변경되지 않음)

---

### 부가 시나리오 — Access Token 재발급

Access Token 만료 시 Refresh Token으로 재발급한다.

```bash
REISSUE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/reissue \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

echo $REISSUE_RESPONSE | jq .

ACCESS_TOKEN=$(echo $REISSUE_RESPONSE | jq -r '.data.accessToken')
REFRESH_TOKEN=$(echo $REISSUE_RESPONSE | jq -r '.data.refreshToken')
```

---

## 4. PostgreSQL 데이터 검증

각 API 호출 후 DB에 데이터가 올바르게 저장되었는지 직접 확인한다.

```bash
# PostgreSQL 컨테이너 접속
docker exec -it runway-db psql -U runway -d runway
```

또는 단일 쿼리로 실행:

```bash
docker exec -it runway-db psql -U runway -d runway -c "<쿼리>"
```

---

### users 테이블 확인

```sql
-- 전체 사용자 목록
SELECT id, email, nickname, created_at, deleted_at
FROM users
ORDER BY created_at DESC;

-- soft delete 포함 전체 확인
SELECT id, email, nickname,
       CASE WHEN deleted_at IS NULL THEN 'active' ELSE 'deleted' END AS account_status
FROM users;
```

---

### running_records 테이블 확인

```sql
-- 런 기록 목록 (최신 순)
SELECT id, user_id, status, started_at, ended_at,
       distance_meters, duration_seconds, avg_pace_seconds_per_km,
       path IS NOT NULL AS has_path,
       created_at
FROM running_records
ORDER BY created_at DESC;

-- path LineString 생성 여부 및 포인트 수 확인
SELECT r.id, r.status,
       ST_NumPoints(r.path::geometry) AS path_point_count,
       ST_Length(r.path) AS path_length_meters
FROM running_records r
WHERE r.path IS NOT NULL;
```

---

### running_points 테이블 확인

```sql
-- 특정 런 기록의 GPS 포인트 조회
-- <RUN_ID>를 실제 값으로 교체
SELECT sequence,
       ST_Y(location::geometry) AS latitude,
       ST_X(location::geometry) AS longitude,
       altitude_meters,
       speed_mps,
       recorded_at
FROM running_points
WHERE running_record_id = '<RUN_ID>'
ORDER BY sequence ASC;

-- 모든 running_points 포인트 수 집계
SELECT running_record_id, COUNT(*) AS point_count
FROM running_points
GROUP BY running_record_id
ORDER BY point_count DESC;
```

---

### courses 테이블 확인

```sql
-- 코스 목록 전체
SELECT id, creator_id, name, status,
       distance_meters, is_loop,
       attempt_count, completion_count,
       created_at, deleted_at
FROM courses
ORDER BY created_at DESC;

-- start_location 좌표 확인
SELECT id, name, status,
       ST_Y(start_location::geometry) AS start_lat,
       ST_X(start_location::geometry) AS start_lon,
       ST_Y(end_location::geometry)   AS end_lat,
       ST_X(end_location::geometry)   AS end_lon
FROM courses
WHERE deleted_at IS NULL;

-- 인근 코스 탐색 검증 (탄금대 근방 3km 이내)
SELECT id, name,
       ST_Distance(
         start_location,
         ST_SetSRID(ST_MakePoint(127.8718, 36.9706), 4326)::geography
       ) AS distance_from_point_meters
FROM courses
WHERE status = 'published'
  AND deleted_at IS NULL
  AND ST_DWithin(
        start_location,
        ST_SetSRID(ST_MakePoint(127.8718, 36.9706), 4326)::geography,
        3000
      )
ORDER BY distance_from_point_meters ASC;
```

---

### course_points 테이블 확인

```sql
-- 특정 코스의 경로 포인트 확인
-- <COURSE_ID>를 실제 값으로 교체
SELECT sequence,
       ST_Y(location::geometry) AS latitude,
       ST_X(location::geometry) AS longitude,
       altitude_meters
FROM course_points
WHERE course_id = '<COURSE_ID>'
ORDER BY sequence ASC;

-- 코스별 포인트 수 집계
SELECT course_id, COUNT(*) AS point_count
FROM course_points
GROUP BY course_id;
```

---

### course_attempts 테이블 확인

```sql
-- 도전 기록 전체
SELECT id, course_id, user_id, running_record_id,
       status, verification_status,
       started_at, completed_at,
       duration_seconds, distance_meters,
       created_at
FROM course_attempts
ORDER BY created_at DESC;

-- 리더보드 데이터 검증 (특정 코스)
-- <COURSE_ID>를 실제 값으로 교체
SELECT
  u.nickname,
  MIN(ca.duration_seconds) AS best_time_seconds,
  COUNT(*) AS completion_count,
  RANK() OVER (ORDER BY MIN(ca.duration_seconds) ASC) AS rank
FROM course_attempts ca
JOIN users u ON ca.user_id = u.id
WHERE ca.course_id = '<COURSE_ID>'
  AND ca.status = 'completed'
  AND ca.verification_status = 'verified'
GROUP BY ca.user_id, u.nickname
ORDER BY best_time_seconds ASC;

-- attempt_count, completion_count 검증
SELECT id, name, attempt_count, completion_count
FROM courses
WHERE id = '<COURSE_ID>';
```

---

## 5. 각 단계별 기대 응답

### Step 1 — 회원가입

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": "<UUID>",
    "email": "runner@example.com",
    "nickname": "runner01",
    "profileImageUrl": null,
    "bio": null,
    "createdAt": "2026-05-13T..."
  }
}
```

### Step 2 — 로그인

```json
{
  "success": true,
  "message": "로그인에 성공했습니다.",
  "data": {
    "accessToken": "<JWT>",
    "refreshToken": "<JWT>",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "userId": "<UUID>",
      "email": "runner@example.com",
      "nickname": "runner01",
      "profileImageUrl": null
    }
  }
}
```

### Step 4 — 런 시작

```json
{
  "success": true,
  "message": "런이 시작되었습니다.",
  "data": {
    "runId": "<UUID>",
    "status": "in_progress",
    "startedAt": "2026-05-13T08:30:00Z"
  }
}
```

### Step 5 — GPS 포인트 저장

```json
{
  "success": true,
  "message": "GPS 포인트가 저장되었습니다.",
  "data": {
    "runId": "<UUID>",
    "savedCount": 5
  }
}
```

### Step 6 — 런 완료

```json
{
  "success": true,
  "message": "런이 완료되었습니다.",
  "data": {
    "runId": "<UUID>",
    "status": "completed",
    "startedAt": "2026-05-13T08:30:00Z",
    "endedAt": "2026-05-13T09:05:00Z",
    "distanceMeters": 1250.5,
    "durationSeconds": 2100,
    "avgPaceSecondsPerKm": 404,
    "pathCreated": true
  }
}
```

### Step 7 — 코스 생성

```json
{
  "success": true,
  "message": "코스가 생성되었습니다.",
  "data": {
    "courseId": "<UUID>",
    "name": "탄금대 아침 러닝 코스",
    "description": "강변을 따라 달리기 좋은 코스입니다.",
    "status": "published",
    "distanceMeters": 1250.5,
    "isLoop": false,
    "startPoint": {
      "latitude": 36.9706,
      "longitude": 127.8718
    },
    "endPoint": {
      "latitude": 36.9728,
      "longitude": 127.8741
    },
    "attemptCount": 0,
    "completionCount": 0,
    "createdAt": "2026-05-13T..."
  }
}
```

### Step 11 — 코스 도전 시작

```json
{
  "success": true,
  "message": "코스 도전이 시작되었습니다.",
  "data": {
    "courseAttemptId": "<UUID>",
    "runningRecordId": "<UUID>",
    "courseId": "<UUID>",
    "status": "in_progress",
    "verificationStatus": "pending",
    "startedAt": "2026-05-13T10:30:00Z"
  }
}
```

### Step 13 — 코스 완주

```json
{
  "success": true,
  "message": "코스 도전이 완료되었습니다.",
  "data": {
    "courseAttemptId": "<UUID>",
    "runningRecordId": "<UUID>",
    "courseId": "<UUID>",
    "status": "completed",
    "verificationStatus": "verified",
    "durationSeconds": 2100,
    "distanceMeters": 1250.5,
    "completedAt": "2026-05-13T11:05:00Z"
  }
}
```

### Step 14 — 리더보드 조회

```json
{
  "success": true,
  "message": "코스 리더보드 조회가 완료되었습니다.",
  "data": {
    "courseId": "<UUID>",
    "items": [
      {
        "rank": 1,
        "userId": "<UUID>",
        "nickname": "runner01",
        "bestTimeSeconds": 2100,
        "completionCount": 1
      }
    ],
    "page": 0,
    "size": 50,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

---

## 6. 자주 발생하는 오류

### Docker 관련

#### Docker daemon이 실행되지 않음

```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock.
Is the docker daemon running?
```

**해결:** Docker Desktop을 먼저 실행한다.

#### PostgreSQL 연결 거부

```
Connection refused to localhost:5432
```

**해결:**
```bash
# 컨테이너 상태 확인
docker-compose ps

# 컨테이너가 내려가 있으면
cd backend && docker-compose up -d

# 로그 확인
docker-compose logs runway-db
```

---

### 서버 기동 관련

#### JWT_SECRET 미설정

```
IllegalArgumentException: JWT secret key must not be null or empty
```

또는

```
The specified key byte array is 0 bits which is not secure enough for any JWT HMAC-SHA algorithm.
```

**해결:** `application-local.yml`에 `jwt.secret` 값을 설정하거나 환경변수 `JWT_SECRET`을 32자 이상으로 설정한다.

#### Flyway 마이그레이션 실패

```
FlywayException: Validate failed: Detected failed migration to version X
```

**해결:** 이미 적용된 마이그레이션 파일을 수정하지 않는다. 새 변경사항은 반드시 새 `V{번호}__설명.sql` 파일로 추가한다.

---

### API 오류

#### 401 Unauthorized

```json
{
  "success": false,
  "message": "인증이 필요합니다.",
  "errorCode": "UNAUTHORIZED"
}
```

**원인 및 해결:**
- `Authorization: Bearer $ACCESS_TOKEN` header가 빠졌거나 토큰 값이 비어있다.
- Step 2를 다시 실행하여 토큰을 재발급한다.
- `echo $ACCESS_TOKEN`으로 변수가 비어있지 않은지 확인한다.

#### 401 INVALID_TOKEN 또는 EXPIRED_TOKEN

```json
{
  "success": false,
  "message": "유효하지 않은 토큰입니다.",
  "errorCode": "INVALID_TOKEN"
}
```

**해결:** Access Token이 만료되었을 경우 부가 시나리오의 reissue 단계를 실행한다.

#### 409 DUPLICATED_EMAIL

```json
{
  "success": false,
  "message": "이미 사용 중인 이메일입니다.",
  "errorCode": "DUPLICATED_EMAIL"
}
```

**해결:** 다른 email 주소로 회원가입하거나, DB에서 기존 계정을 삭제한다.

```bash
docker exec -it runway-db psql -U runway -d runway \
  -c "DELETE FROM users WHERE email = 'runner@example.com';"
```

#### 409 DUPLICATED_NICKNAME

```json
{
  "success": false,
  "message": "이미 사용 중인 닉네임입니다.",
  "errorCode": "DUPLICATED_NICKNAME"
}
```

**해결:** 다른 `nickname` 값을 사용한다.

#### 409 NOT_COMPLETED_RUN — 완료되지 않은 런으로 코스 생성 시도

```json
{
  "success": false,
  "message": "완료된 러닝 기록만 코스로 생성할 수 있습니다.",
  "errorCode": "NOT_COMPLETED_RUN"
}
```

**원인:** Step 6 (런 완료)을 호출하지 않은 상태에서 코스 생성을 시도했다.  
**해결:** Step 6을 먼저 완료한 뒤 Step 7을 실행한다.

#### 409 INVALID_COURSE_STATUS — 비공개 코스 도전 시도

```json
{
  "success": false,
  "message": "공개된 코스에만 도전할 수 있습니다.",
  "errorCode": "INVALID_COURSE_STATUS"
}
```

**원인:** `publish: false`로 생성된 `draft` 상태 코스에 도전을 시도했다.  
**해결:** 코스를 먼저 공개 처리한다.

```bash
curl -s -X PATCH http://localhost:8080/api/courses/$COURSE_ID/publish \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

#### 403 FORBIDDEN — 타인 리소스 접근

```json
{
  "success": false,
  "message": "접근 권한이 없습니다.",
  "errorCode": "FORBIDDEN"
}
```

**원인:** 다른 사용자의 런 기록 완료/삭제, 또는 `draft`/`archived` 상태 코스를 소유자가 아닌 사용자가 조회하려 했다.  
**해결:** 해당 리소스의 소유자 계정으로 로그인한 뒤 재요청한다.

#### 404 COURSE_ATTEMPT_NOT_FOUND

```json
{
  "success": false,
  "message": "코스 시도를 찾을 수 없습니다.",
  "errorCode": "COURSE_ATTEMPT_NOT_FOUND"
}
```

**원인:** 잘못된 `attemptId`를 사용하거나, 다른 사용자의 도전 ID를 사용했다.  
**해결:** `echo $ATTEMPT_ID`로 변수 값을 확인한다. Step 11의 `courseAttemptId`를 사용해야 한다.

#### 409 INVALID_ATTEMPT_STATUS — 이미 완료된 도전에 재완주 시도

```json
{
  "success": false,
  "message": "진행 중인 코스 시도만 완료/포기 처리할 수 있습니다.",
  "errorCode": "INVALID_ATTEMPT_STATUS"
}
```

**원인:** 이미 `completed` 또는 `abandoned` 상태인 도전에 다시 finish/abandon을 호출했다.  
**해결:** Step 11에서 새 도전을 시작한 뒤 진행한다.

---

## 7. Phase 1 설계 참고사항

### GPS 경로 검증은 Phase 1에서 수행하지 않는다

Phase 1에서는 사용자가 제출한 `durationSeconds`, `distanceMeters` 값을 그대로 저장한다. GPS 경로와 실제 코스 경로의 일치 여부는 검증하지 않는다. 이 검증 로직은 Phase 2에서 구현 예정이다.

### 코스 완주 시 verification_status가 자동으로 verified로 설정된다

`POST /api/course-attempts/{attemptId}/finish`를 호출하면 서버는 동일 트랜잭션에서 다음 두 값을 함께 설정한다.

```
course_attempts.status              = 'completed'
course_attempts.verification_status = 'verified'
```

Phase 2에서 GPS 검증이 도입되면 완주 시 `pending` 상태로 우선 저장하고, 비동기로 검증 후 `verified` 또는 `rejected`로 전환하는 방식으로 변경될 예정이다.

### 리더보드는 course_attempts 테이블에서 직접 집계한다

별도의 ranking 테이블이 없다. 리더보드 조회 시 아래 SQL이 실시간으로 실행된다.

```sql
WITH ranked AS (
    SELECT
        u.id               AS user_id,
        u.nickname,
        MIN(ca.duration_seconds) AS best_time_seconds,
        COUNT(*)           AS completion_count,
        RANK() OVER (ORDER BY MIN(ca.duration_seconds) ASC) AS rank
    FROM course_attempts ca
    JOIN users u ON ca.user_id = u.id
    WHERE ca.course_id = ?
      AND ca.status = 'completed'
      AND ca.verification_status = 'verified'
    GROUP BY ca.user_id, u.id, u.nickname
)
SELECT * FROM ranked ORDER BY rank ASC LIMIT ? OFFSET ?
```

`(course_id, duration_seconds) WHERE status='completed' AND verification_status='verified'` partial index가 적용되어 있어 대부분의 코스에서 충분한 성능을 제공한다.

### running_records에는 course_id가 없다

코스 기반 런과 자유 런 모두 `running_records` 테이블에 저장된다. 코스와의 연결은 `course_attempts.running_record_id` 컬럼으로만 표현된다. 따라서 어떤 런이 코스 도전 중에 저장된 것인지 확인하려면 `course_attempts` 테이블을 조인해야 한다.

```sql
SELECT r.id AS run_id, ca.course_id, ca.status AS attempt_status
FROM running_records r
JOIN course_attempts ca ON ca.running_record_id = r.id
WHERE r.user_id = '<USER_ID>';
```
