# RunWay — Product Gap Analysis

> 작성일: 2026-05-20  
> 작성 기준: Backend Phase 1 MVP 완료, Android Phase B-1 ~ B-15 완료  
> 비교 방법: 인터넷 접근 없음 — 경쟁 앱에 대한 일반 제품 지식(heuristic comparison) 기반

---

## 목차

1. [현재 RunWay 제품 요약](#1-현재-runway-제품-요약)
2. [경쟁 앱 비교 분석](#2-경쟁-앱-비교-분석)
3. [누락/취약 기능 카테고리 분석](#3-누락취약-기능-카테고리-분석)
4. [우선순위 로드맵](#4-우선순위-로드맵)
5. [권장 다음 Phase](#5-권장-다음-phase)
6. [지금 만들지 말아야 할 것들](#6-지금-만들지-말아야-할-것들)
7. [최종 권장 사항](#7-최종-권장-사항)

---

## 1. 현재 RunWay 제품 요약

### 이미 잘 되고 있는 것

| 영역 | 내용 |
|---|---|
| **인증** | 이메일 기반 회원가입/로그인, JWT Access+Refresh Token, OkHttp Interceptor 자동 재발급 |
| **GPS 추적** | ForegroundService 기반 안정적 백그라운드 추적, Fused Location Provider 3초/1초 간격, batch 전송 5초마다, 실패 시 re-queue |
| **러닝 기록** | 완료된 런 저장, My Runs 목록, Run Detail (지도 경로 포함) |
| **코스 생성** | 완료된 런에서 코스 생성, `running_points` 다운샘플링, draft/publish 상태 관리 |
| **인근 코스 탐색** | PostGIS `ST_DWithin()` + GiST 인덱스, 반경 1/3/5km 필터, 루프 필터 |
| **코스 상세** | 코스 경로 지도 표시(Google Maps), 코스 생성자, 거리, 도전/완주 카운트 |
| **코스 도전** | 도전 시작/완주/포기, `runningRecordId`와 `courseAttemptId` 분리 관리 |
| **리더보드** | `course_attempts`에서 `RANK() OVER` 집계, 완주자 순위 표시 |
| **프로필** | 닉네임/이메일 표시, 총 runs/거리 통계, My Runs 메뉴 |
| **지도** | Google Maps SDK + Maps Compose, `RouteMapView` (≥2포인트 실지도, 미만 Canvas 폴백) |

### RunWay의 핵심 차별화

다른 러닝 앱들이 개인 기록 저장 + 소셜 공유에 집중하는 반면, RunWay는:

1. **사용자 생성 코스(user-generated course)** — 사용자가 직접 달린 경로를 코스로 공개
2. **주변 코스 탐색(nearby discovery)** — 현재 위치 기반 GPS 탐색
3. **코스별 도전과 경쟁** — 동일 코스를 여러 사람이 도전하고 기록 비교
4. **코스 리더보드** — 코스 단위의 경쟁 구도

이 차별화는 Strava의 Segment와 유사하지만, 사용자가 코스를 직접 만든다는 점에서 더 능동적이다.

---

## 2. 경쟁 앱 비교 분석

> 주의: 아래 내용은 일반 제품 지식 기반의 heuristic comparison입니다. 최신 기능 변경이 반영되지 않을 수 있습니다.

### 2-1. Strava

**핵심 강점**
- Segment 기능 (특정 구간 기록 자동 측정 및 순위)
- Club / Activity Feed (소셜 그래프)
- 상세 분석: 1km splits, 페이스 차트, 고도 프로파일, 심박수 존
- 연간/월간 summary, Personal Records

**참고할 UX 패턴**
- 런 완료 직후 즉시 통계 요약 표시 + "Create Course" 제안
- 지도 위에 경로와 Segment 구간 시각화
- My Year in Review 형태의 연간 리포트

**RunWay가 고려해야 할 것**
- 1km splits (랩 타임) — 현재 완전히 없음
- Personal Record 감지 및 알림
- 런 완료 후 "이 경로로 코스 만들기" 제안 UX 개선 (현재 버튼 존재하지만 흐름이 약함)

**RunWay가 지금 피해야 할 것**
- 전체 Activity Feed (소셜 그래프 설계 복잡도)
- Flyby / Beacon 등 실시간 위치 공유 기능

---

### 2-2. Nike Run Club

**핵심 강점**
- 가이드드 런(Audio Guided Run) — 코치 음성 안내
- 온보딩 플로우가 명확 (목표 설정 → 레벨 선택)
- 런 중 실시간 거리/페이스 음성 고지 (km당 자동)
- Nike 브랜드 챌린지

**참고할 UX 패턴**
- 런 시작 화면의 간결함 (버튼 하나)
- 자동 km 마일스톤 알림 (1km, 2km, ... 경과 시 음성/진동)
- 런 완료 화면에서 동기 부여 메시지

**RunWay가 고려해야 할 것**
- km 마일스톤 진동/알림 — 구현 난이도 낮고 효과 높음
- 런 완료 후 짧은 격려 메시지 (현재 "Run complete" 텍스트만 있음)
- 런 시작 전 목표 거리/시간 설정

**RunWay가 지금 피해야 할 것**
- 오디오 가이드 런 (콘텐츠 제작 필요)
- 브랜드 챌린지 (마케팅 도메인)

---

### 2-3. ASICS Runkeeper

**핵심 강점**
- 훈련 계획(Training Plan) — 목표 경기 기반 주간 플랜
- 자동 일시정지(Auto-Pause) — 신호등 대기 등에서 자동 감지
- 런 중 실시간 오디오 통계 (페이스, 거리, 시간)

**참고할 UX 패턴**
- Auto-Pause: 속도 임계값(예: 0.5 m/s 미만) 기반 자동 일시정지
- 런 후 캘린더 뷰 (날짜별 운동 시각화)

**RunWay가 고려해야 할 것**
- **Auto-Pause** — 현재 RunWay에 없는 가장 중요한 추적 품질 기능
- 런 기록 캘린더 뷰 (현재 단순 리스트만 있음)

**RunWay가 지금 피해야 할 것**
- Training Plan (코칭 콘텐츠 필요)
- 인앱 음악 제어 (플랫폼 통합 복잡도)

---

### 2-4. Adidas Running (Runtastic)

**핵심 강점**
- 루트 계획 기능 (지도 위에서 코스 미리 그리기)
- 스토리 챌린지 (가상 여행 형태의 장기 목표)
- 구독 기반 프리미엄 기능 (페이스 코치, 고급 통계)

**참고할 UX 패턴**
- 코스 탐색에서 거리별 필터 + 난이도 태그 제공
- 런 후 공유 이미지 자동 생성 (Certification Image)

**RunWay가 고려해야 할 것**
- **Certification Image** — 완주 기록 공유 이미지 (Phase 2 예정, 우선순위 높음)
- 코스 난이도 태그

**RunWay가 지금 피해야 할 것**
- 지도 위에서 코스를 미리 그리는 루트 계획 기능 (GPS 기반 RunWay 차별화와 방향이 다름)
- 구독 모델 (아직 사용자 기반 없음)

---

### 2-5. Garmin Connect

**핵심 강점**
- 웨어러블 연동 깊이 (심박수, VO2 Max, 회복 시간)
- 고급 분석: Body Battery, stress score, sleep
- 구간별 페이스 분석 (cadence, stride length)

**참고할 UX 패턴**
- 통계 대시보드 (주간/월간 차트)
- 내 최고 기록(Personal Records) 섹션 — Best 5km, 10km, 하프/풀 마라톤

**RunWay가 고려해야 할 것**
- Personal Records 섹션 (현재 없음) — 사용자 리텐션에 직결
- 월간 통계 차트 (현재 주간 통계만 있음)

**RunWay가 지금 피해야 할 것**
- 웨어러블 연동 (하드웨어 에코시스템 의존도)
- VO2 Max, Body Battery 등 생체 분석 (의료기기 수준 센서 필요)

---

### 2-6. Apple Fitness / Workout App

**핵심 강점**
- OS 수준 통합 (건강 앱 데이터 공유)
- 활동 링 (이동/운동/서있기) 시각화
- Watch와의 통합으로 심박수, ECG, 혈중 산소 측정

**참고할 UX 패턴**
- 진행 중 런의 실시간 원형 링 시각화
- 런 요약 카드 (거리, 시간, 칼로리, 페이스를 2×2 그리드로 표시) — RunWay RunResultScreen과 유사한 패턴

**RunWay가 고려해야 할 것**
- 런 완료 카드 레이아웃 (현재 RunResultScreen의 2×2 구조는 이미 참고 중)
- 통계 시각화의 간결성

**RunWay가 지금 피해야 할 것**
- Apple Health / Google Fit 연동 (시스템 API 통합 복잡도)

---

### 경쟁 앱 비교 요약표

| 기능 | Strava | NRC | Runkeeper | Adidas | Garmin | RunWay |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| GPS 추적 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 코스 생성 | ✅(Segment) | ❌ | ❌ | ✅(계획) | ❌ | ✅ |
| 주변 코스 탐색 | △(Segment) | ❌ | △ | △ | ❌ | ✅ |
| 코스 리더보드 | ✅(Segment) | ❌ | ❌ | ❌ | △ | ✅ |
| Auto-Pause | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| 1km Splits | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| 페이스 차트 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| Personal Records | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| 완주 인증 이미지 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| 소셜 피드 | ✅ | △ | △ | △ | △ | ❌ |
| 온보딩 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |

---

## 3. 누락/취약 기능 카테고리 분석

### A. 러닝 추적 품질

| 문제 | 현황 | 심각도 |
|---|---|:---:|
| **Auto-Pause 없음** | 신호등, 휴식 중에도 타이머와 거리가 계속 누적됨. `DefaultLocationTracker`는 속도 임계값 필터 없음. | 🔴 |
| **비정상 GPS 포인트 필터링 없음** | 건물 사이, 터널 등에서 튀는 포인트가 거리에 그대로 합산됨. `distanceKm += delta` 무조건 누적. | 🔴 |
| **1km Splits 없음** | 런 중 km당 랩 타임이 없음. 런 완료 후 분석도 없음. | 🟠 |
| **페이스 스무딩 없음** | 현재 전체 평균 페이스만 표시. 실시간 페이스가 GPS 오차로 급격히 변동될 수 있음. | 🟡 |
| **배터리 최적화** | GPS 인터벌 고정(3초). 배터리 절약 모드나 적응형 인터벌 없음. | 🟡 |
| **백그라운드 앱 종료 시 복구** | ForegroundService로 보호하지만, 앱 프로세스 강제 종료 시 pendingPoints 손실 위험. 로컬 큐 영속화 없음. | 🟡 |
| **Pause 중 GPS 포인트 수집 계속** | `pause()` 시 타이머/거리는 멈추지만 locationJob이 계속 실행됨. pause 중 포인트가 큐에 쌓일 수 있음. | 🟡 |

### B. 러닝 분석

| 문제 | 현황 | 심각도 |
|---|---|:---:|
| **1km Splits 없음** | `RunDetailScreen`에 없음. 백엔드도 splits 저장 없음. | 🔴 |
| **페이스/속도 차트 없음** | 시계열 GPS 데이터는 있으나 시각화 없음. | 🟠 |
| **고도 차트 없음** | `running_points.altitude_meters` 저장하지만 표시 없음. | 🟡 |
| **Personal Records(PR) 없음** | 최고 기록 추적 없음. 5km PR, 10km PR 등 사용자 동기부여 핵심 기능. | 🔴 |
| **월간/연간 통계 없음** | `HomeViewModel`에 주간 통계만 있음. 장기 트렌드 없음. | 🟠 |
| **케이던스(cadence) 없음** | GPX에 `<gpxtpx:cad>` 있으나 파싱/저장 안 함. | 🟡 |

### C. 코스 경험

| 문제 | 현황 | 심각도 |
|---|---|:---:|
| **코스 검색/키워드 검색 없음** | `DiscoverScreen`의 검색 바가 비기능(UI 장식만). 이름 검색 API 없음. | 🔴 |
| **코스 난이도/태그 없음** | `courses` 테이블에 태그/난이도 컬럼 없음. | 🟠 |
| **코스 평가(rating) 없음** | 사용자가 코스 품질을 평가할 수단 없음. 저품질 코스 필터링 불가. | 🟠 |
| **코스 신고(report) 없음** | 부적절하거나 위험한 코스를 신고할 방법 없음. | 🟠 |
| **내 코스 관리 화면 없음** | `GET /api/courses/me` API는 있으나 Android UI 없음. Profile에서 내 코스 리스트 접근 불가. | 🟠 |
| **코스 도전 중 지도 경로 오버레이 없음** | `CourseAttemptTrackingScreen`이 `RouteMapPlaceholder`(Canvas placeholder) 사용. 실제 코스 경로가 지도에 표시되지 않음. | 🔴 |
| **코스 완주율 표시 약함** | `attemptCount`/`completionCount`는 있으나 완주율(%) 표시 없음. | 🟡 |

### D. 경쟁 및 리더보드 무결성

| 문제 | 현황 | 심각도 |
|---|---|:---:|
| **Global Leaderboard 탭이 더미 데이터** | `LeaderboardScreen`(탭)이 하드코딩된 DUMMY_ENTRIES 표시. 실제 API 미연결. | 🔴 |
| **GPS 경로 검증 없음** | 완주 시 자동 `verified` 처리. 코스를 실제로 달렸는지 검증 없음 (Phase 2 예정). | 🔴 |
| **이상 기록 감지 없음** | 비정상적으로 빠른 완주 기록(예: 5km를 1분) 필터링 없음. | 🟠 |
| **중복 도전 제한 없음** | 동일 코스를 연속으로 여러 번 도전하여 리더보드를 의도적으로 채울 수 있음. | 🟡 |
| **비공개 시도(private attempt) 없음** | 모든 완주 기록이 자동으로 리더보드에 노출됨. | 🟡 |

### E. 사용자 리텐션

| 문제 | 현황 | 심각도 |
|---|---|:---:|
| **목표 설정 없음** | 주간 거리 목표, 월간 런 횟수 목표 등 없음. | 🔴 |
| **Personal Records(PR) 없음** | 최고 기록 갱신 알림 없음. 사용자가 앱을 계속 열 이유가 약함. | 🔴 |
| **연속 달성(Streak) 없음** | 연속 러닝 일수 추적 없음. | 🟠 |
| **업적/배지(Achievement) 없음** | 첫 5km, 100km 누적 등 마일스톤 없음. | 🟠 |
| **알림 없음** | 런 목표 리마인더, 코스 새 도전자 알림 등 없음. | 🟠 |
| **주간 요약 없음** | 앱 내 주간 리포트 없음. HomeScreen의 WeeklyStats는 단순 숫자 표시. | 🟡 |

### F. 프라이버시 및 안전

| 문제 | 현황 | 심각도 |
|---|---|:---:|
| **Privacy Zone 없음** | 코스 시작/종료점이 집 앞인 경우 GPS 좌표가 그대로 공개됨. | 🔴 |
| **비공개 런 없음** | 모든 런이 기본적으로 나의 기록으로 저장되며 코스 생성 시 공개 선택 가능하지만, 런 기록 자체의 공개/비공개 설정 없음. | 🟠 |
| **이상 위치 노출** | `courses.start_location`이 정확한 좌표로 공개됨. 반경 조정 없음. | 🔴 |

### G. 소셜 / 공유

| 문제 | 현황 | 심각도 |
|---|---|:---:|
| **완주 인증 이미지 없음** | Phase 2 예정이지만 실제 사용자 획득/공유에 가장 직접적인 기능. | 🔴 |
| **공유 기능 없음** | 런 기록이나 코스를 외부 SNS에 공유할 방법 없음. | 🟠 |
| **친구/팔로우 없음** | 아는 사람의 코스를 탐색하거나 기록을 볼 수 없음. | 🟡 (Phase 2+) |

### H. UX 폴리시

| 문제 | 현황 | 심각도 |
|---|---|:---:|
| **온보딩 없음** | 앱 최초 실행 시 Login/Signup 화면으로 바로 이동. 앱이 무엇을 하는지 설명 없음. | 🔴 |
| **HomeScreen Nearby Courses 빈칸** | "Nearby courses" 섹션이 빈 Row. `DiscoverScreen`에서 로드하라는 주석만 있고 실제 데이터 없음. | 🔴 |
| **런 완료 메시지 고정** | `RunResultScreen`에 "Free run" 하드코딩. 코스 기반 런에서도 동일하게 표시. | 🟠 |
| **빈 상태(empty state) 개선 여지** | "주변에 코스가 없습니다" 등 텍스트만 있고 CTA(코스 만들기) 미약. | 🟡 |
| **오프라인 처리** | 네트워크 없을 때 에러 메시지 표시는 있으나 오프라인 캐싱 없음. | 🟡 |
| **런 권한 설명** | Location permission 요청 전 설명 카드는 있으나 거부 시 재요청 플로우 없음. | 🟡 |
| **다크/라이트 테마** | Material 3 기반으로 시스템 테마 대응은 되어 있으나 명시적 선택 없음. | 🟢 (양호) |

---

## 4. 우선순위 로드맵

### 우선순위 정의

| 레벨 | 의미 |
|---|---|
| **P0** | 실제 사용자 테스트 전 반드시 수정해야 할 결함 수준의 문제 |
| **P1** | MVP 폴리시를 위한 중요 기능 |
| **P2** | RunWay의 차별화를 강화하는 기능 |
| **P3** | 이후 개선 사항 |

---

### P0 — 실제 사용자 테스트 전 필수

| # | 기능 | 현재 갭 | Backend 영향 | Android 영향 | 복잡도 |
|---|---|---|:---:|:---:|:---:|
| P0-1 | **코스 도전 중 실제 코스 경로 지도 표시** | `CourseAttemptTrackingScreen`이 Canvas placeholder 사용. 달리는 경로 위에 목표 코스가 표시되지 않아 UX 핵심 가치 반감 | ❌ | ✅ | Low |
| P0-2 | **Global Leaderboard 탭 실제 API 연결** | `LeaderboardScreen`이 하드코딩된 더미 데이터 사용. 실제 데이터 없음 | ❌ | ✅ | Low |
| P0-3 | **GPS 비정상 포인트 필터링** | 속도 임계값 없이 모든 GPS 델타 누적 → 빌딩 반사, 터널 등에서 거리 오염 | ❌ | ✅ | Low |
| P0-4 | **HomeScreen Nearby Courses 빈 섹션 수정** | 비어있는 Row가 표시됨. 제거하거나 실제 데이터 연결 필요 | ❌ | ✅ | Low |
| P0-5 | **Privacy Zone(시작점 좌표 마스킹)** | 코스 시작/종료 GPS 좌표가 정밀하게 공개됨. 집 앞에서 시작한 코스 위험 | ✅ | ✅ | Medium |

---

### P1 — MVP 폴리시를 위한 중요 기능

| # | 기능 | 왜 중요한가 | 현재 갭 | 구현 Phase | Backend 영향 | Android 영향 | 복잡도 |
|---|---|---|---|:---:|:---:|:---:|:---:|
| P1-1 | **Auto-Pause** | 신호등, 음수대 정지 때 타이머 멈춤. 모든 경쟁 앱이 제공. 없으면 페이스가 부정확해짐 | `DefaultLocationTracker`에 속도 임계값 없음 | B-16 | ❌ | ✅ | Low |
| P1-2 | **1km Splits** | 런 분석의 기본. 구간 페이스 파악. 훈련 피드백 핵심 | `RunDetailScreen`에 없음. 백엔드는 포인트 데이터 있음 | B-16 | △(계산만) | ✅ | Medium |
| P1-3 | **Personal Records(PR)** | 앱을 계속 켜는 이유 1위. "오늘 5km PR 달성" 순간이 리텐션 핵심 | 전혀 없음 | B-17 | ✅(PR 집계 API) | ✅ | Medium |
| P1-4 | **내 코스 관리 화면** | `GET /api/courses/me` API 있지만 Android UI 없음. 내가 만든 코스 확인 불가 | Profile에 메뉴 없음 | B-16 | ❌ | ✅ | Low |
| P1-5 | **완주 인증 이미지(Certification Image)** | 소셜 공유의 기반. "나 이 코스 완주했어" 카드 이미지. 신규 사용자 유입 효과 | Phase 2 예정이지만 사용자 가치 높음 | B-18 | ✅(S3, 이미지 합성) | ✅ | High |
| P1-6 | **온보딩 플로우** | 앱 첫 실행 시 RunWay의 개념 설명 없음. 코스 기반 경쟁이라는 차별점 설명 필요 | 없음 | B-16 | ❌ | ✅ | Low |
| P1-7 | **런 완료 후 코스 생성 CTA 개선** | 완료 직후 "이 경로로 코스 만들기" 제안이 자연스럽게 이어져야 RunWay의 코어 루프 완성 | `RunResultScreen`에 버튼 있으나 UX 흐름 약함 | B-16 | ❌ | ✅ | Low |

---

### P2 — 차별화 강화 기능

| # | 기능 | 왜 중요한가 | 현재 갭 | 구현 Phase | Backend 영향 | Android 영향 | 복잡도 |
|---|---|---|---|:---:|:---:|:---:|:---:|
| P2-1 | **GPS 경로 검증** | 리더보드 무결성. 코스를 실제로 달렸는지 GPS 경로 매칭 | Phase 2 예정. `verification_status` 컬럼 있음 | B-19+ | ✅(고 | ✅ | High |
| P2-2 | **코스 검색** | `DiscoverScreen`의 검색 바가 장식. 이름 기반 코스 검색 필요 | API 없음 | B-17 | ✅ | ✅ | Medium |
| P2-3 | **코스 난이도/태그** | 탐색 시 "쉬운 코스", "경치 좋은 코스" 선택 가능하면 탐색 UX 향상 | 스키마 변경 필요 | B-17 | ✅ | ✅ | Medium |
| P2-4 | **월간/연간 통계 + 추이 차트** | 장기 성장 확인. 리텐션 강화 | 주간 통계만 있음 | B-17 | ✅ | ✅ | Medium |
| P2-5 | **주간 목표 설정** | 주간 거리/런 횟수 목표. HomeScreen에 달성률 링 표시 | 없음 | B-18 | ✅(로컬 가능) | ✅ | Medium |
| P2-6 | **km 마일스톤 알림** | 런 중 1km, 2km 진동/알림. 페이스 고지 | 없음 | B-16 | ❌ | ✅ | Low |
| P2-7 | **코스 평가 시스템** | 저품질 코스 필터링. 리더보드 신뢰도 향상 | 없음 | B-18 | ✅ | ✅ | Medium |

---

### P3 — 이후 개선 사항

| # | 기능 | 구현 Phase | 복잡도 |
|---|---|:---:|:---:|
| P3-1 | Streak (연속 달성) | B-19 | Low |
| P3-2 | Achievement/Badge | B-19 | Medium |
| P3-3 | 페이스/속도 차트 | B-18 | Medium |
| P3-4 | 고도 프로파일 차트 | B-18 | Medium |
| P3-5 | 코스 신고(report) 시스템 | B-19 | Medium |
| P3-6 | Push Notification (리마인더, PR 알림) | B-19 | Medium |
| P3-7 | 런 기록 캘린더 뷰 | B-19 | Medium |

---

## 5. 권장 다음 Phase

B-15(Google Maps Route Preview) 완료 기준으로, 코드 분석 및 경쟁 앱 비교 결과를 바탕으로 제안하는 다음 5개 Phase:

---

### B-16: Running Quality & Core UX Polish

**핵심 목적:** 실제 사용자 테스트 직전 결함 수준의 문제 수정 + 기본 품질 확보

| 작업 | 타입 | 상세 |
|---|---|---|
| Auto-Pause 구현 | Android | `DefaultLocationTracker`에서 speed < 0.3 m/s 지속 시 자동 pause. 설정에서 on/off |
| GPS 이상 포인트 필터링 | Android | 속도 > 15 m/s (54 km/h)인 포인트 무시. 이전 포인트와 거리가 200m 이상이면 드롭 |
| 코스 도전 중 코스 경로 지도 표시 | Android | `CourseAttemptTrackingScreen`에 `RouteMapView` + 현재 GPS 위치 마커 |
| Global Leaderboard 탭 실제 API 연결 | Android | `LeaderboardScreen`에서 더미 데이터 → 실제 코스별 리더보드 API |
| HomeScreen Nearby Courses 수정 | Android | 빈 Row 제거 또는 `DiscoverViewModel` 데이터 활용 |
| 1km Splits 표시 | Android | `RunDetailScreen`에 GPS 포인트 기반 km당 페이스 테이블 |
| 내 코스 관리 화면 | Android | Profile → "My Courses" 메뉴, `GET /api/courses/me` 연결 |
| 온보딩 3-스텝 | Android | 앱 최초 실행 시: RunWay 소개 → 코스 개념 → 시작 |
| km 마일스톤 진동 알림 | Android | 런 중 1km, 2km마다 진동 + 경과 페이스 표시 |

**Backend 영향:** 없음  
**Android 영향:** 크다 (여러 화면 수정)  
**브랜치:** `feature/android-running-quality`

---

### B-17: Run Analysis & Personal Records

**핵심 목적:** 사용자가 앱을 다시 열 이유 제공 — 기록 분석과 성장 확인

| 작업 | 타입 | 상세 |
|---|---|---|
| Personal Records API | Backend | `GET /api/runs/me/records`: best 5km, 10km, longest run, fastest pace. `running_records` 집계 |
| Personal Records 화면 | Android | Profile → "Personal Records" 섹션. PR 갱신 시 강조 표시 |
| 코스 검색 API | Backend | `GET /api/courses/nearby?keyword=탄금대` — name ILIKE 검색 |
| 코스 검색 UI | Android | `DiscoverScreen` 검색 바 활성화 |
| 월간 통계 API | Backend | `GET /api/runs/me/stats?period=monthly` |
| 월간/연간 통계 화면 | Android | Profile 또는 별도 Stats 탭에 막대 차트 |
| Privacy Zone | Backend/Android | 코스 `start_location`을 반경 150m offset 처리. 설정 화면에서 활성화 |

**Backend 영향:** 중간  
**Android 영향:** 중간  
**브랜치:** `feature/android-run-analysis`

---

### B-18: Certification Image & Sharing

**핵심 목적:** 사용자 획득 채널 — 외부 공유로 신규 사용자 유입

| 작업 | 타입 | 상세 |
|---|---|---|
| S3 또는 Object Storage 연동 | Backend | 이미지 업로드 인프라 |
| Certification Image 생성 API | Backend | `POST /api/course-attempts/{attemptId}/certification-image`. 코스명, 완주 시간, 지도 썸네일 합성 |
| Certification Image 화면 | Android | 코스 완주 후 완주 카드 표시. "공유하기" 버튼 → Android ShareSheet |
| 런 결과 공유 이미지 | Android | `RunResultScreen`에서 자유 런 결과 공유 이미지 생성 (Canvas 기반) |
| 주간 목표 설정 | Android | HomeScreen에서 주간 거리/런 목표 설정. DataStore 로컬 저장. 달성률 표시 |
| 코스 난이도/태그 | Backend/Android | `courses` 테이블에 `difficulty` 컬럼 추가(Flyway). `DiscoverScreen` 필터 |

**Backend 영향:** 높음 (S3, 이미지 합성)  
**Android 영향:** 중간  
**브랜치:** `feature/android-sharing`

---

### B-19: Leaderboard Integrity & Course Quality

**핵심 목적:** RunWay 차별화 기능(코스 리더보드)의 신뢰도 확보

| 작업 | 타입 | 상세 |
|---|---|---|
| GPS 경로 검증 (Phase 2) | Backend | 완주 GPS 경로가 코스 경로와 Hausdorff distance 임계값 이내인지 검증. `verification_status = 'verified'` 조건부 설정 |
| 이상 기록 감지 | Backend | 완주 시간이 코스 거리/최고 이론 속도보다 빠른 경우 자동 `rejected` |
| 코스 평가(rating) | Backend/Android | 완주 후 코스 1~5점 평가. `courses` 테이블 `avg_rating` 컬럼 |
| 코스 신고 | Backend/Android | 코스 상세에서 신고 버튼 → 관리자 검토 큐 |
| Streak 추적 | Android | 연속 러닝 일수 계산 및 HomeScreen 표시 |
| Achievement/Badge | Backend/Android | 첫 런, 첫 코스 생성, 첫 완주, 100km 달성 등 |
| Push Notification | Android | FCM 연동. PR 달성, 새 코스 도전자, 주간 리마인더 |

**Backend 영향:** 높음  
**Android 영향:** 중간  
**브랜치:** `feature/android-leaderboard-integrity`

---

### B-20: UX Polish & Retention Loop

**핵심 목적:** 장기 사용 유도 및 전반적인 UX 완성도

| 작업 | 타입 | 상세 |
|---|---|---|
| 런 기록 캘린더 뷰 | Android | My Runs에 월별 캘린더 탭. 운동한 날 표시 |
| 페이스/속도 차트 | Android | RunDetailScreen에 시계열 페이스 차트 |
| 고도 프로파일 | Android | RunDetailScreen에 고도 차트 |
| 코스 도전 기록 (내 코스 시도) | Android | CourseDetailScreen에 내 이전 도전 기록 표시 |
| 오프라인 큐 영속화 | Android | Room DB로 pendingPoints 영속화. 앱 재시작 후에도 포인트 복구 |
| 빈 상태 개선 | Android | 모든 빈 상태에 일러스트 + 명확한 CTA |
| 접근성 개선 | Android | contentDescription 보완, 최소 터치 영역 48dp 확인 |

---

## 6. 지금 만들지 말아야 할 것들

### ❌ 전체 소셜 피드 (Activity Feed)

**이유:** 소셜 그래프 설계(팔로우/팔로워 테이블, 피드 집계 쿼리, 알림 시스템)는 독립적인 복잡도를 갖는다. 현재 사용자 기반이 없는 상태에서 피드를 만들면 빈 피드만 보인다. 코어 러닝/코스 루프가 안정화된 이후 의미가 생긴다.

### ❌ AI 이미지 생성

**이유:** LLM API 비용, 프롬프트 엔지니어링, 이미지 품질 관리가 복잡하다. Canvas 기반 Certification Image로 충분히 공유 가능하다.

### ❌ 고급 훈련 계획 (Training Plan)

**이유:** 코칭 콘텐츠와 도메인 지식이 필요하다. 러닝 전문 코치가 설계한 훈련 플랜이 없으면 경쟁 앱 대비 열위다. RunWay의 차별화인 "코스 기반 경쟁"과 방향이 다르다.

### ❌ 수익화 모델 (구독/인앱 결제)

**이유:** 사용자가 없는 상태에서 구독 모델을 먼저 설계하면 기능 우선순위가 비즈니스 로직에 오염된다. 사용자 리텐션 메트릭이 안정화된 이후 설계한다.

### ❌ 웨어러블 연동

**이유:** Wear OS, Galaxy Watch, Apple Watch 각각 별도 SDK와 에코시스템이 필요하다. MVP 단계에서는 스마트폰 GPS로 충분하다.

### ❌ 복잡한 지도 편집 (코스 수동 그리기)

**이유:** RunWay의 핵심 가치는 "실제로 달린 경로를 코스로 만든다"는 것이다. 지도 위에서 마우스로 코스를 그리는 기능은 이 차별화와 반대 방향이다. GPS 기반 생성 품질을 먼저 높이는 것이 우선이다.

### ❌ 대규모 추천 엔진

**이유:** 추천 알고리즘은 충분한 사용자 행동 데이터가 쌓인 후에 의미가 있다. 초기에는 PostGIS `ST_Distance` 기반 거리순 정렬로 충분하다.

### ❌ 그룹 런 (실시간 위치 공유)

**이유:** WebSocket 기반 실시간 위치 공유는 인프라 복잡도(서버 부하, 연결 관리)가 매우 높다. 개인 러닝 기록이 안정화된 후에 도입한다.

---

## 7. 최종 권장 사항

### Google Maps SDK를 지금 계속 진행해야 하나?

**Yes, 단 B-16의 CourseAttemptTrackingScreen 업데이트에 집중한다.** B-15에서 `RunDetailScreen`, `CourseDetailScreen`, `RunResultScreen`에 Google Maps를 추가했다. 남은 가장 중요한 지도 작업은 **코스 도전 중 실제 코스 경로가 지도에 표시되는 것**인데, 이것이 없으면 RunWay의 핵심 가치인 "코스를 달린다"는 경험이 완성되지 않는다. 이 하나만 추가하면 지도 관련 작업의 우선순위는 완료된다.

### 러닝 품질과 분석을 먼저 해야 하나?

**Yes, Auto-Pause와 GPS 필터링이 P0 수준이다.** 현재 GPS 이상 포인트 필터링 없이 거리가 계산되고, Auto-Pause가 없어서 신호 대기 중에도 타이머가 계속 돈다. 이 두 가지는 사용자가 첫날 경험하는 기본 품질 문제다. 실제 사용자 테스트 전에 반드시 수정해야 한다.

### 가장 좋은 다음 구현 Phase는?

```
B-16 (Running Quality & Core UX Polish)
```

구체적 이유:
1. P0 결함(CourseAttemptTrackingScreen 지도 없음, Leaderboard 더미 데이터, HomeScreen 빈 섹션)을 모두 해결한다
2. Auto-Pause + GPS 필터링으로 추적 품질이 경쟁 앱 수준으로 올라간다
3. 온보딩, 내 코스 화면, km 마일스톤 알림 등 폴리시 작업으로 전반적인 완성도가 높아진다
4. Backend 변경이 거의 없어 빠르게 진행 가능하다
5. B-16 완료 후 실제 사용자 테스트를 진행하고, 피드백을 반영하여 B-17(PR + 분석)로 이어가는 것이 권장 순서다

---

## 부록: 분석 전제 및 한계

1. **heuristic comparison** — 경쟁 앱의 현재 기능은 인터넷 접근 없이 일반 제품 지식에 기반한다. 최신 기능 추가/제거가 반영되지 않았을 수 있다.
2. **코드 기반 분석** — RunWay 현황은 실제 소스 코드(`android/`, `backend/`, `docs/`) 직접 분석 기반이다.
3. **Phase 추정** — 각 작업의 복잡도와 Phase 배치는 현재 코드 구조 기반 추정이며, 실제 구현 중 조정이 필요할 수 있다.
4. **`LeaderboardScreen` 더미 데이터** — 소스 코드에서 `DUMMY_ENTRIES` 하드코딩 확인. `CourseLeaderboardScreen`(코스 상세에서 접근)은 실제 API 연결 상태. Global Leaderboard 탭만 미연결.
