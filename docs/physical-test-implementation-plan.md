# Physical Test 구현 계획

작성일: 2026-06-06  
브랜치: `feature/android-physical-test`

---

## 1. 개요

달리기 자세 분석 전에 **정면 T-포즈 + 외발 서기** 영상을 분석해 사용자의 기초 신체 능력을 측정한다.  
측정 결과는 러닝 퍼포먼스와 부상 위험을 해석하기 위한 기준점이 된다.

### 측정 지표

| 지표 | 설명 |
|------|------|
| **관절 가동 범위** (ROM) | 어깨 외전각 — 팔이 수평에서 얼마나 펴졌는지 |
| **신체 대칭성** (Symmetry) | 좌우 어깨 높이 차이 + 골반 기울기 |
| **균형** (Balance) | 외발 서기 구간 골반 중심의 lateral 흔들림 |
| **근력(기능적)** (Strength) | Trendelenburg sign — 외발 서기 시 반대쪽 골반 하강량 |

---

## 2. 사용자 흐름

```
PostureHomeScreen
├── [기초 체력 측정] 버튼
│   → PhysTestGuideScreen       촬영 방법 안내 (T포즈 순서 설명)
│   → PhysTestCaptureScreen     갤러리에서 영상 선택 (T포즈 실루엣 가이드 표시)
│   → PhysTestAnalyzingScreen   MediaPipe 분석 중
│   → PhysTestResultScreen      4개 지표 카드 + "러닝 자세 분석하기" 버튼
│       └──────────────────────→ PostureCaptureScreen (기존 자세 분석 플로우)
└── [러닝 자세 분석] 버튼
    → 기존 PostureCaptureScreen 플로우
```

---

## 3. 촬영 가이드 (PhysTestGuideScreen 안내 내용)

사용자에게 다음 순서로 촬영하도록 안내한다.

1. **T-포즈 유지** (약 3초) — 정면을 바라보고 팔을 양옆으로 수평으로 뻗는다.
2. **왼쪽 무릎 들기** (약 3초) — 오른발로 서서 왼쪽 무릎을 90° 들어 올린다.
3. **오른쪽 무릎 들기** (약 3초) — 왼발로 서서 오른쪽 무릎을 90° 들어 올린다.

총 영상 길이: **약 10초**  
촬영 방향: **정면 (전신이 화면에 들어오도록)**

---

## 4. 점수 산출 알고리즘

### 4-1. 관절 가동 범위 (ROM)

- **측정값**: T-포즈 구간 프레임의 어깨 외전각 중앙값
  - 어깨 외전각 = `threePointAngle(hip, shoulder, elbow)` (좌우 평균)
- **이상 범위**: 80° ~ 100°
- **점수**: 기존 `angleScore(value, idealMin, idealMax)` 사용

### 4-2. 신체 대칭성 (Symmetry)

- **측정값**: T-포즈 구간에서 다음 두 값의 합
  - 좌우 어깨 Y 좌표 차이 / body height (정규화)
  - 좌우 골반 Y 좌표 차이 / body height (정규화)
- **이상 범위**: 0 ~ 0.02 (body height의 2% 이내)
- **점수**: 비율이 클수록 낮은 점수 (역방향)

### 4-3. 균형 (Balance)

- **측정값**: 외발 서기 구간에서 골반 중심 X 좌표의 IQR / body width
  - 골반 중심 X = `(leftHip.x + rightHip.x) / 2`
  - body width 기준 = 어깨 너비 (leftShoulder.x ~ rightShoulder.x 범위)
- **이상 범위**: 0 ~ 0.02 (body width의 2% 이내)
- **점수**: 흔들림이 클수록 낮은 점수 (역방향)

### 4-4. 근력 — Trendelenburg Sign (Strength)

- **측정값**: 외발 서기 구간에서 지지발 반대쪽 골반의 Y 하강량 / body height
  - 예: 오른발로 서면 왼쪽 골반이 내려가는 정도
  - 기준: T-포즈 구간의 골반 Y 중앙값 대비 외발 서기 구간 최대 하강량
- **이상 범위**: 0 ~ 0.03 (body height의 3% 이내)
- **점수**: 하강량이 클수록 낮은 점수 (역방향)

### 4-5. 전체 점수

```
overall = ROM × 0.30 + Symmetry × 0.25 + Balance × 0.25 + Strength × 0.20
```

### 4-6. 등급

기존 `PostureResult.gradeFrom()` 기준 동일 (S/A/B/C/D).

---

## 5. 파일 구조

### 신규 생성

```
android/app/src/main/java/com/runway/android/
├── core/phystest/
│   ├── PhysTestFrameData.kt        프레임별 정면 랜드마크 (어깨/골반/무릎 XY + 타임스탬프)
│   ├── PhysTestResult.kt           4개 카테고리 결과 (PostureCategoryResult 재사용)
│   ├── PhysTestAnalyzer.kt         MediaPipe PoseLandmarker 정면 영상 분석
│   ├── PhysTestRuleEngine.kt       4개 지표 점수화 알고리즘
│   └── local/
│       ├── PhysTestEntity.kt       Room 엔티티
│       └── PhysTestDao.kt          DAO (insert, observeAll, findById, deleteById)
└── ui/phystest/
    ├── PhysTestViewModel.kt        상태 관리 (Idle/Analyzing/Success/Error)
    ├── PhysTestGuideScreen.kt      촬영 방법 안내 + 시작 버튼
    ├── PhysTestCaptureScreen.kt    갤러리 영상 선택 + T포즈 실루엣 가이드
    └── PhysTestResultScreen.kt     4개 지표 카드 + 러닝 자세 분석 연결 버튼
```

> `PhysTestAnalyzingScreen`은 기존 `PostureAnalyzingScreen`을 그대로 재사용한다.

### 수정

| 파일 | 변경 내용 |
|------|----------|
| `core/posture/local/PostureDatabase.kt` | version 3→4, `PhysTestEntity` entities 추가, `MIGRATION_3_4` |
| `di/PostureModule.kt` | `PhysTestDao`, `PhysTestAnalyzer` provide 추가 |
| `ui/posture/PostureHomeScreen.kt` | "기초 체력 측정" 버튼 + 이전 결과 요약 카드 추가 |
| `ui/navigation/MainScaffold.kt` | phystest 상태 변수 + 화면 분기 추가 |

---

## 6. 데이터 모델

### PhysTestFrameData

```kotlin
data class PhysTestFrameData(
    val timestampMs: Long,
    val leftShoulderX: Float, val leftShoulderY: Float,
    val rightShoulderX: Float, val rightShoulderY: Float,
    val leftHipX: Float, val leftHipY: Float,
    val rightHipX: Float, val rightHipY: Float,
    val leftKneeY: Float,
    val rightKneeY: Float,
    val leftAnkleY: Float,
    val rightAnkleY: Float,
    val visibility: Float,
)
```

### PhysTestResult

```kotlin
data class PhysTestResult(
    val overallScore: Int,
    val grade: String,
    val overallFeedback: String,
    val rom: PostureCategoryResult,           // 관절 가동 범위
    val symmetry: PostureCategoryResult,      // 신체 대칭성
    val balance: PostureCategoryResult,       // 균형
    val strength: PostureCategoryResult,      // 근력(기능적)
)
```

### PhysTestEntity (Room)

```
id, createdAt, overallScore, grade, overallFeedback,
romScore, romMeasured, romFeedback, romTip,
symmetryScore, symmetryMeasured, symmetryFeedback, symmetryTip,
balanceScore, balanceMeasured, balanceFeedback, balanceTip,
strengthScore, strengthMeasured, strengthFeedback, strengthTip,
videoPath (nullable)
```

---

## 7. DB 마이그레이션

- `PostureDatabase` version: **3 → 4**
- `MIGRATION_3_4`: `phys_test_results` 테이블 신규 생성
- `fallbackToDestructiveMigration()` 유지

---

## 8. 구현 순서

| 단계 | 작업 | 완료 |
|------|------|------|
| 1 | `PhysTestFrameData`, `PhysTestResult` 데이터 클래스 | - |
| 2 | `PhysTestEntity`, `PhysTestDao` | - |
| 3 | `PostureDatabase` v4 마이그레이션 | - |
| 4 | `PhysTestAnalyzer` (MediaPipe 정면 분석) | - |
| 5 | `PhysTestRuleEngine` (4개 지표 점수화) | - |
| 6 | `PhysTestViewModel` | - |
| 7 | `PhysTestGuideScreen` | - |
| 8 | `PhysTestCaptureScreen` | - |
| 9 | `PhysTestResultScreen` | - |
| 10 | `PostureHomeScreen` 진입점 연결 | - |
| 11 | `MainScaffold` 플로우 연결 | - |
| 12 | `PostureModule` DI 등록 | - |
| 13 | 빌드 + 기기 테스트 | - |

---

## 9. 주의사항

- `PhysTestAnalyzer`는 정면 영상이므로 `PosturePoseAnalyzer`와 달리 **양쪽 랜드마크를 모두** 수집한다. 좌우 visibility 기준 dominant side 선택 로직은 불필요.
- 외발 서기 구간 자동 감지: 한쪽 발목 Y 좌표가 반대쪽 무릎 Y보다 높아지는 시점(무릎을 든 상태)으로 판단.
- T-포즈 구간 자동 감지: 양 손목 Y 좌표가 어깨 Y 좌표와 유사한(±10%) 구간.
- 영상이 너무 짧거나 T-포즈/외발서기 구간이 감지되지 않으면 각 지표를 "측정 불가"로 처리 (score=0).
- `PostureCategoryResult`를 재사용하므로 UI 카드 컴포넌트(`PostureCategoryCard`)도 그대로 재사용 가능.
