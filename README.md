# RunWay

위치 기반 러닝 코스 공유 및 경쟁 앱.

사용자가 GPS로 러닝 경로를 기록하고 코스로 등록해 공개한다. 다른 사용자는 주변 코스를 탐색하고, 해당 코스에 도전하여 완주 기록을 남긴다. 코스별 리더보드에서 완주 기록을 비교한다.

**핵심 플로우:** `런 시작 → GPS 수집 → 런 완료 → 코스 생성 → 코스 탐색 → 코스 도전 → 리더보드`

---

## 개발 현황

| 단계 | 내용 | 상태 |
|------|------|------|
| Phase 1 — Backend | Auth / User / Running / Course / Course Attempt / Leaderboard API | ✅ 완료 (통합 테스트 완료) |
| Android Phase B-3~B-6 | 인증 UI, 홈 네비게이션, 러닝 추적 UI, Running API 연동 | ✅ 완료 |
| Android Phase B-7~B-10 | 코스 생성, 주변 탐색, 코스 상세, 도전 + 리더보드 | 🔄 진행 중 |
| Phase 2 | GPS 경로 검증, 완주 인증 이미지, 코스 통계, 소셜 기능 | 🔜 예정 |

---

## 완료된 Backend 기능

| API 그룹 | 설명 |
|----------|------|
| **Auth API** | 회원가입, 로그인, Access Token 재발급, 로그아웃 |
| **User API** | 내 프로필 조회 / 수정 / 탈퇴 |
| **Running API** | 런 시작 / 일시정지 / 재개 / 완료 / 중단, GPS 포인트 배치 저장, 기록 조회 |
| **Course API** | 코스 생성, 수정, 공개 / 보관, 인근 코스 탐색, 코스 상세 / 경로 포인트 조회 |
| **Course Attempt API** | 코스 도전 시작 / 완주 / 포기 |
| **Leaderboard API** | 코스별 리더보드 조회, 내 도전 이력 조회 |

---

## Tech Stack

| 영역 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Database | PostgreSQL 15, PostGIS 3.4, Flyway |
| Auth | JWT (Access Token + Refresh Token) |
| Android | Kotlin, Retrofit2, OkHttp, Fused Location Provider _(초기화 예정)_ |
| Maps | Google Maps SDK 또는 Kakao Map SDK _(예정)_ |
| Infra | Docker, docker-compose |

---

## Project Structure

```
RunWay/
├── backend/                        # Spring Boot API 서버 (Phase 1 완료)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/runway/    # 메인 패키지
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/   # Flyway 마이그레이션 (V1~V7)
│   ├── docker-compose.yml          # PostgreSQL + PostGIS
│   ├── build.gradle
│   └── gradlew
├── android/                        # Kotlin Android 클라이언트 (Phase B-3~B-6 완료)
│   ├── app/src/main/java/com/runway/android/
│   │   ├── core/                   # network, datastore, result
│   │   ├── data/                   # repository 구현체, API, DTOs
│   │   ├── domain/                 # repository 인터페이스
│   │   ├── di/                     # Hilt 모듈
│   │   └── ui/                     # Compose 화면 및 ViewModel
│   └── app/build.gradle.kts
├── design/
│   └── lovable/                    # Lovable 기반 React/Vite UI 프로토타입
├── docs/
│   ├── technical-specification.md
│   ├── api-specification.md
│   └── backend-test-guide.md
└── README.md
```

---

## Backend 로컬 실행 방법

### 사전 요구사항

- Java 17 이상
- Docker Desktop (실행 중이어야 함)
- 포트 `5432` (PostgreSQL), `8080` (Spring Boot) 사용 가능

### 1. JWT_SECRET 설정

`JWT_SECRET`은 기본값이 없어 반드시 설정해야 한다.  
`backend/src/main/resources/application-local.yml` 파일을 생성한다. 이 파일은 Git에 커밋하지 않는다.

```yaml
# backend/src/main/resources/application-local.yml
jwt:
  secret: local-development-secret-must-be-at-least-32-characters-long
```

또는 환경변수로 직접 설정할 수 있다.

```bash
export JWT_SECRET=local-development-secret-must-be-at-least-32-characters-long
```

### 2. PostgreSQL + PostGIS 실행

```bash
cd backend
docker-compose up -d
```

### 3. Spring Boot 서버 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

아래 로그가 출력되면 서버가 정상 기동된 것이다.

```
Started RunwayApplication in X.XXX seconds
Tomcat started on port(s): 8080
```

### 4. Swagger UI 확인

```
http://localhost:8080/swagger-ui/index.html
```

### 환경변수 전체 목록

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/runway` |
| `DB_USERNAME` | DB 사용자명 | `runway` |
| `DB_PASSWORD` | DB 비밀번호 | `runway` |
| `JWT_SECRET` | JWT 서명 키 **(필수, 기본값 없음)** | — |
| `JWT_ACCESS_TOKEN_EXPIRY` | Access Token 만료 시간 (ms) | `3600000` (1시간) |
| `JWT_REFRESH_TOKEN_EXPIRY` | Refresh Token 만료 시간 (ms) | `1209600000` (14일) |

---

## 문서

- [기술 명세서](docs/technical-specification.md) — 데이터베이스 설계, 기능 범위, 구현 규칙
- [API 명세서](docs/api-specification.md) — REST API 전체 스펙 (request/response body, error code, sequence)
- [Backend 통합 테스트 가이드](docs/backend-test-guide.md) — curl 기반 전체 플로우 테스트 시나리오

---

## GitHub Workflow

이 프로젝트는 경량화된 GitHub Flow 전략을 사용한다.

- 단일 GitHub repository에서 backend, android, docs, design을 함께 관리한다.
- `main` 브랜치는 항상 안정적인 상태를 유지한다.
- 구현 작업은 `feature/*`, `fix/*`, `docs/*`, `chore/*` 브랜치에서 진행한다.
- 솔로 개발 환경에서도 Pull Request 사용을 권장한다.
- 다음 파일은 절대 커밋하지 않는다: secrets, local config, `.idea/`, `.claude/`, `.DS_Store`, `build/`, `.gradle/`, `application-local.yml`

브랜치 명명 규칙, 커밋 메시지 형식, 빌드 확인 명령어, 권장 작업 흐름은 `CLAUDE.md` Section 13을 참고한다.

---

## 다음 단계

| 단계 | 작업 | 브랜치 |
|------|------|--------|
| **Phase B-7** | 완료된 런에서 코스 생성 (`POST /api/courses/from-run/{runId}`) | `feature/android-course-create` |
| **Phase B-8** | 주변 코스 탐색 — DiscoverScreen (`GET /api/courses/nearby`) | `feature/android-discover` |
| **Phase B-9** | 코스 상세 조회 | `feature/android-course-detail` |
| **Phase B-10** | 코스 도전 + 리더보드 | `feature/android-attempt-leaderboard` |
| Phase 2+ | GPS 경로 검증, 완주 인증 이미지, 코스 통계, 소셜 기능 | — |
