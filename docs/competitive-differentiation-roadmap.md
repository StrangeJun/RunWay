# RunWay Competitive Differentiation Roadmap

> 작성일: 2026-05-22
> 기준 상태: Phase B-36 완료 (`feature/course-deviation-warning` 브랜치)
> 목적: 경쟁 앱 분석을 기반으로 RunWay의 차별화 방향과 B-37 이후 로드맵 정의

---

## 1. Purpose

이 문서는 세 가지 질문에 답한다.

1. **경쟁 앱들은 무엇을 잘하고, 무엇에 실패하는가?**
2. **러너들이 실제로 무엇에 불편함을 느끼는가?**
3. **RunWay는 어디서 의미 있게 달라질 수 있는가?**

B-36까지 RunWay는 핵심 러닝 추적, 코스 생성, 코스 도전, 리더보드, 즐겨찾기, 공유 이미지, 프로필 편집, 코스 이탈 경고까지 구현했다. 이제 단순한 기능 완성이 아니라 **제품 방향**을 정의해야 할 시점이다. 이 문서는 그 근거를 제공한다.

---

## 2. Research Scope

**내부 분석 출처:**
- `docs/final-development-roadmap.md` — B-15~B-21 로드맵, 현재 구현 상태
- `docs/ux-improvement-roadmap.md` — B-27~B-36 UX 개선 로드맵
- `docs/product-gap-analysis.md`, `product-gap-analysis-review.md`
- 실제 코드베이스 직접 검사 (B-36 완료 시점)

**외부 리서치 출처:**
- SGX Studio: Nike Run Club Product Intelligence Report
- Tom's Guide: Runkeeper, Adidas Running, Garmin Connect 리뷰
- T3.com, Slashdot: Strava paywall 반발 기사 (2025)
- Android Police: Garmin Connect+ 구독 논란 (2025)
- Strava Engineering Blog: Segment leaderboard 무결성 리포트
- Adidas/CNN: 여성 러너 안전 조사 (2024)
- Running USA / Global Runner Survey 2024
- ASICS Runkeeper App Reviews 2025 (runnerlife.net, techjury.net)
- Fitness Tools Reviewed: MapMyRun 상세 리뷰

---

## 3. Competitor Analysis

| App | Core Strength | Weakness / Common Complaint | What RunWay Should Learn | What RunWay Should Avoid |
|---|---|---|---|---|
| **Nike Run Club** | 무료 플랫폼. Coach Bennett의 guided run이 감성적 동기부여를 제공. 친구 필터 리더보드. | 진지한 러너에게 필요한 심화 데이터 없음. 앱 크래시·sync 오류. 장기 retention 낮음. | 코치 페르소나 없이도 "동기부여 서사"를 UX에 내재화할 수 있다. 완전 무료 진입의 힘. | 브랜드 감성에 과도하게 의존하는 것. 데이터 없는 "응원" 메시지. |
| **Strava** | 9,500만 명 사용자. Segment 리더보드. Heat Map. 방대한 소셜 기능. | 2025년 Year in Sport paywall화로 커뮤니티 신뢰 훼손. 리더보드에 cheating(e-bike, 허위 기록) 만연. 구독 $80/년이 가치 대비 과도하다는 인식. | 사용자가 직접 만든 콘텐츠(경로, 기록)를 플랫폼이 잠그면 안 된다. Segment 경쟁 자체는 매우 효과적인 동기부여 메커니즘. | 기능을 무료로 제공하다가 나중에 paywall로 이전하는 전략. 글로벌 cheating에 취약한 개방형 리더보드 구조. |
| **Runkeeper** | 깔끔한 UI. 다양한 웨어러블 통합. 라이브 트래킹(안전). 초보자 훈련 플랜. | GPS 거리 과소 측정 지속 보고. 앱 크래시로 런 기록 소실. 워치 동기화 50% 실패율. 구독 $39.99/년. | 라이브 트래킹(가족/친구에게 위치 공유)은 안전 기능으로 높은 가치. 초보자 훈련 구조. | 워치 연동 우선순위. 지금 RunWay에는 핸드폰 GPS로 충분하다. |
| **Adidas Running** | 무료 플랜으로 기본 트래킹·커뮤니티 챌린지 접근. 음성 코치. 완료 후 상세 지도 리뷰. | 이전 런 데이터 paywall. 앱 동결·위치 오류. 2024년 훈련 플랜 버그(50주차 강제 이동). 계정 무단 삭제 사건. | 완료 후 상세 경로 리뷰 UX는 사용자가 자신의 런을 "분석"하게 만드는 좋은 접근법. | 품질 관리 없이 기능 확장하면 신뢰 붕괴 리스크. |
| **Garmin Connect** | 전문가급 데이터(VO2 Max, Training Status, Body Battery). 하드웨어 생태계 통합. | Connect+ 구독 도입(2025) → 고가 장비 구매자의 강한 반발. 2024년 UI 재설계 후 핵심 데이터 접근이 오히려 어려워짐. 로드맵 불투명으로 신뢰 하락. | 심화 데이터는 헌신적인 러너에게 강력한 lock-in 요소. 그러나 이것이 RunWay의 포지션은 아님. | 하드웨어 의존 생태계 구축. 구독 모델로 기존 사용자를 잠그는 전략. |
| **Samsung Health** | Galaxy 디바이스 기본 내장. 무료. 다양한 활동 지원. | Advanced Running Metrics 간헐적 누락. Strava 대비 "absurd"한 거리 측정 오차. 2025년 인기 기능 사전 고지 없이 삭제. Training Load 없음. | 진입 장벽 없는 내장 앱의 힘. 그러나 러닝 전문 앱이 아닌 범용 헬스앱의 한계. | 중간 단계 없이 즉시 범용 피트니스 플랫폼으로 확장하는 것. |
| **MapMyRun** | 방대한 사용자 제출 경로 데이터베이스. 거리 필터 탐색(무료). 장기 마일리지 추적. | 공격적인 프리미엄 업셀(광고 팝업 범람). GPS 오차 심각(24km → 20km 기록). Apple Watch 동기화 실패. 비도로 구간 지원 미흡. | 사용자 제출 경로의 가치는 매우 높다. 그러나 품질 검증 없는 경로 DB는 신뢰를 잃는다. | 경로 탐색 기능을 paywall 유도 도구로 사용. 검증 없는 모든 경로 공개. |

### 핵심 관찰: 2025년 경쟁 환경의 변화

> **페이월 피로(Paywall Fatigue)가 시장 최대 이슈다.**

2024~2025년 사이 모든 주요 경쟁 앱이 핵심 기능을 paywall 뒤로 이전했다:
- Strava: Year in Sport 연간 요약 → $80/년 구독 필요 (Reddit·SNS에서 폭발적 반발)
- Garmin: Connect+ 구독 도입 (2025년 3월, 장비 구매자 강한 반발)
- Runkeeper: premium 기능 확대 ($39.99/년)
- Adidas Running: 이전 런 데이터 paywall

이는 **무료로 핵심 기능을 유지하는 앱에 대한 강력한 수요**를 만들고 있다. 사용자들은 "내가 직접 만든 데이터를 내가 보려면 돈을 내야 한다"는 구조에 깊은 거부감을 갖고 있다.

---

## 4. Community Pain Points

| Pain Point | Evidence / Source Summary | Why It Matters | RunWay Opportunity |
|---|---|---|---|
| **페이월 피로** | Strava Year in Sport paywall(2025) Reddit 폭발적 반발. "내 데이터를 내가 플랫폼에 제공했는데 이를 보려면 돈을 내야 한다." 모든 주요 앱이 2024~2025에 핵심 기능 paywall 이전. | 사용자들이 플랫폼과의 신뢰를 잃고 있다. 전환 의향이 매우 높은 상태. | RunWay는 사용자가 기록한 데이터를 언제나 무료로 접근할 수 있도록 보장. 핵심 추적·리더보드·코스 기능을 무료 tier에 유지. |
| **리더보드 무결성** | Strava segment에서 시속 25km+ 불가능한 기록, e-bike 오용, 1,000시간/월 허위 입력 만연. Strava 엔지니어링 팀이 수백만 건 정리 발표(2025). | 리더보드를 "달성 불가능한 공간"으로 인식하면 경쟁 동기부여가 완전히 무너진다. | RunWay의 코스 완주 기반 리더보드는 GPS 기록이 없으면 등록 불가. 시작/종료점 반경 검증(B-21 계획)으로 구조적 방어. |
| **여성 러너 안전** | Adidas 연구: 92%의 여성 러너가 혼자 달릴 때 안전 우려. 46%가 달리는 도중 harassment 경험. MapMyRun/Strava 실시간 위치 공유가 오히려 스토킹에 활용된 사례 보고 (CNN, 2024). | 여성 러너는 전체 러너 시장의 50% 이상. 안전 기능이 앱 선택의 핵심 기준. | "검증된 코스" = 다른 러너들이 많이 달린 경로 → 자연스러운 안전 신호. Privacy Zone(B-17)으로 출발점 노출 방지. 코스 안전 태그(B-38 제안). |
| **GPS 정확도** | Runkeeper: GPS 거리 과소 측정 지속 보고. MapMyRun: 24km → 20km 기록. Samsung Health: Strava 대비 "absurd" 차이. Strava 트래킹 중 도심 GPS drift 공통 문제. | 러너들이 "어느 앱도 믿을 수 없다"는 인식이 퍼져 있다. 측정 정확도는 앱 신뢰의 기본. | B-15에서 GpsPointValidator(12 m/s, 200m, 1m 필터) 구현 완료. 운동 완료 후 경로 지도로 실제 기록 시각화. |
| **로컬 커뮤니티 부재** | 연구: "아는 사람과의 경쟁"이 "글로벌 낯선 사람과의 경쟁"보다 동기부여 효과 훨씬 높음. 어떤 앱도 지역 기반 실제 코스 커뮤니티를 깊게 제공하지 않음. | 나와 같은 동네에서 달리는 사람들과의 비교가 더 현실적이고 달성 가능한 목표를 만든다. | 코스 기반 리더보드 = 내 동네 코스를 함께 달린 사람들과 자연스러운 로컬 경쟁. |
| **초보자 장벽** | Strava segment leaderboard에 프로 선수 기록과 나란히 노출 → 초보자 열등감. GPS 통계 표시 복잡 → 초보자 해석 어려움. | 초보자가 "나는 여기에 맞지 않는다"고 느끼면 D7 이탈로 직결. | 코스별 리더보드는 "같은 코스를 달린 사람들" 중 비교. 초보자도 완주 자체가 성취. "이 코스의 평균 완주 시간" 기준점 제공. |
| **반복 달리기 동기부여 없음** | 경쟁 앱들은 새로운 거리/속도 PR만 추적. 같은 코스를 반복해서 달릴 때 "이번 달리기가 지난번보다 얼마나 좋아졌는지" 보여주는 앱 없음. | 많은 러너가 자신만의 단골 코스가 있다. 그 코스에서의 성장을 추적하는 것이 지속적 동기부여 핵심. | "코스 개인 기록(Course PR)" 기능 — 내가 달린 이 코스에서 가장 빠른 시간, 지난번 대비 개선도. |
| **경로 품질 신뢰 부재** | MapMyRun 경로 DB는 거대하지만 검증 없음. 품질 낮은 경로(잘못된 정보, 위험 구간)가 혼재. | 러너들이 처음 달리는 코스에 대해 "이 코스가 실제로 괜찮은가?"를 알 방법이 없다. | RunWay 코스는 실제 달린 GPS 경로에서 생성. 완주 횟수 + 평점(B-21 계획)으로 품질 신호 제공. |
| **구독 취소 복잡성** | Adidas Running: 앱 내 취소 불가, iTunes 통해야 함. 이는 신뢰 훼손의 대표적 사례. | 투명한 가격 정책과 쉬운 탈퇴가 새로운 신뢰 차별점. | 무료 core 기능 유지. 프리미엄이 있더라도 인앱에서 즉시 관리 가능하도록. |

---

## 5. RunWay Current Strengths

B-36 기준, RunWay가 실제로 잘 구현한 것들.

| 기능 | 구현 상태 | 차별화 가치 |
|---|---|---|
| **GPS 추적 신뢰성** | B-15 완료. GpsPointValidator + PendingPointQueue(Room) + TrackingSessionStore. 크래시 복구 다이얼로그. | 경쟁 앱의 GPS 오차 불만이 만연한 환경에서 신뢰 기반. |
| **실제 달린 경로 → 코스 생성** | B-7 완료. RunResultScreen에서 코스 생성 다이얼로그. `POST /api/courses/from-run/{runId}`. | 수동 지도 그리기가 아닌 실제 GPS 경로 기반. MapMyRun 경쟁자 대비 품질 우위 구조. |
| **코스 도전 + 리더보드** | B-10, B-32 완료. 코스별 완주 기록 + 정렬(최고 기록/최다 완주) + 내 순위 하이라이트. | Strava의 글로벌 segment와 달리 코스 단위 로컬 경쟁. cheating 구조적 어려움. |
| **코스 이탈 경고** | B-36 완료. 300m 이탈 시 햅틱 + 경고 UI. | 사용자가 코스를 실제로 따라가고 있는지 안내. 코스 완주의 의미 강화. |
| **즐겨찾기 코스** | B-30 완료. CourseDetailScreen 토글 + CoursesLibraryScreen 즐겨찾기 탭. | 단골 코스 관리. 반복 도전의 진입점. |
| **코스 공개 정책** | B-31 완료. 완주 조건 + 메타데이터(difficulty, slopeLevel 등). | 저품질 코스 자동 필터링. 커뮤니티 큐레이션 구조. |
| **공유 이미지** | B-21, B-34 완료. Canvas 기반 로컬 생성. 템플릿 + 배경 선택. | 사용자 획득 채널. SNS 공유 시 RunWay 브랜드 노출. |
| **Privacy Zone 계획** | B-17 설계됨. 코스 시작점 좌표 마스킹. | 실사용자 개인정보 보호. 여성 러너 신뢰 기반. |
| **홈 Hero 섹션** | B-28 완료. 지도 배경 + 큰 시작 버튼 + 스크롤 전환. | Run에 집중하는 명확한 앱 진입 경험. |
| **프로필 + 통계** | B-27, B-33 완료. 닉네임/소개 편집 + 이미지 picker. 개인 기록 섹션. | 사용자 자기 표현 + 러닝 성장 기록. |

---

## 6. RunWay Current Weaknesses

솔직한 현재 한계 분석.

| 약점 | 현재 상태 | 위험도 | 해결 방향 |
|---|---|---|---|
| **코스별 개인 기록(Course PR) 없음** | 코스 완주 시 리더보드만 보임. "내가 이 코스에서 얼마나 빨라졌는지" 알 수 없음. | 높음 — 반복 달리기 동기부여의 핵심이 빠져 있음 | B-37: Course PR 추적 |
| **완주 후 동기부여 피드백 약함** | 코스 완주 후 단순 "완주 완료" UI. 개선/악화 피드백 없음. | 높음 — D1 retention에 직접 영향 | B-37: "X초 개선!" 피드백 |
| **코스 품질 신호 불완전** | 평점은 B-21에서 계획됨. 현재 코스 목록에 완주 횟수·평점 없음. | 중간 — 탐색 UX 신뢰도 | B-38: 코스 품질 카드 강화 |
| **초보자 진입 경험 미완성** | B-35에서 권한 설명 추가됨. 그러나 "코스 달리기"가 무엇인지 처음 사용자가 이해하기 어려움. | 중간 — D3 이탈에 영향 | B-39: 초보자 첫 코스 추천 |
| **Push Notification 없음** | FCM 미연동. 새 도전자, PR 경신 등 실시간 알림 없음. | 중간 — D7 retention에 영향 | B-40: Push Notification |
| **Global Leaderboard 탭 미완성** | B-17 계획에서 재정의 필요. 현재 placeholder 상태인지 확인 필요. | 낮음-중간 — BottomNav 4탭 중 하나가 비어있으면 앱 완성도 인식 저해 | B-37 or B-38에서 처리 |
| **코스 안전 정보 없음** | 코스 메타데이터에 difficulty·slope 있으나 안전 관련 태그 없음. | 중간 — 여성 러너 신뢰도 | B-38: 안전 태그 |
| **Auto-Pause 미완성** | B-18 계획. 현재 수동 pause만 가능. | 중간 — 페이스 데이터 정확도 영향 | B-18에서 구현 예정 |
| **오프라인 동작 불완전** | PendingPointQueue는 있으나 완전 오프라인(서버 없이) 런 완료 후 처리 흐름 미검증. | 낮음 — 네트워크 불안정 환경에서 런 손실 가능성 | B-18 또는 별도 Phase |
| **소셜 그래프 없음** | 팔로우/팔로워, 친구 목록 없음. | 낮음 — 지금은 의도적으로 제외. 코어 루프 안정화 후 검토. | Phase 2+ |

---

## 7. Differentiation Strategy

### 방향 A: "로컬 코스 챌린지 플랫폼"

> **"내 동네 코스를 함께 달리고, 공정하게 경쟁하는 앱"**

- **포지셔닝:** Strava의 글로벌 segment가 아닌, 지역 커뮤니티가 만들고 검증한 코스 중심 경쟁
- **타겟 사용자:** 단골 러닝 코스가 있는 중급 러너, 커뮤니티 경쟁을 즐기는 사람
- **코어 루프:** 코스 발견 → 도전 → 리더보드 → 기록 경신 → 코스 재도전
- **핵심 기능:** Course PR, 코스별 완주 뱃지, 코스 이탈 경고, 품질 검증 리더보드
- **리스크:** 코스 생태계가 성장하기 전까지 컨텐츠 부족. 초기 사용자 획득 어려움.

### 방향 B: "안전한 경로 발견 앱"

> **"검증된 경로만 추천하는, 러너를 위한 안전한 러닝 가이드"**

- **포지셔닝:** 여성 러너와 초보자를 위한 안전·품질 중심 경로 탐색
- **타겟 사용자:** 혼자 달리는 여성 러너, 새 동네에서 달릴 곳을 찾는 러너
- **코어 루프:** 안전한 코스 탐색 → 첫 도전 → 커뮤니티 신뢰 형성 → 코스 평점
- **핵심 기능:** 코스 완주 횟수 기반 안전 신호, 시간대별 추천, 인기 경로 우선 노출
- **리스크:** 안전 데이터 축적에 시간 필요. "안전" 보장을 오해할 법적 리스크.

### 방향 C: "반복 가능한 코스 성장 앱"

> **"매주 같은 코스를 달리며 내 기록이 얼마나 성장했는지 보여주는 앱"**

- **포지셔닝:** 일반 GPS 트래커가 아닌, 특정 코스에서의 개인 성장을 추적하는 플랫폼
- **타겟 사용자:** 집 근처 단골 코스를 반복해서 달리는 모든 러너
- **코어 루프:** 코스 즐겨찾기 → 반복 도전 → Course PR 경신 → 장기 성장 추적
- **핵심 기능:** Course PR 추적, "지난번 대비 +3초" 즉각 피드백, 코스 달리기 streak, 월간 코스 통계
- **리스크:** 코스 생태계 없이 개인 성장 추적만으로는 차별화 부족. 코스 생태계 성장에 의존.

### 권장 방향

> **방향 A + C의 조합을 권장한다.**

- 방향 A는 RunWay의 현재 구현(코스 생성→도전→리더보드)과 완벽하게 정렬된다.
- 방향 C는 개인 동기부여 레이어를 추가해 리텐션을 강화한다.
- 방향 B는 방향 A의 결과물(많은 완주자 = 검증된 코스)로 자연스럽게 달성된다.

---

## 8. Recommended Product Direction

RunWay는 다음이 되어야 한다:

> **"지역 러너들이 직접 만들고, 반복해서 도전하며, 공정하게 경쟁하는 코스 기반 러닝 플랫폼"**

### 왜 이 방향인가

**1. 기존 구현과 정렬된다.**
RunWay는 이미 "달린 경로 → 코스 생성 → 도전 → 리더보드" 전체 플로우를 갖추고 있다. 이것은 Strava, Nike Run Club, MapMyRun 어느 앱도 완전하게 제공하지 않는 플로우다.

**2. 경쟁 앱의 실패에서 배운다.**
- Strava의 segment는 e-bike·허위 기록으로 신뢰가 무너졌다. RunWay의 코스 완주는 GPS 경로 기록이 전제된다.
- MapMyRun의 경로 DB는 방대하지만 품질 검증이 없다. RunWay는 "실제 달린 사람이 만든 코스 + 완주 횟수 조건"으로 품질을 구조화한다.
- 모든 경쟁 앱이 paywall을 확장하고 있다. RunWay는 핵심 코스 기능을 무료로 유지할 기회가 있다.

**3. 아직 아무도 잘 하지 않는다.**
- 로컬 코스 도전: 없음
- 코스별 개인 기록 추적: 없음
- 실제 달린 GPS 기반 코스 + 공정한 로컬 리더보드: 없음
- 반복 달리기에서의 개인 성장 추적: 없음

**4. 소셜 그래프 없이도 성립한다.**
팔로우/팔로워 없이 "코스 리더보드"로 자연스러운 커뮤니티 경쟁이 가능하다. 초기에 소셜 그래프를 구축하지 않아도 핵심 경쟁 루프가 작동한다.

---

## 9. New Feature Ideas

### A. Route Discovery (경로 발견)

| 기능 | 사용자 가치 | 복잡도 | Backend 영향 | Android 영향 | 우선순위 |
|---|---|---|---|---|---|
| **지도 기반 코스 탐색** | 거리 목록이 아닌 지도 위에서 코스 핀으로 탐색. 위치 직관적 파악. | 중간 | 없음 | 중간 (Google Maps 마커 클러스터링) | P2 |
| **거리/난이도 필터** | DiscoverScreen에 거리 범위 + 난이도 필터. 초보자가 5km 이하만 볼 수 있음. | 낮음 | 낮음 (API 파라미터 추가) | 낮음 (Filter chip UI) | P1 |
| **"인기 코스" 섹션** | 이번 주 완주자 수 기준 Top 5. 사회적 검증 신호. | 낮음 | 낮음 (집계 쿼리 추가) | 낮음 | P1 |
| **"나와 비슷한 페이스의 코스" 추천** | 내 평균 페이스 기준으로 완주 가능한 코스 추천. 초보자 적합. | 높음 | 높음 (추천 로직) | 중간 | P3 |

### B. Course Quality and Safety (코스 품질·안전)

| 기능 | 사용자 가치 | 복잡도 | Backend 영향 | Android 영향 | 우선순위 |
|---|---|---|---|---|---|
| **코스 완주 후 평점 팝업** | 완주 직후 1~5점 평점 + 한 줄 코멘트. 코스 품질 누적 데이터. | 낮음 | 낮음 (B-21 이미 설계됨) | 낮음 (간단한 다이얼로그) | **P0** |
| **코스 카드에 평점·완주 수 표시** | DiscoverScreen에서 코스 선택 시 "★4.2 · 38회 완주" 표시. 신뢰 신호. | 낮음 | 낮음 | 낮음 (UI 요소 추가) | **P0** |
| **안전 태그 (Safety Tags)** | 코스 생성 시 "야간 조명 있음", "인적이 많은 경로", "트레일/도로" 태그. | 낮음 | 낮음 (컬럼 추가) | 낮음 (태그 선택 UI) | P1 |
| **코스 신고 기능** | 위험하거나 부적절한 코스 신고. 커뮤니티 안전 기본 장치. | 낮음 | 낮음 (B-19 이미 설계됨) | 낮음 | P1 |

### C. Course Challenge / Leaderboard (도전·경쟁)

| 기능 | 사용자 가치 | 복잡도 | Backend 영향 | Android 영향 | 우선순위 |
|---|---|---|---|---|---|
| **Course PR (코스 개인 기록)** | 코스별 내 최고 기록. "이 코스 PR: 24분 32초". 반복 도전의 핵심 동기. | 낮음 | 낮음 (집계 쿼리) | 낮음 (UI 표시) | **P0** |
| **완주 후 PR 피드백** | "이번 완주: 24분 15초 ← 지난번보다 17초 빠름! 🔥" | 낮음 | 낮음 | 낮음 | **P0** |
| **코스 완주 뱃지** | 같은 코스를 3회, 10회, 30회 완주 시 뱃지. "단골 러너" 인증. | 낮음 | 낮음 | 낮음 | P1 |
| **도전 streak (연속 도전)** | 매주 코스를 완주하면 streak 카운터. 4주 연속 → 특별 뱃지. | 낮음 | 낮음 | 낮음 | P2 |
| **GPS 경로 간단 검증** | 완주 GPS 경로의 시작/종료점이 코스 반경 500m 이내인지 확인. 허위 완주 방지. | 중간 | 중간 (B-21 설계됨) | 없음 | P1 |
| **내 코스에서 새 도전자 알림** | 내가 만든 코스에 다른 사람이 도전했을 때 알림. | 낮음 | 중간 (FCM) | 중간 | P2 |

### D. Beginner Motivation (초보자 동기부여)

| 기능 | 사용자 가치 | 복잡도 | Backend 영향 | Android 영향 | 우선순위 |
|---|---|---|---|---|---|
| **"첫 코스 추천"** | 신규 사용자 또는 완주 기록 없는 사용자에게 "쉬운 코스" 3개 추천. | 낮음 | 낮음 | 낮음 | P1 |
| **"이 코스 평균 완주 시간"** | 리더보드 없어도 "이 코스 평균 25분". 자신을 상대화할 기준점. | 낮음 | 낮음 (집계) | 낮음 | P1 |
| **코스 완주 축하 애니메이션** | 처음 완주 시 특별 celebration 애니메이션. 성취감 강화. | 낮음 | 없음 | 낮음 | P2 |
| **주간 코스 목표** | 매주 1개 코스 완주 목표. 진행률 표시. | 낮음 | 낮음 | 낮음 | P2 |

### E. Social Sharing (소셜 공유)

| 기능 | 사용자 가치 | 복잡도 | Backend 영향 | Android 영향 | 우선순위 |
|---|---|---|---|---|---|
| **코스 완주 인증 카드** | "XX 코스 완주! 시간: 24분 15초" 카드 이미지. 기존 공유 이미지와 별도 트리거. | 낮음 | 없음 | 낮음 | P1 |
| **코스 링크 공유** | 코스 Detail 화면에서 딥링크 공유. 다른 사람에게 "이 코스 달려봐" 초대. | 낮음 | 낮음 (딥링크 처리) | 낮음 | P2 |
| **코스 QR 코드** | 코스 시작점에서 QR 코드로 공유. 오프라인 런 그룹에서 활용. | 낮음 | 낮음 | 낮음 | P3 |

### F. Personal Progress (개인 성장)

| 기능 | 사용자 가치 | 복잡도 | Backend 영향 | Android 영향 | 우선순위 |
|---|---|---|---|---|---|
| **월간/연간 러닝 요약** | 이번 달 총 거리, 완주 코스 수, 신기록 개수. 장기 성장 추적. | 낮음 | 중간 (집계 쿼리) | 중간 | P1 |
| **개인 최고 기록 (Global PR)** | 5km, 10km, 하프마라톤 최고 기록. B-20에서 이미 설계됨. | 낮음 | 중간 (B-20 설계됨) | 낮음 | P1 |
| **러닝 히트맵** | 내가 달린 경로의 지도 위 히트맵. 단골 코스 시각화. | 높음 | 낮음 (running_points 집계) | 높음 (지도 렌더링) | P3 |
| **배지/성취 시스템** | "총 100km 달성", "10개 코스 완주" 등 마일스톤 뱃지. | 낮음 | 낮음 | 중간 | P2 |
| **코스 완주 히스토리** | 내가 같은 코스를 몇 번 달렸는지, 각 기록 추이. | 낮음 | 낮음 | 중간 | P1 |

### G. Reliability / Trust (신뢰·안정성)

| 기능 | 사용자 가치 | 복잡도 | Backend 영향 | Android 영향 | 우선순위 |
|---|---|---|---|---|---|
| **Auto-Pause** | 신호등·음수대 정지 시 타이머/거리 자동 일시정지. 페이스 정확도 향상. | 낮음 | 없음 | 중간 (B-18 설계됨) | **P0** |
| **Push Notification** | PR 경신, 새 도전자, 코스 댓글 알림. FCM 연동. D7 retention에 직접 영향. | 중간 | 높음 | 중간 | P1 |
| **이상 기록 자동 감지** | 불가능한 페이스(5km를 1분) 자동 rejected 처리. 리더보드 신뢰 보호. | 낮음 | 중간 (B-21 설계됨) | 없음 | P1 |
| **백그라운드 추적 안정성** | 배터리 최적화 제외 안내 + 앱 포커스 lost 시 ForegroundService 유지 검증. | 낮음 | 없음 | 낮음 | P1 |

---

## 10. Prioritized Roadmap (B-37 이후)

> B-37 이후는 B-36까지 구축된 탄탄한 기반 위에 **반복 달리기 동기부여 + 코스 품질 신호 + 리텐션 레이어**를 더하는 방향이다.

| Phase | Feature | Why Now | Backend Impact | Android Impact | Complexity | Acceptance Criteria |
|---|---|---|---|---|---|---|
| **B-37** | **Course PR + 완주 후 동기부여 피드백** | 코어 루프의 빠진 피드백 레이어. "이번 달리기가 좋아졌는지" 알 수 없으면 재방문 이유 없음. D1 retention에 직접 영향. | 낮음 (`course_attempts`에서 집계) | 낮음 (완주 후 결과 화면에 PR 표시) | Low | 코스 완주 후 "Course PR" 또는 "지난번보다 +X초" 표시. CourseDetailScreen에 "내 기록" 섹션. |
| **B-38** | **코스 평점 + 코스 카드 품질 신호** | 탐색 UX에서 어느 코스가 좋은지 알 수 없음. 평점이 없으면 커뮤니티 큐레이션 기능 없음. | 낮음 (ratings 테이블, B-21 설계됨) | 낮음 (완주 후 팝업 + 카드 UI 업데이트) | Low | 완주 후 1~5점 팝업. DiscoverScreen 코스 카드에 평점·완주 수 표시. |
| **B-39** | **Auto-Pause + 1km Splits** | 기본 러닝 분석 기능 부재. 신호등 앞에서 멈출 때마다 페이스가 틀어짐. B-18에서 이미 설계됨. | 없음 | 중간 | Medium | 속도 < 0.3 m/s 지속 시 자동 pause. RunDetailScreen에 1km별 페이스 테이블. |
| **B-40** | **Push Notification (FCM)** | D7 retention의 핵심. "새로운 사람이 내 코스에 도전했어요" 알림이 없으면 앱을 다시 열 이유 없음. | 높음 (FCM 연동, 알림 트리거) | 중간 (FCM client, 알림 채널) | Medium | PR 경신, 새 도전자, 코스 새 완주자 알림 수신. 알림 설정 화면. |
| **B-41** | **월간 통계 + 코스 완주 히스토리** | 장기 리텐션 레이어. "이번 달 3개 코스 완주" 요약으로 성취감 강화. | 중간 (집계 API) | 중간 | Medium | ProfileScreen에 월간 통계 섹션. "내 코스 기록" 탭에서 코스별 완주 횟수·최고 기록 표시. |
| **B-42** | **코스 완주 뱃지 + 성취 시스템** | 같은 코스 3회/10회 완주 뱃지로 gamification. Push Notification과 연동하면 시너지. | 낮음 | 중간 | Low-Medium | 완주 3/10/30회 뱃지. ProfileScreen에 뱃지 갤러리. |
| **B-43** | **거리/난이도 필터 + 인기 코스 섹션** | DiscoverScreen에 필터 없으면 코스가 늘어날수록 탐색 어려움. | 낮음 | 낮음 | Low | 거리·난이도 filter chip. HomeScreen에 "이번 주 인기" 섹션. |
| **B-44** | **Privacy Zone (코스 출발점 마스킹)** | 실제 집 앞 출발 코스 좌표 공개 → 프라이버시 위험. B-17에서 설계됨. | 중간 | 낮음 | Medium | 코스 출발점 150m 반경 내 랜덤 offset. 설정에서 활성화/비활성화. |
| **B-45** | **GPS 경로 완주 검증 + 이상 기록 감지** | 코스를 실제 달리지 않고 완주 처리하는 경우 방어. 리더보드 신뢰의 핵심. B-21 설계됨. | 높음 | 없음 | Medium-High | 완주 GPS 경로 시작/종료점 반경 500m 확인. 불가능한 페이스 자동 rejected. |

---

## 11. Features to Avoid for Now

아래 기능들은 현 단계에서 개발하지 않아야 하는 이유가 명확하다.

| 기능 | 이유 |
|---|---|
| **전체 소셜 피드 (팔로우/팔로워)** | 팔로우 테이블, 피드 집계, 알림 인프라가 별도 복잡도. 코어 루프(코스 도전·리더보드)가 먼저 안정화되어야 함. 비어있는 소셜 피드는 앱을 오히려 작아 보이게 만든다. |
| **댓글 / 좋아요** | 모더레이션 필요. 악성 댓글 처리 인프라 없음. 지금은 평점·완주 수로 충분한 커뮤니티 신호. |
| **AI 코칭 플랜** | 도메인 지식 + 전문 러닝 코치 설계 없이는 경쟁 앱 대비 열위. RunWay의 차별화는 코스 도전이지 훈련 계획이 아님. LLM API 비용도 고려해야 함. |
| **구독/인앱 결제** | 사용자 기반 + 리텐션 메트릭이 안정화된 후 설계해야 함. 지금 구독 도입은 경쟁 앱 탈주자를 흡수할 기회를 없앰. |
| **웨어러블 연동 (Wear OS / Galaxy Watch)** | 플랫폼별 별도 SDK. MVP에서는 스마트폰 GPS로 충분. |
| **지도 위 코스 수동 그리기 (Route Editor)** | RunWay의 핵심 가치는 "실제 달린 경로 = 코스". 수동 편집은 이 차별화와 반대 방향. 검증도 불가능해짐. |
| **실시간 그룹 런 / 위치 공유** | WebSocket 인프라 + 동시 접속 관리 복잡도. 지금은 비동기 리더보드 경쟁으로 충분. |
| **대규모 추천 엔진** | 사용자 행동 데이터가 충분히 쌓인 후에 의미가 있다. 지금은 PostGIS `ST_DWithin` 거리순 + 평점 정렬로 충분. |
| **전국 랭킹 / 글로벌 리더보드** | Strava의 실패 사례를 반면교사로. 글로벌 경쟁은 초보자를 배제하고 cheating에 취약하다. 코스별 로컬 리더보드가 RunWay의 답이다. |
| **Google/Apple Health 연동** | 플랫폼별 헬스 API 복잡도 높음. 핵심 추적 기능 완성 후 선택적 추가. |

---

## 12. Recommended Next Phase

### B-37: Course PR & Completion Feedback

**Phase 이름:** Course Personal Records & Completion Motivation Feedback

**브랜치:** `feature/android-course-pr`

**목표:**
코스 완주 후 "이번이 나의 최고 기록인가?"와 "지난번보다 빨라졌는가?"를 즉각 보여주는 기능. 반복 달리기의 가장 강력한 동기부여 루프를 완성한다.

**왜 지금인가:**
- B-36까지 코어 기능 완성. 이제 필요한 것은 "재방문 이유".
- 코스를 완주했는데 "좋아졌는지" 모르면 같은 코스를 다시 달릴 이유가 없다.
- Course PR은 Backend 변경이 매우 적고(집계 쿼리만), Android 변경도 작다. 낮은 비용으로 높은 동기부여 효과.
- 경쟁 앱 중 코스별 개인 기록을 추적하는 앱은 없다. **명확한 차별점.**

**Scope:**

1. **Backend: Course PR 집계 API**
   - `GET /api/courses/{courseId}/attempts/me/best` — 내 해당 코스 최고 기록 조회
   - 또는 기존 `GET /api/courses/{courseId}/attempts/me` 응답에 `myBestSeconds: Int?` 추가
   - `course_attempts` 테이블에서 `user_id = currentUser AND course_id = courseId AND status = 'completed'` 집계

2. **Backend: 완주 후 PR 판단**
   - `POST /api/course-attempts/{attemptId}/finish` 응답에 `isPR: Boolean`, `previousBestSeconds: Int?` 추가
   - 완주 직후 즉각 PR 여부 반환

3. **Android: CourseAttemptResultScreen PR 피드백**
   - 완주 후 결과 화면에 "🏆 코스 신기록!" 또는 "⬆️ 지난번보다 17초 빠름!" 표시
   - 이전 기록 vs 현재 기록 비교 UI

4. **Android: CourseDetailScreen 내 기록 섹션**
   - 코스 상세에 "내 기록" 섹션: 최고 기록, 완주 횟수, 최근 완주일
   - 리더보드 미리보기와 함께 표시

**Out of Scope:**
- 코스 평점 (B-38)
- Auto-Pause (B-39)
- Push Notification (B-40)
- 뱃지/성취 시스템 (B-42)

**Backend 영향:** 낮음 (집계 쿼리 + finishAttempt 응답 필드 추가)

**Android 영향:** 낮음-중간 (완주 결과 화면 수정 + CourseDetailScreen 섹션 추가)

**Complexity:** Low

**Acceptance Criteria:**
- [ ] 코스 완주 직후 "코스 신기록!" 또는 "지난번보다 X초 빠름/느림" 메시지 표시
- [ ] 처음 완주 시 "첫 완주! 기록: 24분 32초" 표시
- [ ] `CourseDetailScreen`에서 내 최고 기록, 완주 횟수 확인 가능
- [ ] `GET /api/courses/{courseId}/attempts/me` 또는 새 API로 내 기록 조회 성공
- [ ] 기존 코스 완주 플로우에 영향 없음
- [ ] `./gradlew assembleDebug` 빌드 성공

**커밋 메시지:**
```
feat: [B-37] add course personal records and completion feedback
```

---

## 13. Implementation Prompt for B-37

```
Implement Phase B-37: Course Personal Records and Completion Motivation Feedback.

Current branch: feature/course-deviation-warning
New branch to create: feature/android-course-pr
Branch from: feature/course-deviation-warning (B-36 completed)

Before implementing:
1. Confirm current branch is feature/course-deviation-warning.
2. Create and switch to feature/android-course-pr.
3. Read docs/competitive-differentiation-roadmap.md Section 12 for full scope.
4. Read docs/api-specification.md for course attempt API specs.

## Context

B-36 (course deviation warning) is complete. B-37 adds the missing "did I improve?" feedback
loop that turns one-time course completions into repeat attempts.

Currently, when a user completes a course:
- They see the basic completion stats (time, distance, pace)
- They see the full leaderboard
- But they get no feedback on whether this is their personal best, or how it compares
  to their previous attempt on this same course.

This is the core retention gap. Running apps like Strava show PR notifications, but only
for generic distances (5km, 10km), not for specific courses. RunWay can be the first app
that tracks personal bests per community-created course.

## Scope

### Task 1: Backend — Add PR data to finishAttempt response

File: backend/src/main/java/.../courseAttempt/service/CourseAttemptService.java
  - In finishAttempt(), after saving the completion:
    1. Query course_attempts for previous best by this user on this course:
       SELECT MIN(duration_seconds) FROM course_attempts
       WHERE user_id = :userId AND course_id = :courseId
       AND status = 'completed' AND id != :currentAttemptId
    2. Determine isPR: currentAttempt.durationSeconds < previousBest (or no previous)
    3. Add to FinishAttemptResponse:
       - isPR: Boolean (true if this is the user's best time on this course)
       - previousBestSeconds: Int? (null if this is the first completion)
       - improvementSeconds: Int? (positive = faster, negative = slower, null if first)

File: backend/src/main/java/.../courseAttempt/dto/FinishAttemptResponse.java (or similar)
  - Add fields: isPR, previousBestSeconds, improvementSeconds

### Task 2: Backend — My best record on a course

Add or update endpoint:
  GET /api/courses/{courseId}/attempts/me
  - If this endpoint already exists, ensure response includes:
    - completionCount: Int
    - bestTimeSeconds: Int? (my personal best on this course)
    - lastAttemptAt: String?
  - If it doesn't exist, create it.

### Task 3: Android — CourseAttemptResultScreen or Finish flow UI

After a course attempt finishes (already navigates to some result or back), show:

If isPR == true and previousBestSeconds == null (first completion):
  Show: "🏅 첫 완주! 기록: {formattedTime}"

If isPR == true and previousBestSeconds != null:
  Show: "🏆 코스 신기록! {formattedTime} (이전: {formattedPrevious})"

If isPR == false:
  Show: if improvementSeconds > 0:
          "⬆️ 지난번보다 {|improvementSeconds|}초 빠름!"
        else:
          "지난번보다 {|improvementSeconds|}초 느림. 다음엔 더 잘 달릴 수 있어요."

Find where course attempt completion is handled in:
- ui/attempt/CourseAttemptTrackingScreen.kt
- ui/attempt/CourseAttemptTrackingViewModel.kt
Check if there's a separate result screen or if it navigates back to CourseDetailScreen.
Add the PR feedback UI wherever the completion result is shown.

### Task 4: Android — CourseDetailScreen "내 기록" section

File: ui/course/detail/CourseDetailScreen.kt
  After the existing leaderboard preview section, add a "내 기록" section:
  - Title: "내 기록"
  - Show: myBestTime (formatted as mm:ss), completionCount, lastAttemptAt
  - If no completions: "아직 이 코스를 완주한 기록이 없어요. 도전해보세요!"

File: ui/course/detail/CourseDetailViewModel.kt
  - Add myRecord: MyCourseRecord? state (mutableStateOf, initially null)
  - Add isLoadingMyRecord: Boolean
  - In init or loadDetail(): load my record via GET /api/courses/{courseId}/attempts/me
  - CourseAttemptRepository already exists — add getMyAttemptRecord(courseId) or reuse existing

data class MyCourseRecord(
    val completionCount: Int,
    val bestTimeSeconds: Int?,
    val lastAttemptAt: String?
)

## Architecture Notes

- FinishAttemptResponse needs new fields. Confirm the Android data model
  (data/attempt/model/) also gets updated to match.
- The "first completion" case (no previous record) must be handled gracefully.
- PR calculation should be done server-side, not client-side, to avoid data inconsistency.
- Do NOT change the existing course attempt flow logic — only add response fields and
  new UI elements.

## Build Command

./gradlew assembleDebug

## Completion Criteria

1. After completing a course attempt, user sees PR/improvement feedback on screen.
2. "첫 완주" message shown when user completes a course for the first time.
3. "코스 신기록" message shown when user beats their own previous best time.
4. Improvement/regression time shown in seconds when not a PR.
5. CourseDetailScreen shows user's best time and completion count for that course.
6. If user has no completions on that course, appropriate empty state shown.
7. Existing course attempt start/abandon/finish flows unchanged.
8. ./gradlew assembleDebug succeeds.

## Commit Rule

- Run ./gradlew assembleDebug before committing.
- Show changed file list.
- Do NOT run git push.
- Commit message: feat: [B-37] add course personal records and completion feedback
```

---

## 14. Decisions Needed from User

구현 전에 결정이 필요한 항목들.

### 결정 1: Course PR 계산 기준

- **옵션 A:** `duration_seconds` 기준 (빠를수록 좋음) — 현재 리더보드와 동일 기준. **추천.**
- **옵션 B:** 페이스(m/s) 기준 — 거리 차이가 있을 때 더 공정하나, 코스 완주는 동일 경로이므로 시간 기준이 직관적.

### 결정 2: 완주 직후 피드백 화면 위치

현재 코스 완주 후 어디로 이동하는지 확인 필요:
- **옵션 A:** 완주 후 별도 `CourseAttemptResultScreen` — PR 피드백 + 공유 + 리더보드 바로가기
- **옵션 B:** 기존 완주 완료 다이얼로그/화면에 PR 정보 추가
- 코드 확인 후 결정 필요.

### 결정 3: 코스 평점 (B-38) 타이밍

B-37 직후 바로 B-38(평점)을 진행할지, 아니면 B-39(Auto-Pause)를 먼저 할지:
- **옵션 A:** B-37 → B-38 → B-39 (사용자 경험 중심 순서)
- **옵션 B:** B-37 → B-39 → B-38 (기술 품질 중심 순서)
- **추천:** B-37 → B-38 순서. 평점은 추가 사용자 행동을 유발해 데이터 축적에 좋음.

### 결정 4: Global Leaderboard 탭 처리

B-17에서 설계됐지만 현재 탭 상태 확인 필요:
- 현재 BottomNav에 LEADERBOARD 탭이 남아있는지, B-27에서 COURSES로 교체됐는지 확인.
- 만약 아직 LEADERBOARD 탭이 placeholder 상태라면 B-37과 함께 처리 권장.

---

## 15. Git Status

```
현재 브랜치: feature/course-deviation-warning
최근 커밋:
  bde167b fix(android): propagate run deletion to home and my-runs lists
  b91c727 feat(android): move scroll hint to left/right sides and enlarge icons
  1336df9 feat(android): add bouncing scroll hint at bottom of run hero section
  430d49a feat: [B-36] add course deviation warning with haptic feedback
  95883cf feat: [B-35] add permission explanation screen and splash tagline

미커밋 변경 파일 (현재 feature/course-deviation-warning 브랜치):
  M app/src/main/java/.../ui/components/RunHeroSection.kt
  M app/src/main/java/.../ui/home/HomeScreen.kt
  M app/src/main/java/.../ui/home/HomeViewModel.kt
  M app/src/main/java/.../ui/navigation/RunwayNavGraph.kt
  M app/src/main/java/.../ui/running/history/RunDetailScreen.kt

주의: B-37 시작 전 이 변경 사항들을 커밋 또는 stash 처리 필요.
```
