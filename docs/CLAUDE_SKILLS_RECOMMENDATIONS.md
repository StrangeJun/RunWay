# RunWay Android 프로젝트 Claude Skills 추천

작성일: 2026-05-21  
참고: SkillsMP는 커뮤니티 기반 스킬 마켓플레이스이며, Anthropic 공식 서비스가 아닙니다. 설치 전에는 반드시 `SKILL.md`, 포함 스크립트, 허용 도구, 저장소 변경 이력을 검토하세요.

## 프로젝트 기준

현재 RunWay Android는 Kotlin 2.0.21, AGP 8.13.2 기반의 단일 Android 앱입니다. 주요 기술은 Jetpack Compose, Material 3, Navigation Compose, Hilt, Retrofit/OkHttp, DataStore, Google Maps/Maps Compose, Play Services Location, Room, Coroutines/Flow입니다.

코드 구조는 `core`, `data`, `domain`, `di`, `ui`로 나뉘며 러닝 트래킹, 코스 탐색, 코스 도전, 통계, 리마인더, 공유 이미지, 인증 기능이 들어 있습니다. 아직 테스트 소스가 거의 없고, `CLAUDE.md`는 초기 빈 프로젝트 설명으로 남아 있어 Claude 작업 품질을 높일 여지가 큽니다.

## 우선 설치 추천

| 우선순위 | 스킬 | 추천 이유 | 적용 범위 |
| --- | --- | --- | --- |
| 1 | `android-expert` | Compose, ViewModel/state, Flow, Hilt, Navigation, 테스트, 성능, Material 3까지 현재 앱의 핵심 스택과 가장 잘 맞습니다. | 기능 구현, 코드 리뷰, 아키텍처 결정, Compose 성능 점검 |
| 2 | `android-agent-skills` | Clean Architecture, Compose, MVI 상태 관리, Repository/UseCase/ViewModel/Screen 생성 패턴을 제공합니다. 현재 레이어 구조를 더 일관되게 만들 때 유용합니다. | 신규 화면, Repository 패턴 정리, UseCase 도입 검토 |
| 3 | `android-ux` | Material 3 UX, 터치 타깃, 간격, 내비게이션, 접근성, 애니메이션 타이밍을 점검합니다. 러닝 앱은 사용 중 화면 가독성과 조작 안정성이 중요하므로 가치가 큽니다. | `RunningTrackingScreen`, `CourseDetailScreen`, `StatsScreen`, 온보딩/인증 화면 |
| 4 | `accessibility` 또는 `accessimind-accessible-ui-agent-skill` | Compose 접근성, TalkBack, `contentDescription`, semantics, touch target, contrast 점검에 특화되어 있습니다. 지도/아이콘/차트/러닝 컨트롤이 많은 앱 특성상 필요합니다. | 릴리즈 전 화면 감사, 아이콘 버튼/차트/지도/커스텀 컴포넌트 점검 |
| 5 | `android-kotlin-core` | Kotlin idiom, nullability, sealed UI state, extension function, collection pipeline 정리에 특화되어 있습니다. ViewModel 상태 모델과 DTO 매핑 정리에 적합합니다. | Kotlin 리팩터링, UI state 모델 정리, 가독성 개선 |

## 조건부 추천

| 스킬 | 언제 쓰면 좋은가 | 주의점 |
| --- | --- | --- |
| `google-app-development` | Android/Google 플랫폼 전반, Compose-first 원칙, 폴더블/태블릿/다른 Google 플랫폼 대응을 검토할 때 | 범위가 넓으므로 현재는 Android phone 중심 규칙만 선별해서 사용 |
| `kotlin-coroutines-flows` | 러닝 트래킹, 위치 스트림, 백그라운드 서비스, 네트워크 재시도, Flow 테스트를 다룰 때 | SkillsMP 검색 결과상 개별 페이지보다 모바일 카테고리 목록에서 확인되는 스킬이라 설치 전 원 저장소 검토 필요 |
| `moai-lang-kotlin` | Kotlin 2.0/K2, Gradle Kotlin DSL, coroutine/Flow, Compose Multiplatform 관점의 점검이 필요할 때 | Android 전용 스킬은 아니므로 Android 프레임워크 판단은 `android-expert`가 우선 |
| `skill-usage` | 여러 스킬을 프로젝트 단위로 설치하고 `CLAUDE.md`에 스킬 목록을 자동 노출하고 싶을 때 | 현재 `CLAUDE.md`가 오래되어 있으므로 먼저 프로젝트 설명부터 갱신하는 편이 좋음 |

## 보류 또는 비추천

| 스킬/카테고리 | 판단 |
| --- | --- |
| 일반 `testing` 카테고리 | 한국어 카테고리 페이지는 현재 0개로 보이며, Android 테스트에는 전용 스킬이 더 적합합니다. 우선 `android-expert`, `android-agent-skills`의 테스트 지침을 사용하세요. |
| 일반 `security` 카테고리 | 인증 토큰, 네트워크 보안, DataStore 저장 정책 점검에는 필요하지만 Android 맥락 없는 일반 보안 스킬은 오탐이 많을 수 있습니다. Android 보안 특화 스킬을 별도로 찾을 때까지 보류합니다. |
| 백엔드/Spring 계열 스킬 | RunWay Android 앱에는 직접 맞지 않습니다. 서버 API 계약 문서나 백엔드 저장소를 같이 다룰 때만 사용하세요. |
| iOS/SwiftUI/Apple Foundation Models 계열 | 현재 Android 앱과 직접 관련이 없습니다. |

## 추천 설치 순서

1. `android-expert`
2. `android-ux`
3. `accessibility` 또는 `accessimind-accessible-ui-agent-skill`
4. `android-agent-skills`
5. `android-kotlin-core`
6. 필요 시 `skill-usage`

처음부터 너무 많이 설치하기보다 위 1-3번을 먼저 적용하고, Claude가 실제 작업에서 과도하거나 충돌되는 규칙을 내지 않는지 확인한 뒤 확장하는 것이 좋습니다.

## 프로젝트에 맞춘 활용 예시

### 기능 구현

신규 러닝 기능이나 코스 기능을 만들 때는 `android-expert`를 기본으로 사용합니다. ViewModel의 `StateFlow`, Repository 호출, Hilt 주입, Navigation Compose 연결까지 현재 구조와 맞춰 구현하게 하는 데 적합합니다.

### UI/UX 리뷰

`android-ux`와 접근성 스킬을 조합해 `RunningTrackingScreen`, `CourseAttemptTrackingScreen`, 지도 미리보기 컴포넌트, 차트 컴포넌트를 검토합니다. 특히 러닝 중 조작 버튼은 터치 영역, 색 대비, 아이콘 설명, 상태 전달이 중요합니다.

### 코드 정리

`android-kotlin-core`는 DTO 매핑, UI state sealed type, extension function 정리, nullable 처리 개선에 적합합니다. 앱 전역 리팩터링보다 특정 화면이나 도메인 단위로 좁혀 적용하세요.

### 테스트 강화

현재 테스트 파일이 거의 없으므로 `android-expert` 또는 `android-agent-skills`로 다음 순서의 테스트를 생성하는 것이 현실적입니다.

1. 순수 Kotlin 로직: `DistanceCalculator`, `GpsPointValidator`, `RunChartCalculator`, `SplitCalculator`
2. ViewModel 상태 전이: 러닝 시작/일시정지/종료, 코스 도전 상태
3. Repository 네트워크 결과 매핑
4. Compose UI의 핵심 상태 표시와 접근성 semantics

## 설치 전 체크리스트

- 저장소가 공개되어 있고 최근 업데이트가 있는지 확인합니다.
- `SKILL.md` 외에 실행 스크립트, hooks, 명령 파일이 포함되어 있으면 내용을 읽습니다.
- `allowed-tools`가 과도하게 넓거나 외부 네트워크/파일 삭제 권한을 요구하면 프로젝트 로컬 설치를 피합니다.
- 프로젝트 규칙과 충돌하는 지침은 `.claude/skills/<skill>/SKILL.md`를 그대로 수정하기보다 `CLAUDE.md`에서 RunWay 규칙을 우선하도록 명시합니다.
- 설치 후 `CLAUDE.md`에 사용 가능한 스킬 목록과 트리거 상황을 짧게 추가합니다.

## SkillsMP에서 확인한 근거

- SkillsMP 카테고리 페이지는 스킬이 여러 카테고리에 속할 수 있다고 안내하며, 개발/테스트&보안/DevOps/문서화 등 큰 분류를 제공합니다: https://skillsmp.com/ko/categories
- SkillsMP FAQ는 스킬이 `SKILL.md` 중심의 모듈식 지침이며 Claude Code의 개인 또는 프로젝트 스킬 디렉터리에 설치할 수 있다고 설명합니다: https://skillsmp.com/ko/docs/faq
- `android-expert`: https://skillsmp.com/skills/oimiragieo-agent-studio-claude-skills-android-expert-skill-md
- `android-agent-skills`: https://skillsmp.com/skills/devtrongle-android-agent-skills-skill-md
- `android-ux`: https://skillsmp.com/ko/skills/rcosteira79-android-skills-plugins-android-skills-skills-android-ux-skill-md
- `android-kotlin-core`: https://skillsmp.com/skills/krutikjain-android-agent-skills-skills-android-kotlin-core-skill-md
- `google-app-development`: https://skillsmp.com/skills/provectus-awos-recruitment-registry-skills-google-app-development-skill-md
- `accessimind-accessible-ui-agent-skill`: https://skillsmp.com/skills/sarperarikan-accessimind-codex-agent-skill-skills-accessimind-accessible-ui-agent-skill-skill-md
- Android 접근성 스킬 예시: https://skillsmp.com/pt/skills/piyushverma0-android-agent-skills-skills-accessibility-skill-md
- `skill-usage`: https://skillsmp.com/skills/mgood-claude-skill-usage-skill-md
