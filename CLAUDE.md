# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RunWay — 러너를 위한 러닝 코스 탐색 및 공유 플랫폼.

## Repository Structure

```
RunWay/
├── backend/    # Spring Boot API 서버 (Java 17, Spring Boot 3.x)
├── android/    # Android 클라이언트 (개발 예정)
└── docs/       # 기술 명세서, API 명세서
```

## Backend

### Tech Stack
- Java 17, Spring Boot 3.x, Spring Security
- PostgreSQL 15 + PostGIS 3.4, Flyway, Spring Data JPA
- JWT (Access Token + Refresh Token)

### Commands

```bash
# DB 실행
cd backend && docker-compose up -d

# 서버 실행 (로컬)
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# 빌드
cd backend && ./gradlew build
```

### Key Conventions
- `application.yml`: 공통 설정, 환경변수 참조 (`${VAR}`)
- `application-local.yml`: 로컬 전용 설정 — gitignore로 제외되므로 커밋 금지
- `JWT_SECRET` 환경변수는 운영 환경에서 반드시 설정 필요 (미설정 시 서버 기동 실패)
- DB 마이그레이션은 Flyway로 관리 (`src/main/resources/db/migration/`)

## Development Status

- Phase 1-1: 프로젝트 초기 설정, DB 스키마 — 완료
- Phase 1-2: 회원가입 / 로그인 / JWT 인증 API — 완료
- Phase 2+: 러닝 코스 CRUD, Android 클라이언트 — 예정
