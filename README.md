# RunWay

러너를 위한 러닝 코스 탐색 및 공유 플랫폼입니다.
사용자의 현재 위치 기반으로 주변 러닝 코스를 탐색하고, 직접 코스를 기록하여 공유할 수 있습니다.

---

## 주요 기능

- 현재 위치 기반 러닝 코스 탐색
- 러닝 코스 생성 및 공유
- 코스 북마크 및 좋아요
- 회원가입 / 로그인 (JWT 인증)
- 리프레시 토큰 기반 세션 유지

---

## Tech Stack

| 영역 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Database | PostgreSQL 15, PostGIS 3.4, Flyway |
| Auth | JWT (Access Token + Refresh Token) |
| Android | Kotlin, Retrofit2, OkHttp, Fused Location Provider _(개발 예정)_ |
| Maps | Google Maps SDK 또는 Kakao Map SDK _(개발 예정)_ |
| Infra | Docker, docker-compose |

---

## Project Structure

```
RunWay/
├── backend/                  # Spring Boot API 서버
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/runway/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/   # Flyway 마이그레이션
│   ├── docker-compose.yml    # PostgreSQL + PostGIS
│   ├── build.gradle
│   └── gradlew
├── android/                  # Android 클라이언트 (개발 예정)
├── docs/
│   ├── technical-specification.md
│   └── api-specification.md
└── README.md
```

---

## Backend 로컬 실행 방법

### 사전 요구사항

- Java 17 이상
- Docker Desktop

### 1. PostgreSQL + PostGIS 실행

```bash
cd backend
docker-compose up -d
```

### 2. Spring Boot 서버 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버가 정상 실행되면 `http://localhost:8080` 에서 접근할 수 있습니다.

### 3. Swagger UI 확인

```
http://localhost:8080/swagger-ui.html
```

### 환경 변수 (선택)

운영 환경에서는 아래 환경 변수를 설정해야 합니다.

| 변수명 | 설명 | 기본값 (로컬 전용) |
|--------|------|-------------------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/runway` |
| `DB_USERNAME` | DB 사용자명 | `runway` |
| `DB_PASSWORD` | DB 비밀번호 | `runway` |
| `JWT_SECRET` | JWT 서명 키 (운영 필수 변경) | placeholder |
| `JWT_ACCESS_TOKEN_EXPIRY` | Access Token 만료 시간 (ms) | `3600000` (1시간) |
| `JWT_REFRESH_TOKEN_EXPIRY` | Refresh Token 만료 시간 (ms) | `1209600000` (14일) |

---

## Documentation

- [기술 명세서](docs/technical-specification.md)
- [API 명세서](docs/api-specification.md)

---

## 개발 현황

| 단계 | 내용 | 상태 |
|------|------|------|
| Phase 1-1 | 프로젝트 초기 설정, DB 스키마, Flyway | ✅ 완료 |
| Phase 1-2 | 회원가입 / 로그인 / JWT 인증 API | ✅ 완료 |
| Phase 2 | 러닝 코스 CRUD API | 🔜 예정 |
| Phase 3 | Android 클라이언트 개발 | 🔜 예정 |