# AI 런닝 자세 분석 기능 설계 문서

**작성일:** 2026-06-05  
**범위:** Android 온디바이스 런닝 자세 분석 (Phase P-1 ~ P-2)

---

## 1. 개요

런닝머신에서 달리는 영상을 분석하여 자세 피드백을 제공하는 기능.  
MediaPipe Pose Landmarker로 관절 좌표를 추출하고, 스포츠 생체역학 기준치에 기반한 규칙 엔진으로 자세를 평가한다.

**최종 목표 (Phase P-2):** 좋은 자세 영상만 수집해 학습한 Autoencoder 기반 온디바이스 유사도 모델로 교체.  
입력 자세가 "좋은 자세 분포"와 얼마나 다른지를 재구성 오차(reconstruction error)로 측정하므로,  
나쁜 자세 예시 없이 좋은 자세 예시만으로 학습할 수 있다.

---

## 2. 목표 및 범위

### Phase P-1 (이번 구현)
- CameraX 영상 녹화 (10~15초, 옆면 촬영)
- 실루엣 가이드 오버레이로 카메라 배치 안내
- 5~10fps로 프레임 샘플링 후 MediaPipe Pose로 관절 각도 추출
- 규칙 기반(Rule-based) 판정 엔진
- 결과: 종합 점수(0~100) + 등급(S/A/B/C/D) + 5개 카테고리 피드백
- 분석 이력 로컬 저장 (Room)
- **TFLite 의존성 추가 안 함** — 규칙 엔진만으로 동작

### Phase P-2 (데이터 축적 후)
- 좋은 자세 영상 수집 → landmark 추출 → 정규화 → 학습 데이터셋 저장
- `tools/posture-model-training/` Python 파이프라인으로 Autoencoder 학습
- 학습된 모델을 `.tflite`로 변환 → 앱에 번들
- 앱에서는 TFLite **추론만** 수행 (학습은 별도 파이프라인에서 오프라인 진행)
- 규칙 엔진을 `PostureAutoencoderInference`로 교체

---

## 3. 분석 항목 및 기준

### 측정 항목 (MediaPipe landmark 번호 기준)

| 항목 | Landmark | 이상 범위 | 근거 |
|---|---|---|---|
| 무릎 굴곡각 (착지 시) | 23·25·27 | 155~170° | Novacheck 1998 |
| 상체 전방 기울기 | 11·23 연결선 vs 수직 | 5~10° | Heiderscheit et al. 2011 |
| 팔꿈치 각도 | 11·13·15 | 85~95° | Jack Daniels' Running Formula |
| 고관절 신전 (push-off) | 23·25 | 175~185° | Williams & Cavanagh 1987 |
| 오버스트라이드 | 27 vs 23 (x축 거리) | 발목이 엉덩이 x좌표 기준 ≤ 0.15 * 신장 앞 | Heiderscheit et al. 2011 |

### 오버스트라이드 측정 방법
- **착지 프레임 감지:** 발목(27) y좌표가 프레임 간 최솟값을 기록하는 순간 = 초기 착지
- **오버스트라이드 판정:** 해당 프레임에서 `(발목 x) - (엉덩이 x)` 거리가 신장 추정치의 15% 초과
- **신장 추정:** 어깨~발목 landmark 수직 거리 × 1.2 (화면 기준 상대값 사용)
- 측면 촬영이 아닌 경우 x좌표 신뢰도가 낮아지므로, MediaPipe 가시성(visibility) 점수 < 0.6인 프레임은 제외

### 프레임 샘플링
- 녹화된 MP4에서 5~10fps로 균등 샘플링 (전체 프레임의 약 1/3~1/6)
- 10~15초 영상 기준: 약 50~150프레임 처리

### 속도 관련 주의사항
- Phase P-1에서는 단일 기준치 적용
- Phase P-2 Autoencoder는 학습 데이터에 다양한 속도 포함 시 자동 반영

---

## 4. 아키텍처

### 4-1. Phase P-1 흐름

```
PostureCaptureScreen
    → CameraX 영상 녹화 (MP4, 10~15초)
    → 녹화 완료 후 "분석 시작" 버튼
    → PostureAnalysisViewModel.analyze(videoUri)
        → PosturePoseAnalyzer.extractAngles(videoUri, fps=7)
            → MediaPipe Pose Landmarker (5~10fps 샘플링)
            → 각 프레임: PostureAngleCalculator.compute(landmarks)
            → List<PostureFrameAngles> 반환
        → PostureFrameAngles 집계 (착지 프레임 중앙값)
        → PostureRuleEngine.evaluate(aggregatedAngles)
        → PostureResult 생성 → Room 저장
    → PostureResultScreen
```

### 4-2. Phase P-2 흐름 (앱 내부)

```
PostureCaptureScreen (동일)
    → PostureAnalysisViewModel.analyze(videoUri)
        → PosturePoseAnalyzer.extractAngles(videoUri, fps=7) (동일)
        → PostureAutoencoderInference.evaluate(frameAngles)
            → 각도 시퀀스 정규화
            → .tflite Autoencoder 추론
            → 재구성 오차 계산 (MSE per dimension)
            → 오차 → 점수/등급/피드백 변환
        → PostureResult 생성 → Room 저장
    → PostureResultScreen (동일)
```

### 4-3. 컴포넌트

```
ui/posture/
├── PostureHomeScreen.kt            # 이력 목록 + 새 분석 시작
├── PostureCaptureScreen.kt         # 카메라 가이드 + 녹화
├── PostureResultScreen.kt          # 결과 표시
└── PostureAnalysisViewModel.kt     # 분석 상태 관리

core/posture/
├── PosturePoseAnalyzer.kt          # MediaPipe 래퍼 (영상 → 프레임별 landmarks, fps 샘플링)
├── PostureAngleCalculator.kt       # Landmark 좌표 → 각도 + 오버스트라이드 계산
├── PostureFrameAngles.kt           # 단일 프레임 각도 데이터 클래스
├── PostureRuleEngine.kt            # 각도 → 점수/등급/피드백 (Phase P-1 규칙 기반)
├── PostureAutoencoderInference.kt  # TFLite 추론 래퍼 (Phase P-2, 교체용)
└── PostureResult.kt                # 결과 데이터 모델

core/posture/local/
├── PostureAnalysisEntity.kt        # Room entity
├── PostureAnalysisDao.kt           # 조회/저장/삭제
└── PostureDatabase.kt              # RoomDatabase
```

**`PostureRuleEngine`과 `PostureAutoencoderInference`는 동일한 인터페이스를 구현해 교체 가능하도록 설계:**
```kotlin
interface PostureEvaluator {
    fun evaluate(frames: List<PostureFrameAngles>): PostureResult
}
```

### 4-4. BottomNav 변경

현재 4탭 → 5탭으로 확장:
```
홈 | 탐색 | [런닝 FAB] | 기록 | 자세(신규)
```
`MainScaffold.kt`의 BottomNavItem 목록에 `자세` 항목 추가.  
`NavGraph`에 posture nested graph 추가.

---

## 5. 화면 상세

### 5-1. PostureCaptureScreen

**카메라 가이드 단계 (녹화 전):**
- 배경: 카메라 프리뷰
- 오버레이: 측면 달리기 실루엣 (Compose Canvas로 직접 그림)
- 안내 텍스트: "카메라를 옆면 허리 높이에 두고, 전신이 화면에 들어오도록 맞추세요"
- "준비됐어요" 버튼으로 녹화 시작

**녹화 단계:**
- 우측 상단: 경과 시간 표시 (00:00 ~ 00:15)
- 진행 바: 10초 도달 시 초록색으로 변경 ("충분해요")
- 15초 자동 종료 또는 하단 정지 버튼
- 10초 미만 녹화 시 경고: "10초 이상 녹화하면 더 정확합니다"

**분석 중 상태:**
- 로딩 인디케이터 + "자세를 분석하고 있어요..." 텍스트

### 5-2. PostureResultScreen

**헤더:**
- 종합 점수 (큰 숫자, 예: 74)
- 등급 배지 (S/A/B/C/D, 색상 구분)
- 총평 텍스트 1~2줄

**카테고리 카드 5개:**
```
[무릎 굴곡] 82점
측정값: 162°  이상범위: 155~170°
피드백: 착지 시 무릎 각도가 이상적입니다.

[상체 기울기] 55점
측정값: 2.3°  이상범위: 5~10°
피드백: 상체를 약 5도 앞으로 기울여 보세요.
팁: 전방 기울기는 추진력과 효율을 높여줍니다.

[오버스트라이드] 70점
측정값: 발 착지가 엉덩이보다 18% 앞
피드백: 착지 위치가 약간 앞쪽입니다. 보폭을 줄여보세요.
```

**하단 버튼:**
- "다시 분석하기" → PostureCaptureScreen
- "이력 보기" → PostureHomeScreen

### 5-3. PostureHomeScreen

- 최근 분석 카드 목록 (날짜, 종합 점수, 등급)
- 카드 탭 → PostureResultScreen (저장된 결과)
- 우측 하단 FAB: "새 분석 시작"

---

## 6. 점수 계산 (Phase P-1 규칙 기반)

### 카테고리 점수

```
이상 범위 내:              100점
범위 이탈 (경미, ≤5°):    70~90점  (선형 감점)
범위 이탈 (중간, 5~15°):   40~70점
범위 이탈 (심함, >15°):    0~40점
```

오버스트라이드는 각도가 아닌 신장 대비 비율로 판정:
```
≤ 10%:   100점  (이상)
10~20%:  60~90점
20~30%:  30~60점
> 30%:    0~30점
```

### 종합 점수 가중치

| 카테고리 | 가중치 |
|---|---|
| 무릎 굴곡각 | 30% |
| 상체 기울기 | 25% |
| 오버스트라이드 | 20% |
| 팔꿈치 각도 | 15% |
| 고관절 신전 | 10% |

### 등급

| 점수 | 등급 |
|---|---|
| 90~100 | S |
| 75~89 | A |
| 60~74 | B |
| 45~59 | C |
| 0~44 | D |

---

## 7. Phase P-2: Autoencoder 기반 유사도 모델

### 설계 원칙
- **좋은 자세 영상만** 학습 데이터로 사용 (나쁜 자세 예시 불필요)
- Autoencoder가 "좋은 자세 분포"를 학습
- 추론 시: 입력 자세 시퀀스를 재구성하고, 재구성 오차가 클수록 좋은 자세에서 멀다고 판정
- 각 차원(카테고리)별 오차로 어느 항목이 나쁜지 특정 가능

### 모델 구조

```
입력: 프레임 시퀀스 × 5개 각도 (shape: [T, 5])
      T = 샘플링된 프레임 수 (약 70~150)

Encoder:
  Dense(32, relu) → Dense(16, relu) → latent(8)

Decoder:
  Dense(16, relu) → Dense(32, relu) → Dense(5, linear)  ← 5개 각도 재구성

출력: 재구성된 각도 시퀀스 (shape: [T, 5])
재구성 오차: MSE per dimension (각 카테고리별 개별 점수로 변환)
```

> 프레임 순서 의존성이 강하면 LSTM Autoencoder로 교체 가능.  
> Phase P-2 초기에는 단순 Dense로 시작해 검증 후 결정.

### 학습 데이터 수집 구조

좋은 자세 영상 수집 후 다음 파이프라인으로 학습 데이터셋을 생성:

```
tools/posture-model-training/
├── collect/
│   └── extract_landmarks.py      # 영상 → MediaPipe → 각도 CSV 추출
├── preprocess/
│   └── normalize.py              # 신장 정규화, 속도 정규화, 이상치 제거
├── dataset/
│   └── good_posture_sequences.npy  # 학습용 시퀀스 배열 (shape: [N, T, 5])
├── train/
│   └── train_autoencoder.py      # Autoencoder 학습 (TensorFlow/Keras)
├── export/
│   └── export_tflite.py          # .h5 → .tflite 변환 + 양자화(int8 optional)
└── assets/
    └── posture_autoencoder.tflite  # 앱에 배포할 모델 파일
```

### 앱 내 추론 흐름

```kotlin
// PostureAutoencoderInference.kt
class PostureAutoencoderInference(context: Context) : PostureEvaluator {

    private val interpreter = Interpreter(
        FileUtil.loadMappedFile(context, "posture_autoencoder.tflite")
    )

    override fun evaluate(frames: List<PostureFrameAngles>): PostureResult {
        val input = frames.toNormalizedFloatArray()   // shape [T, 5]
        val output = Array(frames.size) { FloatArray(5) }
        interpreter.run(input, output)
        val msePerDim = computeMsePerDimension(input, output)  // FloatArray(5)
        return msePerDim.toPostureResult()
    }
}
```

**앱에서는 추론(`interpreter.run`)만 수행.** 학습(`train_autoencoder.py`)은 `tools/` 파이프라인에서 오프라인으로 진행하고, 결과 `.tflite` 파일을 앱 `assets/`에 배포.

### Phase P-2 전환 조건
- 좋은 자세 영상: 전문 러너/코치 영상 200건 이상 (다양한 체형·속도)
- 학습 후 검증: 규칙 엔진 결과와 Autoencoder 결과 비교 → 상관관계 0.8 이상
- 재구성 오차 → 점수 변환 함수 캘리브레이션 완료
- 모델 파일 크기 ≤ 5MB (TFLite int8 양자화 적용 시 달성 가능)

---

## 8. 데이터 모델

### PostureFrameAngles

```kotlin
data class PostureFrameAngles(
    val kneeFlexAngle: Float,        // 무릎 굴곡각 (도)
    val trunkLeanAngle: Float,       // 상체 기울기 (도)
    val elbowAngle: Float,           // 팔꿈치 각도 (도)
    val hipExtensionAngle: Float,    // 고관절 신전각 (도)
    val overstrideRatio: Float,      // 오버스트라이드 비율 (신장 대비)
    val isLandingFrame: Boolean,     // 착지 프레임 여부
    val visibility: Float,           // MediaPipe 가시성 평균 (0~1)
)
```

### Room Entity

```kotlin
@Entity(tableName = "posture_analyses")
data class PostureAnalysisEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val overallScore: Int,
    val grade: String,
    val overallFeedback: String,
    val kneeScore: Int,
    val kneeMeasuredAngle: Float,
    val kneeFeedback: String,
    val trunkScore: Int,
    val trunkMeasuredAngle: Float,
    val trunkFeedback: String,
    val elbowScore: Int,
    val elbowMeasuredAngle: Float,
    val elbowFeedback: String,
    val hipScore: Int,
    val hipMeasuredAngle: Float,
    val hipFeedback: String,
    val overstrideScore: Int,
    val overstrideRatio: Float,
    val overstrideFeedback: String,
)
```

---

## 9. 의존성

### Phase P-1

```kotlin
// MediaPipe (포즈 추출)
implementation("com.google.mediapipe:tasks-vision:0.10.14")
```

APK 크기 약 10~15MB 증가.  
**TFLite 의존성은 Phase P-1에서 추가하지 않는다.**

### Phase P-2 추가

```kotlin
// TFLite (Autoencoder 추론)
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
```

---

## 10. 제외 범위 (Phase P-1)

- 케이던스(보폭수) 측정 — 정확도 낮음, Phase P-2에서 재검토
- 영상 저장/공유 — 개인정보 이슈, 별도 검토 필요
- 백엔드 연동 — 온디바이스로 충분
- 실시간 분석 — 녹화 후 분석으로 충분, 배터리 소모 고려
- 앱 내 모델 학습 — 절대 불가 (학습은 `tools/` 파이프라인에서만)
