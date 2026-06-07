# RunWay Product Gap Analysis Review

## 1. Summary

`docs/product-gap-analysis.md`는 RunWay의 제품 방향과 주요 gap을 잡는 데는 유용하지만, 실제 codebase 기준으로는 몇 가지 핵심 부정확성이 있다.

특히 Android에 이미 Google Maps 기반 `RouteMapView`가 들어와 있고, backend Phase 1 API도 대부분 구현되어 있다는 점은 잘 반영되어 있다. 반면 `GPS 비정상 포인트 필터링 없음`, `현재 위치 기반 nearby discovery`, `pause 중 GPS point queue 적재` 같은 항목은 실제 코드와 다르거나 추가 설명이 필요하다.

가장 중요한 조정은 priority다. 현재 문서는 feature polish와 share/retention 기능을 비교적 높게 두고 있지만, 실제 사용자 테스트 전에는 `tracking reliability`, `crash recovery`, `raw GPS point quality`, `privacy zone`을 먼저 닫는 것이 더 실용적이다.

## 2. Accuracy Check

| Area | Document claim | Actual codebase status | Verdict | Notes |
|---|---|---|---|---|
| Backend APIs | Auth/User/Running/Course/Attempt/Leaderboard API 구현 완료 | 실제 controller/service/repository가 구현되어 있음 | Accurate | `backend/src/main/java/com/runway/*/controller`, `*Service.java` 확인 |
| Auth | 이메일 회원가입/로그인, JWT Access+Refresh Token, OkHttp 자동 재발급 | Android `TokenAuthenticator`와 backend `AuthService.reissue()` 구현 | Accurate | Access token 만료 후 refresh token으로 재발급 가능 |
| Running API | start/points/pause/resume/finish/abandon/list/detail/delete 구현 | backend는 모두 구현, Android는 delete 미연결 | Partially accurate | Android `RunningApi`에는 `DELETE /api/runs/{runId}` 없음 |
| Course API | from-run, nearby, me, detail, points, update, publish, archive 구현 | backend는 구현, Android는 create/nearby/detail/points 중심 연결 | Partially accurate | Android `CourseApi`에 `GET /api/courses/me`, update/publish/archive 없음 |
| Course Attempt API | start/finish/abandon/leaderboard/my attempts 구현 | backend는 구현, Android는 my attempts 미연결 | Partially accurate | Android `CourseAttemptApi`에 `GET /api/courses/{courseId}/attempts/me` 없음 |
| Android screens | 러닝, 기록, 코스 생성, 탐색, 상세, 도전, 리더보드, 프로필 구현 | 주요 화면은 실제 구현됨 | Accurate | `RunwayNavGraph` 기준으로 연결 확인 |
| Run Detail map | Run Detail에 지도 경로 포함 | `RunDetailScreen`에서 `RouteMapView` 사용 | Accurate | Google Maps route preview 사용 |
| Course Detail map | Course Detail에 코스 경로 지도 표시 | `CourseDetailScreen`에서 `RouteMapView` 사용 | Accurate | course points 기반 표시 |
| Run Result map | Run Result에 지도 route preview 표시 | `RunResultViewModel`이 run detail points를 로드해 표시 | Accurate | run 완료 후 route preview 있음 |
| Course Attempt map | 도전 중 실제 코스 경로 지도 미표시 | `CourseAttemptTrackingScreen`은 `RouteMapPlaceholder` 사용 | Accurate | 코스 목표 route overlay 없음 |
| Global Leaderboard | bottom tab leaderboard가 dummy data | `LeaderboardScreen`에 `DUMMY_ENTRIES` 하드코딩 | Accurate | API 미연결 |
| Course Leaderboard | 코스별 leaderboard API 기반 | `CourseLeaderboardScreen`/ViewModel이 실제 API 사용 | Accurate | course detail에서 접근 가능 |
| GPS tracking | ForegroundService 기반 tracking | `RunTrackingService` + `RunTrackingManager` 구현 | Accurate | 단, crash recovery는 취약 |
| GPS abnormal point filtering | 비정상 GPS point filtering 없음 | distance delta에는 10 m/s 초과 implied speed filter 있음 | Inaccurate | raw point upload/path에는 여전히 취약 |
| Pause 중 point 수집 | pause 중 GPS point가 queue에 쌓일 수 있음 | pause 상태에서는 point를 queue에 추가하지 않음 | Inaccurate | 위치 상태만 갱신 |
| Batch upload | 5초마다 batch 전송, 실패 시 requeue | `RunningTrackingViewModel`, `CourseAttemptTrackingViewModel`에서 구현 | Accurate | memory queue 기반 |
| Nearby discovery | 현재 위치 기반 주변 코스 탐색 | Android는 fixed test coordinate 사용 | Partially accurate | `DiscoverViewModel.TEST_LATITUDE`, `TEST_LONGITUDE` |
| Home nearby courses | HomeScreen nearby section 비어 있음 | 실제 빈 `Row`와 placeholder comment 존재 | Accurate | CTA도 동작 연결 없음 |
| Splits | 1km splits 없음 | backend/android 모두 split model/UI 없음 | Accurate | run points는 있어 계산 가능 |
| Auto-Pause | 없음 | 자동 pause 로직 없음 | Accurate | manual pause만 있음 |
| Privacy Zone | 없음 | DB/API가 precise start/end location 저장 및 노출 | Accurate | course publish 안전 gap |
| Leaderboard integrity | GPS route verification 없음 | finish attempt 시 자동 `verified` 처리 | Accurate | Phase 1 단순화 |
| Offline handling | offline cache 없음 | pending queue는 memory only | Accurate | process death 시 손실 가능 |
| Token expiration handling | 자동 reissue 있음 | 구현됨 | Accurate | refresh 실패 시 token clear 및 login redirect |
| Crash recovery | ForegroundService로 보호하지만 process kill 시 손실 위험 | 실제로 큰 risk | Needs clarification | `START_NOT_STICKY`, memory state/queue |

## 3. Missing or Underestimated Gaps

### Gap: Tracking crash recovery / process death recovery

Why it matters: 실제 러닝 중 OS가 process를 kill하거나 앱이 crash되면 tracking state와 pending GPS points가 사라질 수 있다. running app에서 가장 치명적인 사용자 경험 손상이다.

Evidence from codebase: `RunTrackingService`는 `START_NOT_STICKY`를 반환하고, `RunTrackingManager`는 elapsed time, last location, pending points를 memory state로만 가진다.

Suggested priority: P0

### Gap: Raw GPS point quality

Why it matters: 현재 distance 계산에는 teleport filter가 있지만 raw point는 그대로 upload된다. 이 raw point가 `running_points`, `running_records.path`, `course_points`, `courses.path`로 이어지면 지도 경로와 course 생성 품질이 오염된다.

Evidence from codebase: `DistanceCalculator.calculate()`는 10 m/s 초과 delta를 0으로 만들지만, `RunTrackingManager`는 그 location을 `RunPointRequest`로 queue에 추가한다.

Suggested priority: P0

### Gap: Nearby discovery가 실제 현재 위치를 쓰지 않음

Why it matters: RunWay의 핵심 가치인 “주변 코스 탐색”이 실사용 위치와 맞지 않는다. 실사용 테스트 전에 fixed test coordinate는 제거해야 한다.

Evidence from codebase: `DiscoverViewModel`에 `TEST_LATITUDE = 36.9706`, `TEST_LONGITUDE = 127.8718` 하드코딩.

Suggested priority: P0

### Gap: ForegroundService lifecycle edge cases

Why it matters: Android 13+ notification permission, background start restriction, battery optimization, service restart behavior는 tracking app에서 실제 device 테스트를 크게 좌우한다.

Evidence from codebase: service는 notification 유지와 manager start/stop 중심이며, persisted session 복구나 battery optimization 안내가 없다.

Suggested priority: P0

### Gap: Backend trusts client-provided distance/duration too much

Why it matters: course attempt finish request의 distance/duration을 그대로 leaderboard에 반영하면 조작이나 bug에 취약하다.

Evidence from codebase: `CourseAttemptService.finishAttempt()`는 request의 `distanceMeters`, `durationSeconds`로 `RunningRecord`와 `CourseAttempt`를 완료 처리한다.

Suggested priority: P1

### Gap: Data consistency between running_records, running_points, courses, course_attempts

Why it matters: finish 실패, point upload 실패, abandon 실패, app crash가 섞이면 `running_records.status`, `course_attempts.status`, `running_points` 존재 여부가 서로 어긋날 수 있다.

Evidence from codebase: Android는 finish 전에 `flushPendingPoints()`를 호출하지만 실패한 finish/abandon의 복구 queue나 reconciliation flow가 없다.

Suggested priority: P0

### Gap: Pagination UX

Why it matters: backend는 pagination을 제공하지만 Android는 fixed size 조회가 많아 데이터가 늘면 누락되거나 통계가 부정확해진다.

Evidence from codebase: `MyRunsViewModel`은 `size = 50`, `ProfileViewModel`은 `size = 100`, `HomeViewModel`은 최근 20개 기반 주간 통계를 계산한다.

Suggested priority: P1

## 4. Overrated or Premature Features

### Feature: Certification Image

Why it is premature: 공유 이미지는 acquisition에는 유리하지만, 현재는 tracking reliability와 route/course 품질이 먼저다. 기록 자체가 흔들리면 공유 이미지는 오히려 잘못된 결과를 확산한다.

What should be done first: crash recovery, persistent point queue, raw GPS filtering, privacy zone.

### Feature: Personal Records

Why it is premature: PR은 retention 기능으로 좋지만, GPS distance/duration 신뢰도가 충분하지 않으면 잘못된 PR이 발생한다.

What should be done first: tracking reliability, impossible speed filtering, splits calculation.

### Feature: Goals and achievements

Why it is premature: 아직 core run loop 안정화가 먼저다. 목표/업적은 정확한 기록과 복구 가능한 tracking 위에 올려야 한다.

What should be done first: reliable run recording and run detail analytics.

### Feature: Global Leaderboard tab API 연결

Why it is premature: 현재 backend API는 course별 leaderboard 중심이다. global leaderboard의 제품 정의가 명확하지 않다.

What should be done first: dummy tab 제거, featured course leaderboard로 명확화, 또는 course leaderboard entry point 강화.

### Feature: Share image generation with backend S3/image synthesis

Why it is premature: backend storage, image composition, lifecycle 관리가 필요해 complexity가 높다.

What should be done first: Android local share card prototype 또는 run result screen polish 정도로 제한.

## 5. Priority Review

| Feature | Current priority in document | Recommended priority | Reason |
|---|---:|---:|---|
| CourseAttempt route map overlay | P0 | P0 | 코스 도전의 핵심 UX. Map SDK 전체가 아니라 attempt screen overlay task로 좁히는 것이 좋음 |
| Global Leaderboard tab API 연결 | P0 | P1 | dummy 제거는 필요하지만 real user test blocker는 아님. 탭 제거로도 해결 가능 |
| GPS abnormal point filtering | P0 | P0 | 중요함. 단, 문서의 “없음”은 부정확. delta filter는 있고 raw point/path filter가 부족 |
| HomeScreen Nearby Courses 빈 섹션 | P0 | P1 | UX 문제지만 tracking reliability보다 낮음 |
| Privacy Zone | P0 | P0 | 공개 course의 집/회사 위치 노출 위험. real user test 전 필요 |
| Auto-Pause | P1 | P1 | 페이스 품질에 중요하지만 crash recovery보다 뒤 |
| 1km Splits | P1 | P1 | 기본 run analytics. retention보다 먼저 |
| Personal Records | P1 | P2 | 정확한 기록 기반이 먼저 필요 |
| My Courses screen | P1 | P2 | 유용하지만 blocker는 아님 |
| Certification Image | P1 | P3 | core reliability 이후에 진행 |
| Onboarding | P1 | P2 | 사용자 이해에는 중요하지만 기능 안정화 뒤 |
| Run completion CTA polish | P1 | P2 | 이미 create course button은 있음. 흐름 polish 단계 |
| GPS route verification | P2 | P1 | leaderboard integrity에 직접 영향. 너무 낮게 잡힘 |
| Course search | P2 | P2 | 탐색 UX 개선. nearby 위치 정확화 후 진행 |
| Course difficulty/tag | P2 | P3 | 초기 데이터가 적으면 가치 제한적 |
| Weekly goals | P2 | P3 | tracking 신뢰성 이후 retention layer |
| Course rating | P2 | P2 | course quality signal로 유용 |
| Course report | P3 | P1 | user-generated public course에는 abuse/reporting이 빠르게 필요 |
| Pace/speed chart | P3 | P2 | splits 이후 자연스러운 analytics 확장 |
| Offline queue persistence | P3 수준으로 취급 | P0 | tracking 중 네트워크/process failure 대응의 핵심 |
| Battery optimization handling | P3 수준으로 취급 | P0 | real device tracking 안정성에 직접 영향 |

## 6. Recommended Next Phase

### Phase name

Crash recovery / tracking reliability

### Why this should be next

RunWay의 핵심은 “사용자가 실제로 달린 기록이 정확히 저장되고, 그 기록으로 course와 leaderboard가 만들어진다”는 점이다. 현재 app은 running flow가 동작하지만, tracking state와 pending points가 memory 중심이라 process death, network failure, service lifecycle edge case에 취약하다.

Map SDK route preview는 이미 `RunDetailScreen`, `CourseDetailScreen`, `RunResultScreen`에 들어와 있다. 따라서 지금은 지도 확장보다 tracking reliability를 먼저 닫는 것이 더 실용적이다.

### Backend changes required

- finish 시 impossible pace/speed validation 추가
- zero/negative duration, unrealistic distance 방어
- duplicated GPS sequence 처리 정책 명확화
- active run 또는 active attempt 복구용 API 검토
- abandoned/in_progress stale record reconciliation 정책 수립

### Android changes required

- tracking session state persistence
- pending GPS points persistent queue 도입, likely Room
- process restart 후 in-progress run 복구 UX
- raw GPS point reject 기준을 distance 계산, upload, route path에 동일 적용
- notification permission denied behavior 정리
- battery optimization 안내 및 실기기 테스트 checklist
- free run과 course attempt 모두 동일한 reliability layer 사용

### Estimated complexity

Medium-High

### Risks

- `RunTrackingManager`가 singleton memory state 중심이라 구조 변경 영향이 크다.
- Room 도입 시 queue ordering, retry, deduplication 정책이 필요하다.
- crash recovery UX가 잘못되면 duplicate finish/abandon 또는 orphan record가 생길 수 있다.
- Android OS version별 ForegroundService behavior 차이가 있다.

### Acceptance criteria

- 앱 process kill 후 재실행 시 진행 중 run을 복구하거나 명확한 abandon/finish 선택지를 제공한다.
- network failure 중 쌓인 GPS points가 앱 재시작 후에도 보존된다.
- 비정상 GPS jump가 distance, uploaded points, route path, course path에 반영되지 않는다.
- 실기기에서 background 30분 tracking 후 finish가 성공한다.
- Android 13+ notification permission denied 상태에서도 사용자가 tracking 상태와 제약을 이해할 수 있다.
- course attempt 중 crash 후 `running_records`와 `course_attempts` 상태 불일치가 발생하지 않는다.

## 7. Suggested Roadmap After Next Phase

- B-15: Crash recovery / tracking reliability
- B-16: Privacy and course safety, including privacy zone and hide start/end points
- B-17: Splits and run analytics
- B-18: Course attempt route overlay and nearby discovery using real current location
- B-19: Leaderboard integrity, route verification, suspicious attempt rejection
- B-20: Share image generation and lightweight retention features

## 8. Final Recommendation

Map SDK는 지금 “중단”할 필요는 없다. 다만 다음 큰 phase로 계속 밀기보다는 `CourseAttemptTrackingScreen`의 route overlay 정도로 scope를 좁히는 것이 맞다.

다음 구현 phase는 **Crash recovery / tracking reliability**를 권장한다.

실제 사용자 테스트 전에는 다음을 먼저 처리해야 한다.

- tracking session 복구
- persistent GPS point queue
- raw GPS point filtering
- Android ForegroundService lifecycle 검증
- privacy zone
- real current location 기반 nearby discovery

`Share image generation`은 현재 priority가 높게 잡혀 있다. 실제로는 tracking reliability, privacy, splits, leaderboard integrity 이후로 미루는 것이 맞다.

