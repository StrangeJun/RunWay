# RunWay — API Specification

> 작성일: 2026-05-13  
> 상태: MVP Phase 1 API 설계 초안  
> 기준 문서: `docs/technical-specification.md`

---

## 목차

1. [API 설계 개요](#1-api-설계-개요)
2. [공통 규칙](#2-공통-규칙)
3. [공통 응답 형식](#3-공통-응답-형식)
4. [공통 Error Code](#4-공통-error-code)
5. [Auth API](#5-auth-api)
6. [User API](#6-user-api)
7. [Running API](#7-running-api)
8. [Course API](#8-course-api)
9. [Course Attempt API](#9-course-attempt-api)
10. [주요 사용자 흐름별 API Sequence](#10-주요-사용자-흐름별-api-sequence)
11. [구현 우선순위](#11-구현-우선순위)
12. [Android Retrofit 연동 참고](#12-android-retrofit-연동-참고)
13. [Phase 2로 미루는 API](#13-phase-2로-미루는-api)

---

## 1. API 설계 개요

RunWay MVP Phase 1 API는 위치 기반 러닝 기록, 코스 생성, 주변 코스 탐색, 코스 시도 및 리더보드 기능을 구현하기 위한 REST API이다.

### 핵심 설계 원칙

- 모든 API는 `/api` prefix를 사용한다.
- 인증이 필요한 API는 `Authorization: Bearer {accessToken}` header를 사용한다.
- 모든 응답은 공통 응답 형식으로 반환한다.
- GPS 좌표는 클라이언트에서 `latitude`, `longitude` 형태로 전달한다.
- 서버 내부에서는 PostgreSQL + PostGIS의 `geography(Point, 4326)`, `geography(LineString, 4326)` 타입을 사용한다.
- 일반 러닝 기록은 `running_records`에 저장한다.
- 코스 기반 러닝은 `course_attempts.running_record_id`를 통해 `running_records`와 연결한다.
- `running_records`에는 `course_id`를 직접 저장하지 않는다.
- 리더보드는 별도 ranking table 없이 `course_attempts`에서 계산한다.
- MVP Phase 1에서는 코스 완주 시 `verification_status = 'verified'`로 자동 처리한다.

---

## 2. 공통 규칙

### Base URL

로컬 개발 기준:

```http
http://localhost:8080
```

API prefix:

```http
/api
```

예시:

```http
POST http://localhost:8080/api/auth/login
```

### 인증 Header

인증이 필요한 API는 다음 header를 포함한다.

```http
Authorization: Bearer {accessToken}
```

### Content-Type

Request Body가 있는 API는 JSON을 사용한다.

```http
Content-Type: application/json
```

### 좌표 형식

클라이언트는 위도/경도를 다음 형식으로 전달한다.

```json
{
  "latitude": 36.9706,
  "longitude": 127.8718
}
```

서버에서 PostGIS point를 만들 때는 반드시 `longitude`, `latitude` 순서로 처리한다.

```sql
ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
```

### 시간 형식

모든 시간 값은 ISO-8601 문자열을 사용한다.

```json
{
  "startedAt": "2026-05-13T08:30:00Z"
}
```

Backend에서는 `Instant` 또는 `OffsetDateTime`으로 매핑하고, Database에는 `TIMESTAMPTZ`로 저장한다.

### Pagination 규칙

목록 조회 API는 기본적으로 `page`, `size` query parameter를 사용한다.

```http
GET /api/runs/me?page=0&size=20
```

기본값:

| Parameter | Default | 설명 |
|---|---:|---|
| `page` | `0` | 0부터 시작 |
| `size` | `20` | 한 페이지 크기 |

Page 응답 형식:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "hasNext": true
}
```

---

## 3. 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### 에러 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "errorCode": "ERROR_CODE"
}
```

---

## 4. 공통 Error Code

| Error Code | HTTP Status | 설명 |
|---|---:|---|
| `INVALID_REQUEST` | 400 | 요청 형식이 올바르지 않음 |
| `VALIDATION_ERROR` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `INVALID_TOKEN` | 401 | 유효하지 않은 token |
| `EXPIRED_TOKEN` | 401 | 만료된 token |
| `FORBIDDEN` | 403 | 접근 권한 없음 |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| `RUN_NOT_FOUND` | 404 | 러닝 기록을 찾을 수 없음 |
| `COURSE_NOT_FOUND` | 404 | 코스를 찾을 수 없음 |
| `COURSE_ATTEMPT_NOT_FOUND` | 404 | 코스 시도를 찾을 수 없음 |
| `DUPLICATED_EMAIL` | 409 | 이미 사용 중인 email |
| `DUPLICATED_NICKNAME` | 409 | 이미 사용 중인 nickname |
| `INVALID_RUN_STATUS` | 409 | 러닝 상태 전환 불가 |
| `INVALID_COURSE_STATUS` | 409 | 코스 상태 전환 불가 |
| `INVALID_ATTEMPT_STATUS` | 409 | 코스 시도 상태 전환 불가 |
| `NOT_COMPLETED_RUN` | 409 | 완료되지 않은 러닝 기록으로 코스 생성 시도 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 5. Auth API

## 5-1. 회원가입

| 항목 | 내용 |
|---|---|
| Feature | 회원가입 |
| Method | `POST` |
| URI | `/api/auth/signup` |
| Auth Required | No |
| Related Tables | `users` |

### Request Body

```json
{
  "email": "runner@example.com",
  "password": "password1234!",
  "nickname": "runner01"
}
```

### Success Response

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": "4d3b1c9e-2d57-4c3b-93b4-1fb8c3f5a001",
    "email": "runner@example.com",
    "nickname": "runner01",
    "profileImageUrl": null,
    "bio": null,
    "createdAt": "2026-05-13T08:30:00Z"
  }
}
```

### Error Response Examples

```json
{
  "success": false,
  "message": "이미 사용 중인 이메일입니다.",
  "errorCode": "DUPLICATED_EMAIL"
}
```

```json
{
  "success": false,
  "message": "이미 사용 중인 닉네임입니다.",
  "errorCode": "DUPLICATED_NICKNAME"
}
```

### Service Logic Summary

1. `email`, `password`, `nickname` validation
2. email 중복 확인
3. nickname 중복 확인
4. password를 BCrypt로 hash 처리
5. `users` row 생성
6. 생성된 사용자 정보 반환

---

## 5-2. 로그인

| 항목 | 내용 |
|---|---|
| Feature | 로그인 |
| Method | `POST` |
| URI | `/api/auth/login` |
| Auth Required | No |
| Related Tables | `users` |

### Request Body

```json
{
  "email": "runner@example.com",
  "password": "password1234!"
}
```

### Success Response

```json
{
  "success": true,
  "message": "로그인에 성공했습니다.",
  "data": {
    "accessToken": "access-token-value",
    "refreshToken": "refresh-token-value",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "userId": "4d3b1c9e-2d57-4c3b-93b4-1fb8c3f5a001",
      "email": "runner@example.com",
      "nickname": "runner01",
      "profileImageUrl": null
    }
  }
}
```

### Service Logic Summary

1. email로 user 조회
2. password hash 검증
3. Access Token 생성
4. Refresh Token 생성
5. token 정보와 user summary 반환

### Android Retrofit Notes

- `accessToken`, `refreshToken`은 `DataStore`에 저장한다.
- 이후 인증 API 요청 시 `OkHttp Interceptor`에서 `Authorization` header를 자동 추가한다.

---

## 5-3. Access Token 재발급

| 항목 | 내용 |
|---|---|
| Feature | Access Token 재발급 |
| Method | `POST` |
| URI | `/api/auth/reissue` |
| Auth Required | No |
| Related Tables | `users` |

### Request Body

```json
{
  "refreshToken": "refresh-token-value"
}
```

### Success Response

```json
{
  "success": true,
  "message": "토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "new-access-token-value",
    "refreshToken": "new-refresh-token-value",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### Service Logic Summary

1. refreshToken 유효성 검증
2. user 식별
3. 새 Access Token 발급
4. 필요 시 Refresh Token rotate
5. token 응답 반환

---

## 5-4. 로그아웃

| 항목 | 내용 |
|---|---|
| Feature | 로그아웃 |
| Method | `POST` |
| URI | `/api/auth/logout` |
| Auth Required | Yes |
| Related Tables | `users` |

### Request Headers

```http
Authorization: Bearer {accessToken}
```

### Request Body

```json
{
  "refreshToken": "refresh-token-value"
}
```

### Success Response

```json
{
  "success": true,
  "message": "로그아웃되었습니다.",
  "data": null
}
```

---

## 6. User API

## 6-1. 내 프로필 조회

| 항목 | 내용 |
|---|---|
| Feature | 내 프로필 조회 |
| Method | `GET` |
| URI | `/api/users/me` |
| Auth Required | Yes |
| Related Tables | `users` |

### Request Headers

```http
Authorization: Bearer {accessToken}
```

### Success Response

```json
{
  "success": true,
  "message": "내 프로필 조회에 성공했습니다.",
  "data": {
    "userId": "4d3b1c9e-2d57-4c3b-93b4-1fb8c3f5a001",
    "email": "runner@example.com",
    "nickname": "runner01",
    "profileImageUrl": null,
    "bio": null,
    "createdAt": "2026-05-13T08:30:00Z"
  }
}
```

---

## 6-2. 내 프로필 수정

| 항목 | 내용 |
|---|---|
| Feature | 내 프로필 수정 |
| Method | `PUT` |
| URI | `/api/users/me` |
| Auth Required | Yes |
| Related Tables | `users` |

### Request Body

```json
{
  "nickname": "newRunner",
  "profileImageUrl": "https://example.com/profile.jpg",
  "bio": "아침 러닝을 좋아합니다."
}
```

### Success Response

```json
{
  "success": true,
  "message": "프로필이 수정되었습니다.",
  "data": {
    "userId": "4d3b1c9e-2d57-4c3b-93b4-1fb8c3f5a001",
    "email": "runner@example.com",
    "nickname": "newRunner",
    "profileImageUrl": "https://example.com/profile.jpg",
    "bio": "아침 러닝을 좋아합니다.",
    "updatedAt": "2026-05-13T09:00:00Z"
  }
}
```

---

## 6-3. 회원 탈퇴

| 항목 | 내용 |
|---|---|
| Feature | 회원 탈퇴 |
| Method | `DELETE` |
| URI | `/api/users/me` |
| Auth Required | Yes |
| Related Tables | `users` |

### Success Response

```json
{
  "success": true,
  "message": "회원 탈퇴가 완료되었습니다.",
  "data": null
}
```

---

## 7. Running API

## 7-1. 런 시작

| 항목 | 내용 |
|---|---|
| Feature | 런 시작 |
| Method | `POST` |
| URI | `/api/runs/start` |
| Auth Required | Yes |
| Related Tables | `running_records` |

### Request Body

```json
{
  "startedAt": "2026-05-13T08:30:00Z"
}
```

### Success Response

```json
{
  "success": true,
  "message": "런이 시작되었습니다.",
  "data": {
    "runId": "3f9c1d21-bc0e-4dc9-b92f-3a2a3c4c1001",
    "status": "in_progress",
    "startedAt": "2026-05-13T08:30:00Z"
  }
}
```

### Service Logic Summary

1. 현재 user 조회
2. 진행 중인 run 존재 여부 확인
3. `running_records` row 생성
4. `status = 'in_progress'`
5. `runId` 반환

---

## 7-2. GPS 포인트 배치 저장

| 항목 | 내용 |
|---|---|
| Feature | GPS 포인트 배치 저장 |
| Method | `POST` |
| URI | `/api/runs/{runId}/points` |
| Auth Required | Yes |
| Related Tables | `running_records`, `running_points` |

### Path Variables

| Name | Type | 설명 |
|---|---|---|
| `runId` | UUID | 러닝 기록 ID |

### Request Body

```json
{
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
      "latitude": 36.970650,
      "longitude": 127.871850,
      "altitudeMeters": 72.7,
      "speedMps": 2.9,
      "recordedAt": "2026-05-13T08:30:04Z"
    }
  ]
}
```

### Success Response

```json
{
  "success": true,
  "message": "GPS 포인트가 저장되었습니다.",
  "data": {
    "runId": "3f9c1d21-bc0e-4dc9-b92f-3a2a3c4c1001",
    "savedCount": 2
  }
}
```

### Android Retrofit Notes

- 1개씩 실시간 전송하지 말고 5~20개 단위 batch 전송을 권장한다.
- 네트워크 실패 시 local queue에 보관 후 재전송한다.
- `(running_record_id, sequence)` unique constraint로 중복 저장을 방지한다.

---

## 7-3. 런 일시정지

| 항목 | 내용 |
|---|---|
| Feature | 런 일시정지 |
| Method | `POST` |
| URI | `/api/runs/{runId}/pause` |
| Auth Required | Yes |
| Related Tables | `running_records` |

### Success Response

```json
{
  "success": true,
  "message": "런이 일시정지되었습니다.",
  "data": {
    "runId": "3f9c1d21-bc0e-4dc9-b92f-3a2a3c4c1001",
    "status": "paused"
  }
}
```

---

## 7-4. 런 재개

| 항목 | 내용 |
|---|---|
| Feature | 런 재개 |
| Method | `POST` |
| URI | `/api/runs/{runId}/resume` |
| Auth Required | Yes |
| Related Tables | `running_records` |

### Success Response

```json
{
  "success": true,
  "message": "런이 재개되었습니다.",
  "data": {
    "runId": "3f9c1d21-bc0e-4dc9-b92f-3a2a3c4c1001",
    "status": "in_progress"
  }
}
```

---

## 7-5. 런 완료

| 항목 | 내용 |
|---|---|
| Feature | 런 완료 |
| Method | `POST` |
| URI | `/api/runs/{runId}/finish` |
| Auth Required | Yes |
| Related Tables | `running_records`, `running_points` |

### Request Body

```json
{
  "endedAt": "2026-05-13T09:05:00Z",
  "distanceMeters": 5200.4,
  "durationSeconds": 2100,
  "avgPaceSecondsPerKm": 404,
  "caloriesBurned": 310
}
```

### Success Response

```json
{
  "success": true,
  "message": "런이 완료되었습니다.",
  "data": {
    "runId": "3f9c1d21-bc0e-4dc9-b92f-3a2a3c4c1001",
    "status": "completed",
    "startedAt": "2026-05-13T08:30:00Z",
    "endedAt": "2026-05-13T09:05:00Z",
    "distanceMeters": 5200.4,
    "durationSeconds": 2100,
    "avgPaceSecondsPerKm": 404,
    "pathCreated": true
  }
}
```

### Service Logic Summary

1. run 조회
2. 소유자 확인
3. status가 `in_progress` 또는 `paused`인지 확인
4. distance, duration, pace 정보 저장
5. `running_points`를 sequence 순서로 조회
6. `ST_MakeLine()` 기반 `path` LineString 생성
7. `status = 'completed'`
8. 완료 응답 반환

---

## 7-6. 런 중단

| 항목 | 내용 |
|---|---|
| Feature | 런 중단 |
| Method | `POST` |
| URI | `/api/runs/{runId}/abandon` |
| Auth Required | Yes |
| Related Tables | `running_records` |

### Success Response

```json
{
  "success": true,
  "message": "런이 중단되었습니다.",
  "data": {
    "runId": "3f9c1d21-bc0e-4dc9-b92f-3a2a3c4c1001",
    "status": "abandoned"
  }
}
```

---

## 7-7. 내 러닝 기록 목록 조회

| 항목 | 내용 |
|---|---|
| Feature | 내 러닝 기록 목록 조회 |
| Method | `GET` |
| URI | `/api/runs/me` |
| Auth Required | Yes |
| Related Tables | `running_records` |

### Query Parameters

| Name | Type | Required | Default | 설명 |
|---|---|---|---|---|
| `page` | Integer | No | 0 | 페이지 번호 |
| `size` | Integer | No | 20 | 페이지 크기 |
| `status` | String | No | `completed` | `in_progress`, `paused`, `completed`, `abandoned` |

### Success Response

```json
{
  "success": true,
  "message": "내 러닝 기록 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "runId": "3f9c1d21-bc0e-4dc9-b92f-3a2a3c4c1001",
        "status": "completed",
        "startedAt": "2026-05-13T08:30:00Z",
        "endedAt": "2026-05-13T09:05:00Z",
        "distanceMeters": 5200.4,
        "durationSeconds": 2100,
        "avgPaceSecondsPerKm": 404
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

---

## 7-8. 러닝 기록 상세 조회

| 항목 | 내용 |
|---|---|
| Feature | 러닝 기록 상세 조회 |
| Method | `GET` |
| URI | `/api/runs/{runId}` |
| Auth Required | Yes |
| Related Tables | `running_records`, `running_points` |

### Success Response

```json
{
  "success": true,
  "message": "러닝 기록 상세 조회에 성공했습니다.",
  "data": {
    "runId": "3f9c1d21-bc0e-4dc9-b92f-3a2a3c4c1001",
    "status": "completed",
    "startedAt": "2026-05-13T08:30:00Z",
    "endedAt": "2026-05-13T09:05:00Z",
    "distanceMeters": 5200.4,
    "durationSeconds": 2100,
    "avgPaceSecondsPerKm": 404,
    "caloriesBurned": 310,
    "points": [
      {
        "sequence": 0,
        "latitude": 36.970600,
        "longitude": 127.871800,
        "altitudeMeters": 72.5,
        "speedMps": 2.8,
        "recordedAt": "2026-05-13T08:30:01Z"
      }
    ]
  }
}
```

---

## 7-9. 러닝 기록 삭제

| 항목 | 내용 |
|---|---|
| Feature | 러닝 기록 삭제 |
| Method | `DELETE` |
| URI | `/api/runs/{runId}` |
| Auth Required | Yes |
| Related Tables | `running_records`, `running_points` |

### Success Response

```json
{
  "success": true,
  "message": "러닝 기록이 삭제되었습니다.",
  "data": null
}
```

---

## 8. Course API

## 8-1. 러닝 기록 기반 코스 생성

| 항목 | 내용 |
|---|---|
| Feature | 러닝 기록 기반 코스 생성 |
| Method | `POST` |
| URI | `/api/courses/from-run/{runId}` |
| Auth Required | Yes |
| Related Tables | `running_records`, `running_points`, `courses`, `course_points` |

### Request Body

```json
{
  "name": "탄금대 아침 러닝 코스",
  "description": "강변을 따라 달리기 좋은 코스입니다.",
  "publish": true
}
```

### Success Response

```json
{
  "success": true,
  "message": "코스가 생성되었습니다.",
  "data": {
    "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
    "name": "탄금대 아침 러닝 코스",
    "description": "강변을 따라 달리기 좋은 코스입니다.",
    "status": "published",
    "distanceMeters": 5200.4,
    "isLoop": false,
    "startPoint": {
      "latitude": 36.970600,
      "longitude": 127.871800
    },
    "endPoint": {
      "latitude": 36.981000,
      "longitude": 127.882000
    },
    "attemptCount": 0,
    "completionCount": 0,
    "createdAt": "2026-05-13T09:10:00Z"
  }
}
```

### Service Logic Summary

1. run 조회
2. 소유자 확인
3. run status가 `completed`인지 확인
4. `running_points` 조회
5. 시작점, 종료점 추출
6. `running_points` 다운샘플링
7. `course_points` 저장
8. `courses.path` 생성
9. `publish = true`이면 `status = 'published'`
10. 생성된 course 반환

---

## 8-2. 인근 코스 조회

| 항목 | 내용 |
|---|---|
| Feature | 인근 코스 조회 |
| Method | `GET` |
| URI | `/api/courses/nearby` |
| Auth Required | Yes |
| Related Tables | `courses` |

### Query Parameters

| Name | Type | Required | Default | 설명 |
|---|---|---|---|---|
| `latitude` | Double | Yes | - | 현재 위도 |
| `longitude` | Double | Yes | - | 현재 경도 |
| `radiusMeters` | Integer | No | 3000 | 검색 반경 |
| `minDistanceMeters` | Double | No | - | 최소 코스 거리 |
| `maxDistanceMeters` | Double | No | - | 최대 코스 거리 |
| `isLoop` | Boolean | No | - | 루프 코스 여부 |
| `page` | Integer | No | 0 | 페이지 번호 |
| `size` | Integer | No | 20 | 페이지 크기 |

### Success Response

```json
{
  "success": true,
  "message": "인근 코스 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
        "name": "탄금대 아침 러닝 코스",
        "description": "강변을 따라 달리기 좋은 코스입니다.",
        "distanceMeters": 5200.4,
        "distanceFromMeMeters": 850.3,
        "isLoop": false,
        "attemptCount": 12,
        "completionCount": 8,
        "startPoint": {
          "latitude": 36.970600,
          "longitude": 127.871800
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

---

## 8-3. 내가 만든 코스 목록 조회

| 항목 | 내용 |
|---|---|
| Feature | 내가 만든 코스 목록 조회 |
| Method | `GET` |
| URI | `/api/courses/me` |
| Auth Required | Yes |
| Related Tables | `courses` |

### Query Parameters

| Name | Type | Required | Default |
|---|---|---|---|
| `status` | String | No | - |
| `page` | Integer | No | 0 |
| `size` | Integer | No | 20 |

---

## 8-4. 코스 상세 조회

| 항목 | 내용 |
|---|---|
| Feature | 코스 상세 조회 |
| Method | `GET` |
| URI | `/api/courses/{courseId}` |
| Auth Required | Yes |
| Related Tables | `courses`, `users` |

### Success Response

```json
{
  "success": true,
  "message": "코스 상세 조회에 성공했습니다.",
  "data": {
    "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
    "name": "탄금대 아침 러닝 코스",
    "description": "강변을 따라 달리기 좋은 코스입니다.",
    "status": "published",
    "distanceMeters": 5200.4,
    "isLoop": false,
    "attemptCount": 12,
    "completionCount": 8,
    "creator": {
      "userId": "4d3b1c9e-2d57-4c3b-93b4-1fb8c3f5a001",
      "nickname": "runner01",
      "profileImageUrl": null
    },
    "startPoint": {
      "latitude": 36.970600,
      "longitude": 127.871800
    },
    "endPoint": {
      "latitude": 36.981000,
      "longitude": 127.882000
    },
    "createdAt": "2026-05-13T09:10:00Z"
  }
}
```

---

## 8-5. 코스 경로 포인트 조회

| 항목 | 내용 |
|---|---|
| Feature | 코스 경로 포인트 조회 |
| Method | `GET` |
| URI | `/api/courses/{courseId}/points` |
| Auth Required | Yes |
| Related Tables | `courses`, `course_points` |

### Success Response

```json
{
  "success": true,
  "message": "코스 경로 조회에 성공했습니다.",
  "data": {
    "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
    "points": [
      {
        "sequence": 0,
        "latitude": 36.970600,
        "longitude": 127.871800,
        "altitudeMeters": 72.5
      },
      {
        "sequence": 1,
        "latitude": 36.970850,
        "longitude": 127.872000,
        "altitudeMeters": 73.1
      }
    ]
  }
}
```

---

## 8-6. 코스 기본 정보 수정

| 항목 | 내용 |
|---|---|
| Feature | 코스 기본 정보 수정 |
| Method | `PUT` |
| URI | `/api/courses/{courseId}` |
| Auth Required | Yes |
| Related Tables | `courses` |

### Request Body

```json
{
  "name": "탄금대 저녁 러닝 코스",
  "description": "해질녘에 달리기 좋은 강변 코스입니다."
}
```

---

## 8-7. 코스 공개

| 항목 | 내용 |
|---|---|
| Feature | 코스 공개 |
| Method | `PATCH` |
| URI | `/api/courses/{courseId}/publish` |
| Auth Required | Yes |
| Related Tables | `courses` |

### Success Response

```json
{
  "success": true,
  "message": "코스가 공개되었습니다.",
  "data": {
    "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
    "status": "published"
  }
}
```

---

## 8-8. 코스 보관 처리

| 항목 | 내용 |
|---|---|
| Feature | 코스 보관 처리 |
| Method | `PATCH` |
| URI | `/api/courses/{courseId}/archive` |
| Auth Required | Yes |
| Related Tables | `courses` |

### Success Response

```json
{
  "success": true,
  "message": "코스가 보관 처리되었습니다.",
  "data": {
    "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
    "status": "archived"
  }
}
```

---

## 9. Course Attempt API

## 9-1. 코스 시도 시작

| 항목 | 내용 |
|---|---|
| Feature | 코스 시도 시작 |
| Method | `POST` |
| URI | `/api/courses/{courseId}/attempts/start` |
| Auth Required | Yes |
| Related Tables | `courses`, `running_records`, `course_attempts` |

### Request Body

```json
{
  "startedAt": "2026-05-13T10:30:00Z"
}
```

### Success Response

```json
{
  "success": true,
  "message": "코스 시도가 시작되었습니다.",
  "data": {
    "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
    "courseAttemptId": "5f7c2f92-718f-43c2-b7f2-9992e9101001",
    "runningRecordId": "9afcd671-8d70-4987-98fc-10e71c201001",
    "attemptStatus": "in_progress",
    "runStatus": "in_progress",
    "startedAt": "2026-05-13T10:30:00Z"
  }
}
```

### Service Logic Summary

1. course 조회
2. course가 `published` 상태인지 확인
3. 현재 user 조회
4. `running_records` 먼저 생성
5. `course_attempts` 생성
6. `course_attempts.running_record_id`에 `running_records.id` 연결
7. `courses.attempt_count` 증가
8. `courseAttemptId`, `runningRecordId` 반환

### Android Retrofit Notes

- 반환된 `runningRecordId`로 GPS point 저장 API를 호출한다.
- 반환된 `courseAttemptId`로 완주/포기 API를 호출한다.
- 코스 기반 러닝 중에도 GPS 저장 API는 `/api/runs/{runId}/points`를 그대로 사용한다.

---

## 9-2. 코스 시도 완주

| 항목 | 내용 |
|---|---|
| Feature | 코스 시도 완주 |
| Method | `POST` |
| URI | `/api/course-attempts/{attemptId}/finish` |
| Auth Required | Yes |
| Related Tables | `course_attempts`, `running_records`, `running_points`, `courses` |

### Request Body

```json
{
  "endedAt": "2026-05-13T11:05:00Z",
  "distanceMeters": 5200.4,
  "durationSeconds": 2100,
  "avgPaceSecondsPerKm": 404,
  "caloriesBurned": 310
}
```

### Success Response

```json
{
  "success": true,
  "message": "코스 시도가 완료되었습니다.",
  "data": {
    "courseAttemptId": "5f7c2f92-718f-43c2-b7f2-9992e9101001",
    "runningRecordId": "9afcd671-8d70-4987-98fc-10e71c201001",
    "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
    "attemptStatus": "completed",
    "verificationStatus": "verified",
    "durationSeconds": 2100,
    "distanceMeters": 5200.4,
    "completedAt": "2026-05-13T11:05:00Z"
  }
}
```

### Service Logic Summary

1. attempt 조회
2. user 소유 여부 확인
3. attempt status가 `in_progress`인지 확인
4. 연결된 `running_records` 조회
5. `running_records`를 `completed`로 변경
6. `running_points`로 `path` LineString 생성
7. `course_attempts.status = 'completed'`
8. `course_attempts.verification_status = 'verified'`
9. `courses.completion_count` 증가
10. 동일 트랜잭션으로 처리

---

## 9-3. 코스 시도 포기

| 항목 | 내용 |
|---|---|
| Feature | 코스 시도 포기 |
| Method | `POST` |
| URI | `/api/course-attempts/{attemptId}/abandon` |
| Auth Required | Yes |
| Related Tables | `course_attempts`, `running_records` |

### Request Body

```json
{
  "abandonedAt": "2026-05-13T10:50:00Z"
}
```

### Success Response

```json
{
  "success": true,
  "message": "코스 시도가 중단되었습니다.",
  "data": {
    "courseAttemptId": "5f7c2f92-718f-43c2-b7f2-9992e9101001",
    "runningRecordId": "9afcd671-8d70-4987-98fc-10e71c201001",
    "attemptStatus": "abandoned",
    "runStatus": "abandoned"
  }
}
```

---

## 9-4. 코스 리더보드 조회

| 항목 | 내용 |
|---|---|
| Feature | 코스 리더보드 조회 |
| Method | `GET` |
| URI | `/api/courses/{courseId}/leaderboard` |
| Auth Required | Yes |
| Related Tables | `course_attempts`, `users` |

### Query Parameters

| Name | Type | Required | Default | 설명 |
|---|---|---|---|---|
| `limit` | Integer | No | 50 | 조회할 순위 수 |

### Success Response

```json
{
  "success": true,
  "message": "리더보드 조회에 성공했습니다.",
  "data": {
    "courseId": "0af9f3d2-8e7d-45c2-97e1-7a99a5b91001",
    "items": [
      {
        "rank": 1,
        "userId": "4d3b1c9e-2d57-4c3b-93b4-1fb8c3f5a001",
        "nickname": "runner01",
        "profileImageUrl": null,
        "bestTimeSeconds": 2100,
        "completionCount": 3
      }
    ]
  }
}
```

### SQL Concept

```sql
SELECT
  ca.user_id,
  u.nickname,
  MIN(ca.duration_seconds) AS best_time_seconds,
  COUNT(*) AS completion_count,
  RANK() OVER (ORDER BY MIN(ca.duration_seconds) ASC) AS rank
FROM course_attempts ca
JOIN users u ON ca.user_id = u.id
WHERE ca.course_id = :courseId
  AND ca.status = 'completed'
  AND ca.verification_status = 'verified'
GROUP BY ca.user_id, u.nickname
ORDER BY best_time_seconds ASC
LIMIT :limit;
```

---

## 9-5. 특정 코스의 내 시도 이력 조회

| 항목 | 내용 |
|---|---|
| Feature | 특정 코스의 내 시도 이력 조회 |
| Method | `GET` |
| URI | `/api/courses/{courseId}/attempts/me` |
| Auth Required | Yes |
| Related Tables | `course_attempts`, `running_records` |

### Query Parameters

| Name | Type | Required | Default |
|---|---|---|---|
| `page` | Integer | No | 0 |
| `size` | Integer | No | 20 |

---

## 10. 주요 사용자 흐름별 API Sequence

## 10-1. 회원가입 / 로그인

```text
1. POST /api/auth/signup
2. POST /api/auth/login
3. Android DataStore에 accessToken, refreshToken 저장
4. GET /api/users/me
```

## 10-2. 자유 러닝 기록 저장

```text
1. POST /api/runs/start
2. POST /api/runs/{runId}/points 반복 호출 또는 batch 호출
3. POST /api/runs/{runId}/pause    [선택]
4. POST /api/runs/{runId}/resume   [선택]
5. POST /api/runs/{runId}/finish
6. GET /api/runs/{runId}
```

## 10-3. 완료된 러닝 기록으로 코스 생성

```text
1. GET /api/runs/me
2. GET /api/runs/{runId}
3. POST /api/courses/from-run/{runId}
4. GET /api/courses/{courseId}
5. GET /api/courses/{courseId}/points
```

## 10-4. 주변 코스 탐색

```text
1. Android 현재 위치 획득
2. GET /api/courses/nearby?latitude={lat}&longitude={lng}&radiusMeters=3000
3. GET /api/courses/{courseId}
4. GET /api/courses/{courseId}/points
```

## 10-5. 코스 기반 러닝

```text
1. GET /api/courses/{courseId}
2. GET /api/courses/{courseId}/points
3. POST /api/courses/{courseId}/attempts/start
   - response: runningRecordId, courseAttemptId
4. POST /api/runs/{runningRecordId}/points 반복 호출 또는 batch 호출
5. POST /api/course-attempts/{courseAttemptId}/finish
6. GET /api/courses/{courseId}/leaderboard
```

---

## 11. 구현 우선순위

### 1순위 — Backend 기반

1. 공통 응답 구조
2. 공통 예외 처리
3. JWT 인증 구조
4. `users`
5. Auth API
6. User API

### 2순위 — 자유 러닝

1. `running_records`
2. `running_points`
3. Running API
4. `path` LineString 생성

### 3순위 — 코스 생성 / 탐색

1. `courses`
2. `course_points`
3. Course Creation API
4. Nearby Course API
5. Course Detail API

### 4순위 — 코스 시도 / 리더보드

1. `course_attempts`
2. Course Attempt API
3. Leaderboard API
4. `attempt_count`, `completion_count` 업데이트

### 5순위 — Android 연동

1. Auth 연동
2. 자유 러닝 GPS 저장
3. 코스 생성
4. 주변 코스 조회
5. 코스 기반 러닝
6. 리더보드 표시

---

## 12. Android Retrofit 연동 참고

### 공통 Wrapper

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val errorCode: String? = null
)
```

### Token 저장

- `accessToken`: DataStore 저장
- `refreshToken`: DataStore 저장
- `Authorization` header는 `OkHttp Interceptor`에서 자동 추가

### GPS 포인트 전송 전략

- GPS point는 단건 실시간 전송보다 batch 전송 권장
- 추천 batch 기준:
  - 5~20개 point 단위
  - 또는 10~30초 단위
- 네트워크 실패 시 local queue에 저장 후 재전송
- 서버 중복 방지를 위해 `(running_record_id, sequence)` unique constraint 활용

### Course Attempt 연동 핵심

코스 기반 러닝 시작 시 서버는 두 ID를 반환한다.

```json
{
  "courseAttemptId": "attempt-id",
  "runningRecordId": "run-id"
}
```

Android 사용 기준:

| ID | 사용처 |
|---|---|
| `runningRecordId` | GPS point 저장 |
| `courseAttemptId` | 코스 완주 / 포기 처리 |

---

## 13. Phase 2로 미루는 API

MVP Phase 1에서는 아래 API를 구현하지 않는다.

### Certification Image API

```http
POST /api/course-attempts/{attemptId}/certification-image
GET /api/course-attempts/{attemptId}/certification-image
```

미루는 이유:

- 이미지 합성 로직 필요
- S3 또는 Object Storage 연동 필요
- Phase 1 핵심 기능이 아님

### Course Usage Statistics API

```http
GET /api/courses/{courseId}/stats
GET /api/courses/{courseId}/stats/hourly
```

미루는 이유:

- Phase 1에서는 `attempt_count`, `completion_count`, leaderboard만으로 충분
- 실제 트래픽 이후 통계 캐싱 필요성 확인 후 도입

### Route Analysis API

```http
GET /api/routes/candidates
POST /api/routes/analyze
```

미루는 이유:

- 반복 경로 분석은 충분한 `running_records`와 `running_points` 데이터가 쌓인 뒤 의미 있음
- Phase 1에서는 사용자가 완료된 run으로 직접 course를 생성하는 방식으로 core loop 검증

### Social API

```http
POST /api/users/{userId}/follow
GET /api/feed
POST /api/courses/{courseId}/likes
```

미루는 이유:

- 소셜 그래프 설계는 독립적인 복잡도를 가짐
- MVP 핵심인 러닝 기록, 코스 생성, 코스 경쟁이 안정화된 이후 도입
