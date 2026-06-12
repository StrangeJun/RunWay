# RunWay

GPS로 달린 경로를 코스로 등록하고, 다른 러너들이 같은 코스에 도전해 리더보드에서 기록을 경쟁하는 소셜 러닝 앱.

**핵심 플로우:** `런 시작 → GPS 수집 → 런 완료 → 코스 생성 → 코스 탐색 → 코스 도전 → 리더보드`

---

## 개발 현황

| 영역 | 내용 | 상태 |
|------|------|------|
| Backend | Auth / User / Running / Course / Attempt / Leaderboard API | ✅ 완료 |
| Android | 인증, 러닝 추적, 코스 생성·탐색·도전, 리더보드, AI 자세 분석 | ✅ 완료 |
| Wear OS | Galaxy Watch 6 독립 실행, 코스 도전, 폰 연동 | ✅ 완료 |
| 배포 | Oracle Cloud (OCI) + GitHub Actions CI/CD | ✅ 운영 중 |

---

## Tech Stack

| 영역 | 기술 |
|------|------|
| **Backend** | Java 17, Spring Boot 3.x, Spring Security + JWT, Spring Data JPA |
| **Database** | PostgreSQL 15, PostGIS 3.4, Flyway |
| **Android** | Kotlin 2.0, Jetpack Compose, Hilt, Retrofit2, Room, Google Maps SDK |
| **Wear OS** | Kotlin, Compose Wear Material 3, HealthServices, Wearable DataLayer |
| **AI/ML** | MediaPipe BlazePose, TensorFlow Lite (On-Device) |
| **Infra** | Oracle Cloud (OCI), Docker, GitHub Actions |

---

## 주요 기능

### GPS 러닝 추적
- ForegroundService 기반 백그라운드 GPS 수집 (3초 간격)
- 비정상 포인트 자동 필터링 (속도·거리 기준)
- Room 기반 크래시 복구 — 앱 강제 종료 후 재시작 시 이어달리기 제안

### 코스 생성 및 도전
- 런 완료 후 GPS 경로를 코스로 등록
- PostGIS `ST_DWithin`으로 반경 내 코스 탐색
- 코스 도전 완주 기록을 `RANK() OVER` 윈도우 함수로 실시간 리더보드 집계
- Privacy Zone: 코스 시작/종료 지점 좌표 자동 마스킹

### Galaxy Watch 6 독립 실행
- 스마트폰 없이 GPS 추적·코스 도전 가능
- 경로 이탈 20m 기준 자동 일시정지 / 복귀 감지 자동 재개
- 경로 투영(Arc Length) 기반 진행도 계산
- AOD(Always-On Display) 저전력 화면 지원
- 런 완료 후 DataLayer로 폰에 자동 업로드

### AI 러닝 자세 분석 (On-Device)
- MediaPipe BlazePose 33개 관절 추출 → 12개 각도 계산
- TensorFlow Lite Autoencoder로 이상 자세 감지
- 영상을 서버로 전송하지 않고 기기 내에서 처리

---

## Project Structure

```
RunWay/
├── android/        # Kotlin Android 클라이언트 (Jetpack Compose)
├── backend/        # Java Spring Boot REST API 서버
├── wear-os/        # Kotlin Wear OS 클라이언트 (Galaxy Watch 6)
├── docs/           # 기술 명세서, API 명세서, 로드맵
├── design/         # Lovable 기반 UI 프로토타입
└── sample/         # 테스트용 Samsung Health GPX 파일
```

---

## Backend 로컬 실행

### 사전 요구사항
- Java 17 이상
- Docker Desktop (실행 중이어야 함)
- 포트 `5432` (PostgreSQL), `8080` (Spring Boot) 사용 가능

### 1. JWT_SECRET 설정

`backend/src/main/resources/application-local.yml` 파일을 생성한다. (Git에 커밋하지 않는다)

```yaml
jwt:
  secret: local-development-secret-must-be-at-least-32-characters-long
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

### 4. Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## 문서

- [기술 명세서](docs/technical-specification.md)
- [API 명세서](docs/api-specification.md)
- [프로젝트 종합 설명서](docs/project-overview.md)
- [개발 로드맵](docs/final-development-roadmap.md)
