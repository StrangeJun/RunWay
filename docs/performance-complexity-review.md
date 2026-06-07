# RunWay Performance & Complexity Review

## 1. Review Context

- 검토 기준 브랜치: `main`
- 검토 시점 git log 요약:
  - `96c34cc feat(android): P2+P3 animation polish — confetti, count-ups, pager, nav transitions, staggered lists`
  - `d8b38f6 feat(android): P3 animations — result screen reveal, discover stagger, leaderboard stagger, weekly stats count-up`
  - `f0796bf feat(android): P2 animations — stats count-up, HorizontalPager, nav transitions, confetti`
  - `a8f9016 Merge branch 'feature/android-animation-p1'`
  - `11d12b8 feat(android): apply P1 UI animation polish`
  - `aacee21 fix(android): allow cleartext HTTP to production server and show empty state on API error`
  - `c28082d Merge branch 'feature/android-animation-p0'`
  - `a0c3b0d chore(ci): add workflow_dispatch trigger and fix JAR rename script`
  - `b9b5e63 chore: retrigger backend deployment after fixing SSH key secret`
  - `02da96e chore(backend): trigger initial production deployment`
- 검토 범위:
  - Android: `CourseAttemptTrackingScreen`, `RunningTrackingScreen`, `HomeScreen`, `CourseDetailScreen`, `CoursesLibraryScreen`, `ProfileScreen`, `DiscoverScreen`, `RunShareImageScreen`, tracking/Room/DataStore/GPS 관련 코드
  - Backend: `CourseService`, `CourseAttemptService`, `RunningService`, `UserService`, 관련 Repository와 Flyway index
  - 문서: `CLAUDE.md`, `README.md`, `docs/final-development-roadmap.md`, `docs/ux-improvement-roadmap.md`, `docs/competitive-differentiation-roadmap.md`, `docs/android-ui-animation-polish-plan.md`
- 확인 불가 문서: `docs/api-specification.md`는 현재 저장소에 없음.
- 검토 목적: 불필요한 API/DB 반복 호출, 명확한 계산 병목, 과도한 recomposition 가능성, 유지보수에 실제 비용을 만드는 복잡도만 선별한다.

## 2. Summary

- 전체적으로 가장 큰 성능 리스크는 Android 화면 복귀/탭 화면에서 여러 API를 반복 호출하는 구조와, backend 통계/프로필 API가 전체 러닝 기록을 메모리로 가져와 계산하는 구조다.
- 가장 먼저 고쳐야 할 부분은 `CourseDetailScreen`의 `ON_RESUME` 전체 새로고침이다. 화면 진입 init 로드와 resume refresh가 겹쳐 상세/포인트/리더보드/내 기록 API 4개가 불필요하게 반복될 수 있다.
- 수정하지 않아도 되는 부분은 단발성 count-up, Lazy list stagger animation, 공유 이미지 생성 화면의 local bitmap 생성이다. 현재 코드 기준으로는 상시 CPU/DB 병목보다 체감 polish 성격이 강하다.

## 3. Android Performance Issues

| Priority | Issue | File | Why it matters | Suggested fix | Risk |
|---|---|---|---|---|---|
| P0 | 코스 상세 화면이 `init` 로드 후 `ON_RESUME`에서도 전체 API 4개를 다시 호출함 | `android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailScreen.kt:92`, `android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailViewModel.kt:119`, `android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailViewModel.kt:368` | `load()`가 상세, 포인트, 리더보드 preview, 내 기록 API를 동시에 호출한다. 화면 최초 진입, 뒤로 돌아옴, 다이얼로그 닫힘 같은 lifecycle resume에서 반복 호출되어 서버와 지도 렌더링 부담이 커진다. | 최초 resume은 skip하고, 도전 완료/평점/공개/보관처럼 데이터 변경이 있는 이벤트 후에만 refresh한다. 필요하면 `refreshLight()`와 `reloadRoutePoints()`를 분리한다. | 낮음. refresh trigger만 줄이는 변경이라 화면 로직 영향이 작다. |
| P0 | 코스 라이브러리 화면 복귀마다 3개 목록 API를 모두 재호출함 | `android/app/src/main/java/com/runway/android/ui/course/library/CoursesLibraryScreen.kt:63`, `android/app/src/main/java/com/runway/android/ui/course/library/CoursesLibraryViewModel.kt:50`, `android/app/src/main/java/com/runway/android/ui/course/library/CoursesLibraryViewModel.kt:69` | init에서 만든 코스/즐겨찾기/참여 코스를 모두 로드하고, `ON_RESUME`에서도 세 API를 다시 호출한다. 탭 전환이 아니라 화면 복귀만 해도 불필요한 네트워크 호출이 발생한다. | `ON_RESUME` 자동 전체 로드를 제거하거나 dirty flag 기반으로 필요한 탭만 재조회한다. Pull-to-refresh는 유지한다. | 낮음. 데이터 최신성 정책만 정하면 변경 범위가 작다. |
| P1 | 코스 이탈 판정이 GPS 업데이트마다 전체 코스 segment를 선형 탐색함 | `android/app/src/main/java/com/runway/android/ui/attempt/CourseAttemptTrackingViewModel.kt:226`, `android/app/src/main/java/com/runway/android/ui/attempt/CourseAttemptTrackingViewModel.kt:482` | 현재 코스 포인트는 최대 200개로 제한되어 치명적이지 않지만, 1~3초 GPS tick마다 `zipWithNext().minOf`로 모든 segment 거리를 계산한다. 추적 중에는 배터리와 CPU에 직접 영향을 준다. | 다음 segment index를 캐시하거나 route progress 기반 sliding window만 검사한다. 최소한 최근 nearest segment 주변 N개만 계산한다. | 중간. 이탈 판정 정확도 회귀 테스트가 필요하다. |
| P1 | Pending GPS queue에 Room index가 없음 | `android/app/src/main/java/com/runway/android/core/tracking/local/PendingRunPointDao.kt:14`, `android/app/src/main/java/com/runway/android/core/tracking/local/PendingRunPointEntity.kt:6` | `WHERE runningRecordId ORDER BY sequence LIMIT`와 `DELETE WHERE runningRecordId`가 반복 실행된다. 장시간 오프라인 추적 후 pending row가 쌓이면 dequeue/delete가 느려질 수 있다. | `@Entity(indices = [Index(value = ["runningRecordId", "sequence"])])` 추가 후 Room migration 또는 destructive fallback 정책을 정한다. | 중간. Room schema version/migration 처리가 필요하다. |
| P1 | Tracking ViewModel이 많은 개별 `mutableStateOf`를 직접 노출해 tick마다 넓은 화면 recomposition을 유발할 수 있음 | `android/app/src/main/java/com/runway/android/ui/running/RunningTrackingViewModel.kt:61`, `android/app/src/main/java/com/runway/android/ui/attempt/CourseAttemptTrackingViewModel.kt:81` | GPS/timer/cadence 업데이트마다 여러 state가 순차 변경된다. 화면은 ViewModel 필드를 직접 읽고 있어 상단, 카드, 지도, 컨트롤까지 넓게 다시 계산될 수 있다. | immutable `UiState`로 묶고, composable에는 필요한 값만 파라미터로 내려 memoized subcomponent를 유지한다. 초 단위 텍스트와 지도 위치는 별도 state stream으로 분리한다. | 중간. 구조 변경이므로 한 화면씩 적용해야 한다. |
| P2 | Home 화면이 날씨와 주변 코스 위치를 별도 location request로 가져올 수 있음 | `android/app/src/main/java/com/runway/android/ui/home/HomeScreen.kt:59`, `android/app/src/main/java/com/runway/android/ui/home/HomeViewModel.kt:153`, `android/app/src/main/java/com/runway/android/ui/home/HomeViewModel.kt:168` | 화면 진입 시 `tryLoadNearbyCourses()`와 `tryLoadWeather()`가 각각 현재 위치를 요청한다. 네트워크보다 위치 취득이 느린 기기에서는 초기 홈 로드가 불필요하게 무거워진다. | 위치를 한 번 resolve한 뒤 nearby/weather에 공유한다. 현재 `currentLocation`을 source of truth로 사용한다. | 낮음. fallback만 유지하면 안전하다. |
| P2 | Lazy list item animation이 item별 `LaunchedEffect`를 생성함 | `android/app/src/main/java/com/runway/android/ui/home/HomeScreen.kt:253`, `android/app/src/main/java/com/runway/android/ui/discover/DiscoverScreen.kt:344` | 현재 리스트 크기는 작아 큰 문제는 아니지만, 코스 수가 늘면 visible item마다 coroutine/delay가 생긴다. 스크롤 중 새 item compose 시에도 animation state가 생긴다. | 첫 로드에만 실행하는 list-level animation flag를 두거나 animation을 상위 상태로 제한한다. | 낮음. polish 유지 여부만 결정하면 된다. |

## 4. Android Code Complexity Issues

| Priority | Issue | File | Why it matters | Suggested fix | Risk |
|---|---|---|---|---|---|
| P1 | `CourseDetailScreen`과 `CourseDetailViewModel`이 너무 많은 책임을 가짐 | `android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailScreen.kt:77`, `android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailViewModel.kt:43` | 상세 표시, 지도, 리더보드 preview, 내 기록, 즐겨찾기, 신고, 평가, 공개, 보관, 도전 시작 상태가 한 화면/ViewModel에 묶여 있다. 작은 상태 변경에도 큰 화면이 영향을 받기 쉽고, API refresh 정책도 복잡해진다. | Dialog state, favorite/rating/report action, route/leaderboard preview를 작은 state holder 또는 private composable로 분리한다. 첫 수정은 refresh trigger 분리부터 한다. | 중간. 단계적으로 나눠야 한다. |
| P1 | 자유 러닝과 코스 도전 tracking ViewModel에 배치 업로드/finish/진동/서비스 제어 로직이 중복됨 | `android/app/src/main/java/com/runway/android/ui/running/RunningTrackingViewModel.kt:202`, `android/app/src/main/java/com/runway/android/ui/attempt/CourseAttemptTrackingViewModel.kt:331` | GPS flush, pending queue drain, milestone vibration, service stop이 거의 같은 형태다. 한쪽의 실패 처리나 batch policy가 바뀌면 다른 쪽이 어긋날 수 있다. | `TrackingPointUploader` 또는 `TrackingSessionController`로 queue flush/drain/stop 공통 로직만 추출한다. | 중간. 추적 안정성에 직접 닿으므로 테스트 후 진행한다. |
| P2 | `CoursesLibraryScreen`에 3개 탭 카드 UI가 중복됨 | `android/app/src/main/java/com/runway/android/ui/course/library/CoursesLibraryScreen.kt:149`, `android/app/src/main/java/com/runway/android/ui/course/library/CoursesLibraryScreen.kt:221`, `android/app/src/main/java/com/runway/android/ui/course/library/CoursesLibraryScreen.kt:288` | 만든 코스/즐겨찾기/참여 코스 카드가 거리, 루프, 완주 count 표시를 반복한다. API 정책 변경보다 UI 일관성 유지 비용이 커질 수 있다. | 공통 `CourseListRow`와 각 탭 전용 trailing metadata만 분리한다. | 낮음. UI 표시만 비교하면 된다. |
| P2 | `MetadataChip`이 no-op click을 갖고 있음 | `android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailScreen.kt:928` | 실제 기능은 없지만 clickable chip처럼 접근성/사용자 기대를 만든다. 성능 문제는 아니나 production UI 복잡도를 늘린다. | clickable이 필요 없으면 `AssistChip`/비클릭 Surface로 바꾼다. | 낮음. |

## 5. Backend Performance Issues

| Priority | Issue | File | Why it matters | Suggested fix | Risk |
|---|---|---|---|---|---|
| P0 | GPS point 저장이 request batch 안에서 point마다 repository insert를 반복함 | `backend/src/main/java/com/runway/run/service/RunningService.java:72`, `backend/src/main/java/com/runway/run/repository/RunningPointRepository.java` | Android는 5초마다 최대 50개를 업로드한다. 서버는 같은 트랜잭션 안에서 point 수만큼 native insert를 호출하므로 장시간 러닝/복구 업로드 시 DB round-trip이 커진다. | JDBC batch insert 또는 native multi-row insert로 바꾼다. 최소한 repository custom batch insert를 둔다. | 중간. conflict ignore와 sequence 보존 테스트 필요. |
| P0 | `getRunningStats("all")`와 업적 API가 완료 러닝 전체를 메모리로 가져와 계산함 | `backend/src/main/java/com/runway/run/service/RunningService.java:245`, `backend/src/main/java/com/runway/user/service/UserService.java:80`, `backend/src/main/java/com/runway/run/repository/RunningRecordRepository.java:55` | Profile은 `getPersonalRecords()`와 `getRunningStats("all")`를 함께 호출한다. 사용자의 기록이 늘면 전체 row 로드와 Java stream 계산이 매번 발생한다. | 총계/기간 통계는 SQL aggregate로 계산하고, streak는 날짜만 조회하거나 별도 projection을 사용한다. achievements도 필요한 aggregate와 날짜 projection만 조회한다. | 중간. 응답 값 회귀 검증 필요. |
| P1 | Course detail 로드가 Android 한 화면에서 backend API 4개를 병렬 호출하게 되어 중복 쿼리가 많음 | `android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailViewModel.kt:373`, `backend/src/main/java/com/runway/course/service/CourseService.java:264`, `backend/src/main/java/com/runway/attempt/service/CourseAttemptService.java:225`, `backend/src/main/java/com/runway/attempt/service/CourseAttemptService.java:308` | 상세, 포인트, 리더보드, 내 기록이 각자 course visibility 조회를 수행한다. Android의 lifecycle refresh와 결합되면 코스 상세 진입당 쿼리 수가 커진다. | Android refresh를 먼저 줄이고, 이후 backend에서 detail response에 preview data 포함 여부를 제품/API 차원에서 검토한다. | 중간. API 변경은 영향이 크므로 Android trigger 개선이 우선이다. |
| P1 | 리더보드가 data/count/myRank 세 쿼리로 매번 ranking CTE를 재계산함 | `backend/src/main/java/com/runway/attempt/service/CourseAttemptService.java:237`, `backend/src/main/java/com/runway/attempt/service/CourseAttemptService.java:257`, `backend/src/main/java/com/runway/attempt/service/CourseAttemptService.java:265` | 코스별 완주자가 많아질수록 ranking 계산이 반복된다. 현재 partial index는 좋지만, 한 요청 안에서 data와 myRank가 같은 ranking을 따로 계산한다. | CTE를 한 번 확장해 page rows와 myRank를 함께 가져오거나, myRank는 요청자가 리더보드에 없을 때만 lazy load한다. | 중간. SQL 결과 매핑 테스트 필요. |
| P1 | course detail에서 평점 평균/count가 별도 쿼리임 | `backend/src/main/java/com/runway/course/service/CourseService.java:268`, `backend/src/main/java/com/runway/course/repository/CourseRatingRepository.java` | 상세 호출마다 course, creator, avg rating, rating count, favorite 존재 여부가 따로 조회된다. 큰 병목은 아니지만 CourseDetail이 자주 refresh되는 현재 Android 구조와 만나면 쿼리 수가 늘어난다. | Android refresh를 줄인 뒤, 필요하면 native projection으로 detail aggregate를 한 번에 조회한다. | 낮음-중간. |
| P2 | Nearby courses의 평점 평균/count가 row별 scalar subquery임 | `backend/src/main/java/com/runway/course/service/CourseService.java:169` | page size 기본 20에서는 감당 가능하지만, 코스 수와 요청 빈도가 늘면 `course_ratings`를 item별로 조회한다. | `LEFT JOIN course_ratings` + `GROUP BY` 또는 materialized rating aggregate를 검토한다. | 낮음. 현재 page size에서는 P2. |
| P2 | pending course_points 조회 SQL이 IN clause 문자열 조립임 | `backend/src/main/java/com/runway/course/service/CourseService.java:201` | 현재 값은 서버가 조회한 UUID라 injection 위험은 낮지만, 쿼리 계획 캐싱과 유지보수성이 좋지 않다. | array parameter 또는 repository query로 바인딩 처리한다. | 낮음. |

## 6. Backend Code Complexity Issues

| Priority | Issue | File | Why it matters | Suggested fix | Risk |
|---|---|---|---|---|---|
| P1 | `CourseService`가 탐색 SQL, 포인트 가공, 즐겨찾기, 참여 코스, 공개 정책을 모두 처리함 | `backend/src/main/java/com/runway/course/service/CourseService.java:50` | 성능 최적화가 필요한 SQL과 도메인 정책이 한 클래스에 섞여 있어 변경 영향 추적이 어렵다. Nearby 쿼리 개선, detail aggregate 개선 같은 작업이 같은 파일을 계속 키운다. | `CourseQueryService` 또는 repository custom query 클래스로 read-heavy native SQL만 분리한다. | 중간. 우선 문서화 후 기능별로 나눈다. |
| P1 | `RunningService` 통계/PR/기록/포인트 저장 책임이 섞여 있음 | `backend/src/main/java/com/runway/run/service/RunningService.java:63`, `backend/src/main/java/com/runway/run/service/RunningService.java:178`, `backend/src/main/java/com/runway/run/service/RunningService.java:216` | batch insert 개선과 aggregate SQL 개선이 같은 service에 들어가면 추적 로직과 통계 로직이 계속 충돌한다. | `RunningPointService`, `RunningStatsService`로 성능 민감 경로를 분리한다. | 중간. |
| P2 | streak 계산과 distance 계산이 중복됨 | `backend/src/main/java/com/runway/run/service/RunningService.java:287`, `backend/src/main/java/com/runway/user/service/UserService.java:178`, `backend/src/main/java/com/runway/run/service/RunningService.java:369`, `backend/src/main/java/com/runway/attempt/service/CourseAttemptService.java:356` | 현재는 작은 중복이지만 통계 기준이 바뀌면 서로 다른 결과를 만들 수 있다. | 공통 utility보다 먼저 SQL/projection 개선 후 남는 순수 계산만 추출한다. | 낮음. |
| P2 | `baseSql` 미사용 코드가 남아 있음 | `backend/src/main/java/com/runway/course/service/CourseService.java:155` | 성능 문제는 아니지만 native SQL이 긴 파일에서 혼란을 만든다. | 삭제한다. | 낮음. |

## 7. Quick Wins

수정 범위가 작고 효과가 큰 순서:

| Order | Task | Files | Expected benefit | Build impact |
|---|---|---|---|---|
| 1 | `CourseDetailScreen` 최초 `ON_RESUME` refresh skip 또는 dirty-event 기반 refresh로 변경 | `CourseDetailScreen.kt`, `CourseDetailViewModel.kt` | 코스 상세 진입/복귀 시 불필요한 API 4개 반복 호출 감소 | Android build only: `cd android && ./gradlew assembleDebug` |
| 2 | `CoursesLibraryScreen`의 `ON_RESUME` 전체 3 API reload 제거 또는 dirty flag 적용 | `CoursesLibraryScreen.kt`, `CoursesLibraryViewModel.kt` | 탭 화면 복귀 시 반복 네트워크 호출 감소 | Android build only |
| 3 | Pending queue Room index 추가 | `PendingRunPointEntity.kt`, `TrackingDatabase.kt` | 장시간 오프라인/복구 업로드 시 dequeue/delete 안정성 개선 | Android build + Room migration 검토 |
| 4 | Backend GPS point 저장 batch insert화 | `RunningService.java`, `RunningPointRepository.java` 또는 custom repository | 5초 주기 point 업로드와 finish drain DB round-trip 감소 | Backend build: `cd backend && ./gradlew build` |
| 5 | `getRunningStats("all")` aggregate SQL로 전환 | `RunningService.java`, `RunningRecordRepository.java` | Profile/Stats가 기록 수 증가에도 안정적으로 동작 | Backend build + 통계 값 검증 |

## 8. Deferred / Do Not Fix Now

- 코스 상세 API를 하나의 mega response로 합치는 작업은 지금 바로 하지 않는 편이 낫다. Android의 refresh trigger만 줄여도 호출 수가 크게 줄고, API 변경은 서버/클라이언트 동시 수정 범위가 커진다.
- `RunTrackingManager`와 두 tracking ViewModel의 공통화는 추적 안정성에 직접 닿는다. batch upload나 service stop 정책을 먼저 테스트로 고정한 뒤 진행한다.
- Nearby course rating aggregate 최적화는 page size가 작아 현재 P2다. 실제 코스/평점 데이터가 쌓인 뒤 slow query 기준으로 조정한다.
- Compose animation polish 자체를 일괄 제거할 필요는 없다. 현재 확인된 상시 리스크는 animation보다 반복 API 호출과 GPS tick 계산이다.
- 이미 적용된 Flyway migration 파일을 수정하지 않는다. index 추가가 필요하면 새 migration으로 추가한다.

## 9. Recommended Fix Order

1. Android `CourseDetailScreen`의 최초 `ON_RESUME` 중복 refresh 제거.
2. Android `CoursesLibraryScreen`의 `ON_RESUME` 세 API 전체 reload 제거.
3. Backend `RunningService.savePoints()` batch insert 개선.
4. Android PendingPointQueue Room index와 migration 추가.
5. Backend `RunningService.getRunningStats()`와 `UserService.getAchievements()`의 전체 row 로드 제거.
6. Course attempt 이탈 판정의 nearest segment 계산을 sliding window 방식으로 최적화.
7. `CourseService` read-heavy native SQL을 별도 query service로 분리.

## 10. First Fix Implementation Prompt

다음 프롬프트를 Codex/Claude Code에 바로 넣을 수 있다.

```text
RunWay Android에서 CourseDetailScreen의 불필요한 반복 API 호출을 줄여줘.

조건:
- 한 번에 이 문제만 수정해.
- 관련 파일만 수정해:
  - android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailScreen.kt
  - android/app/src/main/java/com/runway/android/ui/course/detail/CourseDetailViewModel.kt
- 현재 CourseDetailViewModel.init의 load()는 유지해.
- CourseDetailScreen의 Lifecycle.Event.ON_RESUME observer가 최초 화면 진입 직후 init load와 중복으로 refresh하지 않도록 해.
- 화면이 실제로 다시 돌아왔을 때만 refresh하거나, 더 안전하면 최초 resume skip flag를 사용해.
- rating/publish/archive 성공 후 load() 호출은 유지해.
- 동작 변경 후 cd android && ./gradlew assembleDebug 를 실행해.
- 코드는 커밋하지 말고, 빌드 결과와 변경 파일만 보고해.

빌드 성공 후 추천 커밋 메시지:
fix(android): avoid duplicate course detail refresh on first resume
```
