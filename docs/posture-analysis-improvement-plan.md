# 자세 분석 기능 개선 계획

> 작성일: 2026-06-06  
> 대상 브랜치: `main` (현재 커밋 기준)  
> 범위: `android/` — 자세 분석 파이프라인 및 영상 리플레이 오버레이

---

## 현재 구조 요약

```
녹화 (CameraX VideoCapture)
  ↓
PosturePoseAnalyzer.extractAnalysis()
  ├── MediaMetadataRetriever.getFrameAtTime()  // 7fps, OPTION_CLOSEST_SYNC
  ├── Bitmap 회전 보정
  ├── MediaPipe PoseLandmarker (RunningMode.IMAGE, detect())
  └── 각도 계산 + SkeletonPoint 수집
  ↓
PostureRuleEngine.evaluate()  // 각도 중앙값 → 점수
  ↓
Room DB 저장 (videoPath, videoFramesJson, 점수)
  ↓
PostureVideoPlayerCard  // ExoPlayer + Canvas 보간 오버레이
```

**주요 제약사항**

| 항목 | 현재 값 | 문제 |
|------|---------|------|
| 모델 | `pose_landmarker_lite.task` (5.5MB) | 정확도 최하위 |
| RunningMode | `IMAGE` | 프레임 간 추적 없음, 튐 발생 |
| 프레임 추출 | `OPTION_CLOSEST_SYNC`, 7fps | 키프레임만 추출, 시간 오차 큼 |
| 프레임 탐색 | `lastOrNull { it.t <= posMs }` | O(n) 선형 탐색 |
| 스무딩 | 선형 보간(lerp)만 적용 | 고속 동작 시 오버슈트 |

---

## 개선 영역 1 — RunningMode.IMAGE → VIDEO 전환

### 현재 동작과 문제

```kotlin
// 현재: 매 프레임이 독립적으로 분석됨
val result = landmarker.detect(mpImage)
```

`RunningMode.IMAGE`는 각 프레임을 완전히 독립적으로 처리한다. MediaPipe 내부 칼만 필터(Kalman filter)와 포즈 추적 상태가 초기화되므로, 프레임 간 연속성이 없어 같은 관절이 프레임마다 다른 위치로 튀는 현상이 발생한다.

### 변경 내용

```kotlin
// 변경 후: 타임스탬프 기반 시퀀셜 처리
val result = landmarker.detectForVideo(mpImage, timestampMs)
```

`RunningMode.VIDEO`에서는 PoseLandmarker가 내부적으로 다음을 수행한다.

- **포즈 추적**: 이전 프레임에서 감지한 포즈를 다음 프레임 탐색의 초기 추정값으로 사용
- **칼만 필터 기반 평활화**: 관절 위치가 물리적으로 가능한 범위 내에서 갱신됨
- **Re-ID 없이 연속 추적**: 포즈가 일시적으로 가려져도 이전 상태에서 이어서 추적

**타임스탬프 요구사항**: `detectForVideo()` 호출 시 전달하는 타임스탬프는 **단조 증가(monotonically increasing)** 해야 한다. 같은 타임스탬프를 두 번 전달하거나, 역순으로 전달하면 예외가 발생한다.

### 예상 효과

| 항목 | 개선 전 | 개선 후 |
|------|---------|---------|
| 프레임 간 랜드마크 안정성 | 불안정, 튐 있음 | 물리적으로 연속적인 움직임 |
| 착지 프레임 탐지 신뢰도 | 낮음 (노이즈 개별 처리) | 높음 (연속 추적 활용) |
| 무릎·고관절 각도 중앙값 정확도 | 노이즈 포함 | 추적 기반 안정화 후 측정 |
| 분석 속도 | ─ | 거의 동일 (탐지 시작 단계 비용 제거) |

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `PosturePoseAnalyzer.kt` | `buildLandmarker()` — `RunningMode.VIDEO` 설정; `detect()` → `detectForVideo(mpImage, timeMs)` |

### 구체적 코드 변경

```kotlin
// buildLandmarker()
val options = PoseLandmarker.PoseLandmarkerOptions.builder()
    .setBaseOptions(baseOptions)
    .setRunningMode(RunningMode.VIDEO)          // IMAGE → VIDEO
    .setNumPoses(1)
    .setMinPoseDetectionConfidence(0.5f)
    .setMinTrackingConfidence(0.5f)
    .build()

// 추출 루프 내부
val result = landmarker.detectForVideo(mpImage, timeMs)   // detect() → detectForVideo()
```

> **주의**: `RunningMode.VIDEO`에서는 `PoseLandmarker`를 단일 인스턴스로 재사용하고,  
> 한 번 닫으면 재사용할 수 없다. 현재 코드 구조(루프 전 생성, `finally`에서 닫기)는 이미 올바른 패턴이다.

### 리스크

- 타임스탬프가 역순이거나 중복되면 런타임 예외 → `timeMs` 단조 증가를 반드시 보장
- `OPTION_CLOSEST_SYNC`는 동일한 키프레임을 여러 타임스탬프에 대해 반환할 수 있음 → 아래 영역 4에서 별도 대응

---

## 개선 영역 2 — 스켈레톤 오버레이 스무딩

### 현재 동작

`PostureVideoPlayerCard.kt`의 `interpolateFrame()`이 인접 두 프레임 사이를 **선형 보간(linear interpolation)**한다. 이 방식은 단순하지만:

- 고속 동작(착지, 팔 스윙) 시 보간이 따라가지 못해 관절이 부자연스럽게 이동
- visibility가 낮은 관절을 그대로 보간해 오버레이에서 불안정하게 표시
- 방향 전환(두 프레임 사이 각도가 뒤집히는 경우) 시 중간에 이상한 위치를 통과

### 권장 스무딩 방법: EMA (Exponential Moving Average)

**One Euro Filter**가 이론적으로 더 우수하지만, 구현 복잡도 대비 효과 차이가 크지 않다.  
러닝 분석처럼 동작 속도가 일정한 경우 EMA로 충분하다.

```
smoothed_t = α × raw_t + (1 − α) × smoothed_{t-1}
```

- `α = 0.35` (권장): 반응성과 안정성의 실용적 균형점
- `α`가 클수록 반응 빠름(노이즈 많음), 작을수록 안정적(지연 있음)

### 구현 방안

`SkeletonSmoother` 클래스를 신규 생성한다. 재생 중 각 관절의 평활화 상태를 유지한다.

```kotlin
// 신규 파일: PostureSkeletonSmoother.kt
class SkeletonSmoother(private val alpha: Float = 0.35f) {
    private var prev: List<SkeletonPoint>? = null

    fun smooth(raw: List<SkeletonPoint>): List<SkeletonPoint> {
        val p = prev
        val result = if (p == null || p.size != raw.size) {
            raw
        } else {
            raw.zip(p).map { (r, s) ->
                // 가시성이 낮은 관절은 이전 상태를 더 강하게 유지
                val effectiveAlpha = if (r.v < 0.5f) alpha * 0.3f else alpha
                SkeletonPoint(
                    lerp(s.x, r.x, effectiveAlpha),
                    lerp(s.y, r.y, effectiveAlpha),
                    lerp(s.v, r.v, effectiveAlpha),
                )
            }
        }
        prev = result
        return result
    }

    fun reset() { prev = null }
}
```

`PostureVideoPlayerCard`에서는 seek 시 `smoother.reset()`을 호출한다.

### 급격한 좌표 점프 방지

EMA 이전에 **클리핑(clipping)** 단계를 추가한다.

```kotlin
// 한 프레임 간 이동이 정규화 좌표 기준 0.15 이상이면 클리핑
fun SkeletonPoint.clampJump(prev: SkeletonPoint, maxDelta: Float = 0.15f): SkeletonPoint {
    val dx = (x - prev.x).coerceIn(-maxDelta, maxDelta)
    val dy = (y - prev.y).coerceIn(-maxDelta, maxDelta)
    return copy(x = prev.x + dx, y = prev.y + dy)
}
```

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `PostureSkeletonSmoother.kt` | 신규 생성 — EMA + 점프 클리핑 |
| `PostureVideoPlayerCard.kt` | `remember { SkeletonSmoother() }` 인스턴스 보유; seek 시 `reset()`; 보간 결과를 EMA에 통과 |

### 파이프라인

```
interpolateFrame() → clampJump() → SkeletonSmoother.smooth() → SkeletonOverlay 렌더링
```

### 리스크

- `alpha`가 너무 작으면 슬로우모션에서 관절이 실제 위치보다 뒤처져 보임
- seek 이후 `reset()` 미호출 시 이전 위치에서부터 서서히 수렴하는 부자연스러운 동작 발생

---

## 개선 영역 3 — 오버레이 탐색 성능 최적화

### 현재 동작과 문제

```kotlin
// PostureVideoPlayerCard.kt — interpolateFrame()
val before = frames.lastOrNull { it.t <= posMs }   // O(n) 전체 탐색
val after  = frames.firstOrNull { it.t > posMs }   // O(n) 전체 탐색
```

80ms마다 재생 위치를 갱신할 때마다 `videoFrames` 전체를 두 번 순회한다.  
프레임 수가 많을수록(15초 × 10fps = 150개) 낭비가 커진다.

### 변경 방안: Binary Search 기반 인덱스 탐색

프레임 리스트는 타임스탬프 순으로 정렬돼 있다는 것이 보장된다 (삽입 순서대로 추가하므로).  
`Collections.binarySearch` 또는 수동 bisect 구현으로 O(log n)으로 줄인다.

```kotlin
private fun findFrameIndex(frames: List<PostureVideoFrame>, posMs: Long): Int {
    var lo = 0; var hi = frames.size - 1
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        if (frames[mid].t <= posMs) lo = mid else hi = mid - 1
    }
    return lo
}

private fun interpolateFrame(frames: List<PostureVideoFrame>, posMs: Long): PostureVideoFrame? {
    if (frames.isEmpty()) return null
    val idx = findFrameIndex(frames, posMs)
    val before = frames[idx]
    val after  = frames.getOrNull(idx + 1) ?: return before
    val range  = (after.t - before.t).toFloat()
    if (range <= 0f) return before
    val t = ((posMs - before.t) / range).coerceIn(0f, 1f)
    val pts = before.pts.zip(after.pts).map { (a, b) ->
        SkeletonPoint(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.v, b.v, t))
    }
    return PostureVideoFrame(posMs, pts)
}
```

### 폴링 주기 조정

현재 80ms(약 12.5fps) 폴링은 적절하다. 30fps 영상에서 80ms = 2.4프레임이므로 오버레이가  
최대 2프레임 지연될 수 있으나 육안으로는 거의 감지되지 않는다.  
0.25x 슬로우모션 시에는 80ms × 4 = 320ms 간격으로 실제 동작 40ms가 표시되므로  
오버레이가 더 부드럽게 느껴진다. 폴링 주기 변경은 필요 없다.

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `PostureVideoPlayerCard.kt` | `interpolateFrame()` — binary search로 교체 |

---

## 개선 영역 4 — 프레임 추출 전략

### 현재 방식과 근본적 한계

```kotlin
retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
```

`OPTION_CLOSEST_SYNC`는 요청한 시각에서 **가장 가까운 I-frame(키프레임)**을 반환한다.  
CameraX가 생성하는 H.264/H.265 영상에서 키프레임 간격은 보통 1~2초다.

**결과**: 7fps 요청 시 142ms마다 샘플링하지만, 키프레임 간격이 1초라면  
1초 구간의 7번 요청이 모두 **같은 키프레임 비트맵**을 반환한다.  
→ `RunningMode.VIDEO`의 타임스탬프 단조 증가 요구사항을 만족하더라도,  
실제로는 동일 포즈를 7번 반복 입력하는 것과 같다.

### 옵션 비교

| 방식 | 속도 | 정확도 | 설명 |
|------|------|--------|------|
| `OPTION_CLOSEST_SYNC` | ✅ 빠름 | ❌ 키프레임만 | 현재 방식 |
| `OPTION_CLOSEST` | ❌ 매우 느림 | ✅ 정확 | 15초 × 7fps = 105회 개별 디코딩, 60~120초 소요 |
| `OPTION_PREVIOUS_SYNC` | ✅ 빠름 | △ 직전 키프레임 | CLOSEST_SYNC와 유사 |
| `MediaCodec` + `MediaExtractor` | ✅ 빠름 | ✅ 정확 | 단방향 스트리밍, 구현 복잡 |

### 권장 방식: `OPTION_CLOSEST_SYNC` + 샘플링 주기 단축

`OPTION_CLOSEST`는 실용적이지 않다. `MediaCodec` 방식은 구현 복잡도가 높아 Phase 3에서 고려한다.

단기적으로는 **샘플링 FPS를 높여 키프레임 해상도를 높인다**.

```kotlin
// targetFps 7 → 10
// 15초 영상: 105개 → 150개 샘플 요청
// 키프레임 1초 간격 기준: 각 키프레임이 10회 반복 → 7회 반복으로 줄어듦
```

**추가 대응**: 동일한 비트맵이 반복 반환되는 현상을 탐지해 중복 프레임을 건너뛴다.

```kotlin
// 직전 프레임과 픽셀 체크섬(또는 픽셀 샘플링) 비교
var lastBitmapHash = 0
// ...
val hash = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2).hashCode()
if (hash == lastBitmapHash) { timeMs += intervalMs; continue }
lastBitmapHash = hash
```

픽셀 1개 비교는 충분하지 않을 수 있으므로, 4개 코너 픽셀 XOR 등으로 강화 가능.

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `PosturePoseAnalyzer.kt` | `targetFps` 기본값 7 → 10; 중복 프레임 건너뛰기 로직 추가 |

---

## 개선 영역 5 — 실시간 분석 vs 후처리 분석 비교

### 현재 흐름 (후처리)

```
녹화 (15초) → 영상 저장 → MediaPipe 분석 (5~15초) → 결과 표시
```

**장점**
- 구현 단순, 영상 파일이 리플레이에 재사용됨
- 분석이 실패해도 원본 영상이 보존됨
- 현재 `PostureAnalyzingScreen`으로 대기 중 UX 처리 완료

**단점**
- 분석 대기 시간 5~15초 (하드웨어에 따라 다름)
- 녹화와 분석 사이에 자원 전환 필요 (CameraX → MediaPipe)

---

### 대안 흐름 (실시간 분석)

```
CameraX Preview + ImageAnalysis → PoseLandmarker (VIDEO 모드, 실시간) → 랜드마크 타임라인 축적
                                                                              ↓
                                                            녹화 완료 후 점수 계산만 수행 (빠름)
```

**장점**
- 분석 대기 시간 거의 없음 (녹화 종료 후 즉시 점수 표시 가능)
- 사용자가 녹화 중 실시간 피드백을 볼 수 있는 옵션 (선택 기능)
- `RunningMode.LIVE_STREAM`으로 최적화된 비동기 처리 가능

**단점**
- CameraX `ImageAnalysis`와 `VideoCapture`의 동시 사용 (UseCase 충돌 가능)
  - CameraX 1.3+에서 `ConcurrentCameraSession` 또는 `VideoCapture` + `ImageAnalysis` 동시 바인딩은 디바이스마다 지원 여부가 다름
- 랜드마크를 실시간으로 저장할 메모리 버퍼 필요
- MediaPipe `RunningMode.LIVE_STREAM`은 콜백 기반이므로 비동기 처리 복잡도 증가
- 저사양 기기에서 VideoCapture + ImageAnalysis 동시 실행 시 프레임 드랍 위험

### 권장 결론

| 기준 | 후처리 (현재) | 실시간 |
|------|-------------|--------|
| 구현 복잡도 | 낮음 | 높음 |
| 대기 시간 | 5~15초 | ~0초 |
| 기기 호환성 | 높음 | 낮음 (디바이스별 UseCase 제한) |
| 영상 저장 | 자동 보존 | 별도 처리 필요 |
| 정확도 | VIDEO 모드 적용 시 동등 | 동등 |

**단기(Phase 1~2)**: 후처리 구조를 유지하면서 RunningMode.VIDEO 전환으로 정확도를 높인다.  
**장기(Phase 3)**: 기기 호환성 테스트 후 실시간 분석으로 전환을 검토한다.

---

## 단계별 구현 계획

### Phase 1 — 핵심 분석 정확도 개선 (즉시 시작, 리스크 낮음)

**목표**: 가장 높은 임팩트를 가장 낮은 리스크로  
**예상 작업량**: 약 2~3시간

| 작업 | 파일 | 상세 |
|------|------|------|
| RunningMode.IMAGE → VIDEO | `PosturePoseAnalyzer.kt` | `buildLandmarker()` + `detect()` → `detectForVideo()` |
| 샘플링 FPS 상향 | `PosturePoseAnalyzer.kt` | `targetFps` 기본값 7 → 10 |
| 중복 프레임 건너뛰기 | `PosturePoseAnalyzer.kt` | 키프레임 반복 탐지 로직 |

**검증 방법**: 동일 영상 분석 전후 `videoFramesJson` 파싱해 동일 타임스탬프 중복 여부 확인

---

### Phase 2 — 오버레이 품질 개선 (Phase 1 완료 후)

**목표**: 스켈레톤 시각적 안정성  
**예상 작업량**: 약 2~4시간

| 작업 | 파일 | 상세 |
|------|------|------|
| Binary search 프레임 탐색 | `PostureVideoPlayerCard.kt` | `interpolateFrame()` 교체 |
| `SkeletonSmoother` 구현 | `PostureSkeletonSmoother.kt` | EMA α=0.35 + 점프 클리핑 |
| Smoother를 VideoPlayerCard에 연결 | `PostureVideoPlayerCard.kt` | `remember { SkeletonSmoother() }`; seek 시 `reset()` |

**검증 방법**: 0.25x 슬로우모션에서 관절이 부드럽게 이동하는지 육안 확인; 급격한 위치 점프 없음 확인

---

### Phase 3 — 프레임 추출 정밀도 개선 (선택적)

**목표**: 키프레임 제한 극복  
**예상 작업량**: 약 1~2주 (복잡도 높음)

| 작업 | 파일 | 상세 |
|------|------|------|
| `MediaCodec` 기반 프레임 디코더 구현 | `PostureFrameDecoder.kt` (신규) | `MediaExtractor` + `MediaCodec` 순방향 디코딩 |
| Analyzer에서 새 디코더 사용 | `PosturePoseAnalyzer.kt` | `MediaMetadataRetriever` 교체 |

**조건**: Phase 1~2 완료 후 실제 사용성 테스트에서 여전히 프레임 추출 정밀도가 문제가 될 때만 진행

---

### Phase 4 — 실시간 분석 (장기, 선택적)

**목표**: 분석 대기 시간 제거  
**예상 작업량**: 약 2~4주

| 작업 | 파일 | 상세 |
|------|------|------|
| CameraX UseCase 호환성 조사 | — | 타겟 기기에서 VideoCapture + ImageAnalysis 동시 지원 확인 |
| `RunningMode.LIVE_STREAM` Analyzer 구현 | `PostureRealtimeAnalyzer.kt` (신규) | 비동기 콜백 기반 |
| 랜드마크 타임라인 버퍼 | `PostureCaptureScreen.kt` | 녹화 중 좌표 수집 |
| `PostureAnalyzingScreen` 제거 | `MainScaffold.kt` | 분석 대기 화면 불필요 |

---

## 리스크 및 트레이드오프

### RunningMode.VIDEO 관련

| 리스크 | 가능성 | 대응 |
|--------|--------|------|
| 타임스탬프 중복으로 예외 발생 | 중간 (`OPTION_CLOSEST_SYNC` 키프레임 반복 반환 시) | 중복 프레임 건너뛰기 로직 선행 구현 |
| 추적 실패 시 이전 포즈 고착 | 낮음 | 가시성 임계값(0.55) 유지로 필터링 |
| 처음 1~3프레임은 추적 미확립 | 높음 | 초반 프레임 드롭 또는 warmup 처리 |

### EMA 스무딩 관련

| 리스크 | 가능성 | 대응 |
|--------|--------|------|
| α 과소 시 슬로우모션에서 지연 느낌 | 중간 | α를 재생 속도에 따라 조정 가능하도록 파라미터화 |
| seek 후 이전 상태 잔존 | 높음 | seek 이벤트 감지 시 `reset()` 필수 |

### MediaCodec 디코더 (Phase 3)

| 리스크 | 가능성 | 대응 |
|--------|--------|------|
| 기기별 코덱 지원 차이 | 높음 | 폴백으로 `MediaMetadataRetriever` 유지 |
| 메모리 사용량 증가 | 중간 | Surface 기반 디코딩으로 Bitmap 복사 최소화 |

---

## 검증 체크리스트

### 분석 정확도

- [ ] 동일 영상 재분석 시 점수 편차 ±5점 이내
- [ ] 전문 러너 무릎 각도 130~150° 범위에서 60점 이상
- [ ] `videoFramesJson`에서 중복 타임스탬프 없음
- [ ] `RunningMode.VIDEO` 전환 후 LogCat에 MediaPipe 예외 없음

### 오버레이 시각 품질

- [ ] 1x 속도 재생 시 스켈레톤이 영상 내 실제 관절 위치를 ±15px 이내로 추적
- [ ] 0.25x 슬로우모션에서 관절 이동이 끊김 없이 부드러움
- [ ] seek 후 스켈레톤이 즉시 올바른 위치로 점프 (EMA 리셋 확인)
- [ ] visibility < 0.5인 관절이 오버레이에서 흔들리거나 점프하지 않음

### 성능

- [ ] 15초 영상 분석 시간 10초 이내 (현재 대비 동등 또는 개선)
- [ ] 재생 중 프레임 탐색이 `systrace`에서 1ms 이내
- [ ] 스켈레톤 Canvas 드로잉이 composition 기준 16ms(60fps) 내 완료

### 회귀

- [ ] 분석 이력 삭제 시 영상 파일도 함께 삭제됨
- [ ] 카메라 권한 없을 때 캡처 화면이 정상 처리됨
- [ ] 영상에서 포즈가 감지되지 않을 때 오류 메시지 정상 표시
- [ ] DB version 2 마이그레이션 후 앱이 크래시 없이 실행됨

---

## 우선순위 요약

| 순위 | 작업 | 임팩트 | 리스크 | 권장 이유 |
|------|------|--------|--------|-----------|
| **1** | RunningMode.VIDEO 전환 | 매우 높음 | 낮음 | 3줄 변경으로 추적 안정성 대폭 향상 |
| **2** | 중복 프레임 건너뛰기 | 높음 | 낮음 | VIDEO 모드 타임스탬프 예외 방지에 필수 |
| **3** | Binary search 프레임 탐색 | 중간 | 낮음 | 코드 품질 + O(n)→O(log n), 리스크 없음 |
| **4** | EMA 스무딩 + 점프 클리핑 | 중간 | 낮음 | 시각 품질 향상, 파라미터 튜닝 필요 |
| **5** | MediaCodec 디코더 | 높음 | 높음 | Phase 1~2로 충분히 개선되면 생략 가능 |
| **6** | 실시간 분석 | 매우 높음 | 매우 높음 | 기기 호환성 검증 후 장기 검토 |

> **첫 PR 권장 범위**: Phase 1 전체 (`PosturePoseAnalyzer.kt`만 변경).  
> 단일 파일 변경으로 분석 정확도와 VIDEO 모드 안정성을 동시에 확보한다.
