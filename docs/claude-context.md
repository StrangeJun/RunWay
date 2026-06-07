# RunWay — Claude Session Context

Last updated: 2026-06-06

---

## 1. Current Goal

RunWay is a GPS-based running course sharing and competition app. The broader project goal is:
`run → GPS record → course create → course discover → course attempt → leaderboard`

The current focus is **on-device running posture analysis** (Phase P-1 → P-2).

The app uses **MediaPipe Pose Landmarker** (`pose_landmarker_lite.task`, via `tasks-vision:0.10.35`) to extract 33 body landmarks from recorded video frames. The analysis runs **fully on-device** — no cloud inference. A rule-based engine then scores posture across 7 categories (knee flexion, trunk lean, elbow angle, hip extension, overstride, cadence, vertical oscillation).

Phase P-2 goal: replace the rule engine with an on-device TFLite Autoencoder trained on good-posture video landmarks. Training is offline; only inference runs on device.

---

## 2. Current Problem

**Left/right lower-body landmark swap during leg crossing.**

When a runner is filmed from the side and one leg crosses in front of the other, MediaPipe sometimes assigns the `rightKnee` / `rightAnkle` landmark ID to the **left** leg (and vice versa). This is a MediaPipe-level identity error, not a smoothing artifact.

Observed failure pattern:
- Tracking follows the right ankle correctly.
- At the moment the left leg swings forward (gait crossing), the right-ankle landmark jumps to the left leg.
- The tracked point follows the wrong leg until the next stable detection.

Consequences:
- Knee flexion angle is calculated from the wrong leg → wrong score.
- Overstride measurement uses the wrong ankle → wrong stride length.
- `RunningFormStrikeDetector` detects strikes on the wrong foot.
- All posture feedback may be based on the wrong limb.

The current `PostureSkeletonSmoother` (One Euro Filter) and the `PostureLandmarkCorrector` (knee X-offset) reduce jitter and fix visual knee offset, but do **not** solve identity switching. A direction-gate approach was also attempted (`movesAgainstBackwardStride`) but was insufficient because velocity is too weak at crossing onset.

---

## 3. Desired Pose Analysis Pipeline

```
Raw MediaPipe landmarks (33 points, normalized 0-1)
  → confidence validation        (reject frames below threshold)
  → left/right leg swap correction   (identity-tracking, not blind flip)
  → temporal smoothing           (One Euro Filter per landmark)
  → joint angle calculation      (after correction + smoothing)
  → posture feedback
```

**Important constraints:**
- Raw MediaPipe landmarks must **not** be used directly for final joint-angle calculation.
- A modular post-processing layer must be inserted between MediaPipe output and metric calculation.
- Analysis logic must have no UI coupling (no Compose/View imports in core classes).

---

## 4. Relevant Files Already Inspected or Modified

### Core posture pipeline (`android/app/src/main/java/com/runway/android/core/posture/`)

| File | Role |
|------|------|
| `PosturePoseAnalyzer.kt` | Entry point. Runs MediaPipe on video frames via `MediaMetadataRetriever`, calls `PostureLandmarkCorrector`, feeds results to `PostureAngleCalculator`. |
| `PostureLandmarkCorrector.kt` | Wraps `PostureSkeletonSmoother` + applies knee X-offset correction for side-profile view. Does **not** correct left/right identity swaps. |
| `PostureSkeletonSmoother.kt` | One Euro Filter per landmark (x, y separately). Freezes low-visibility landmarks. Used inside `PostureLandmarkCorrector`. |
| `PostureAngleCalculator.kt` | Picks the higher-visibility body side, computes knee/elbow/hip/trunk angles + overstride. |
| `PostureRuleEngine.kt` | Rule-based scoring engine. Evaluates median angles across frames. Implements `PostureEvaluator`. |
| `PostureFrameAngles.kt` | Data class for per-frame angle measurements (includes `isLandingFrame`). |
| `PostureVideoFrame.kt` | Data structures: `SkeletonPoint`, `PostureVideoFrame`, `KEY_LANDMARK_INDICES`, `SKEL_*` constants. |
| `PostureCategoryResult.kt` | Per-category score result (score, value, ideal range, feedback, tip). |
| `PostureResult.kt` | Aggregated result (overall score, grade, per-category results). |
| `PostureEvaluator.kt` | Interface for evaluation engines. |
| `RunningFormStrikeDetector.kt` | Foot-strike detector. Temporal filter (moving average, window=10) + local Y-minimum detection. Ported from `running-form-analyzer`. |
| `AnkleKalmanFilter.kt` | 2D Kalman filter for ankle position/velocity. Includes `isLanding()` / `isToeOff()`. Ported from GaitKeeper (MIT). |
| `PostureFrameInterpolator.kt` | Frame interpolation utility. |
| `PostureAutoencoderInference.kt` | Stub for Phase P-2 TFLite autoencoder. Not yet active. |
| `local/PostureAnalysisDao.kt` | Room DAO for `posture_analyses` table. |
| `local/PostureAnalysisEntity.kt` | Room entity for analysis records. |
| `local/PostureDatabase.kt` | Room database definition. |

### UI (`android/app/src/main/java/com/runway/android/ui/posture/`)

| File | Role |
|------|------|
| `PostureAnalysisViewModel.kt` | Orchestrates analysis flow. Calls `PosturePoseAnalyzer`, persists to Room. |
| `PostureCaptureScreen.kt` | CameraX video capture (side-view). Records MP4, passes URI to ViewModel. |
| `PostureAnalyzingScreen.kt` | Loading/progress screen shown during analysis. |
| `PostureResultScreen.kt` | Displays analysis score, grade, per-category cards. |
| `PostureHomeScreen.kt` | Entry screen showing analysis history list. |
| `PostureVideoPlayerCard.kt` | ExoPlayer + Compose Canvas overlay. Draws skeleton on video. Uses `PostureSkeletonSmoother` for smooth replay. Uses `textMeasurer.measure()` for angle panel (prevents text overlap). |

### Python test scripts (`docs/test-sample/`)

| File | Role |
|------|------|
| `visualize_tracking.py` | Generates annotated MP4 from `running_test.mov` using OpenCV + MediaPipe. Shows skeleton, angles, foot-strike events, dual-color left/right legs. |
| `create_analysis_record.py` | Ports full analysis pipeline to Python. Runs MediaPipe on Mac, evaluates posture, pushes video via `adb exec-out` / stdin pipe, inserts into Room SQLite DB via ADB. |
| `analyze_and_inject.py` | Variant of the above (may overlap). |
| `running_test.mov` | Source test video (side-view professional runner, ~15s). |
| `running_test_analyzed_gaitkeeper.mp4` | Sample output video. |

### Build files

| File | Role |
|------|------|
| `android/app/build.gradle.kts` | Main app build. MediaPipe: `tasks-vision:0.10.35`. |
| `android/build.gradle.kts` | Root build. |

---

## 5. What Has Already Been Implemented

**MediaPipe integration:**
- `RunningMode.VIDEO` with `detectForVideo()` — enables inter-frame Kalman tracking inside MediaPipe.
- `pose_landmarker_lite.task` bundled as asset.
- 13 key landmarks extracted from 33 raw landmarks (`KEY_LANDMARK_INDICES`).
- Frames sampled at configurable FPS (default 10fps, max `MAX_ANALYSIS_FRAMES`).
- Rotation handling for portrait-recorded videos.

**Posture metrics implemented:**
- Knee flexion angle (hip–knee–ankle)
- Trunk lean angle (shoulder midpoint vs hip midpoint, atan2)
- Elbow angle (shoulder–elbow–wrist)
- Hip extension angle (shoulder–hip–knee)
- Overstride ratio (leading ankle X vs hip X, normalized to body height)
- Cadence estimation (ankle Y local-max count over time × 2 × 60 / duration)
- Vertical oscillation (hip Y IQR / estimated body height)
- Landing frame detection (ankle Y below threshold and below hip Y)
- Best-side selection (picks higher-visibility side per frame)

**Filters / stabilization:**
- `PostureSkeletonSmoother`: One Euro Filter (adaptive cutoff, per-landmark x/y).
- `PostureLandmarkCorrector`: wraps smoother + knee X-offset for side profile.
- `AnkleKalmanFilter`: 2D Kalman for ankle tracking (4-state: x, y, vx, vy).
- `RunningFormStrikeDetector`: temporal moving average + Y local minimum.

**Storage / replay:**
- Room DB (`posture_database`) stores full analysis results including JSON video frames.
- `PostureVideoFrame` JSON: `{"t": long, "pts": [{"x": float, "y": float, "v": float}, ...]}`.
- `PostureVideoPlayerCard` replays with ExoPlayer + Canvas skeleton overlay + angle panel.
- Angle panel uses `textMeasurer.measure()` to prevent text overlap at any density.

**Current limitations:**
- No left/right identity swap correction — only smoothing + knee offset.
- No metric confidence scoring — low-confidence frames are included in averages.
- Far-side (occluded) landmarks have lower MediaPipe accuracy — affects right-side joints in left-facing run.
- `PostureAutoencoderInference.kt` is a stub — Phase P-2 not started.

---

## 6. What Still Needs To Be Implemented

- **Landmark confidence validation**: reject or flag frames where key landmark visibility < threshold before angle calculation.
- **Left/right leg swap correction**: tracking-by-detection, not blind flip (see Section 8).
- **Temporal smoothing** (post-swap-correction): One Euro Filter or Kalman, applied after identity is stabilized.
- **Metric confidence scoring**: each angle result should carry a confidence value; skip low-confidence frames from scoring.
- **Safer joint angle calculation**: `PostureAngleCalculator` should receive corrected + validated landmarks only.
- **Debug visualization**: overlay showing raw vs corrected vs smoothed landmarks; identity-switch events logged.
- **Tests**: unit tests for normal, swapped, low-confidence, and sudden-jump landmark sequences.
- **Build/test verification** after each change: `./gradlew :app:compileDebugKotlin`.

---

## 7. Important Design Decisions

- **Do not replace MediaPipe.** Add a post-processing layer after MediaPipe output.
- **Keep analysis logic modular and UI-free.** No Compose/View imports in core classes.
- **Prefer small, single-responsibility classes:**
  - `PoseFrame` — raw per-frame data wrapper
  - `PoseLandmarkMapper` — maps MediaPipe 33-landmark output to 13-key format
  - `LandmarkConfidenceValidator` — per-frame confidence check
  - `LegSwapCorrector` — left/right identity correction
  - `LandmarkSmoother` — temporal filter (One Euro or Kalman)
  - `JointAngleCalculator` — angle math only (no MediaPipe dependency)
  - `PoseAnalysisResult` — structured result with per-metric confidence
- **Do not calculate final posture feedback from low-confidence frames.** Return "insufficient landmark confidence" instead of wrong feedback.
- **Avoid over-correcting.** Normal frames must not be changed by the swap corrector.
- **Hysteresis for identity switching**: allow a track identity change only when the cost improvement is persistent over multiple frames.

---

## 8. Landmark Swap Correction Strategy

Treat the problem as **tracking-by-detection / data-association**, not smoothing.

**Core algorithm (cost-based assignment):**

For each new frame, compute two competing assignment costs using the previous valid frame's landmarks:

```
keepCost =
    distance(currentRightKnee,  previousRightKnee)
  + distance(currentRightAnkle, previousRightAnkle)
  + distance(currentLeftKnee,   previousLeftKnee)
  + distance(currentLeftAnkle,  previousLeftAnkle)

swapCost =
    distance(currentRightKnee,  previousLeftKnee)
  + distance(currentRightAnkle, previousLeftAnkle)
  + distance(currentLeftKnee,   previousRightKnee)
  + distance(currentLeftAnkle,  previousRightAnkle)
```

If `swapCost + HYSTERESIS_MARGIN < keepCost` **and** this persists for `N_CONFIRM_FRAMES` consecutive frames, treat the frame as swapped and swap lower-body landmarks back.

**Additional signals to incorporate:**
- Sudden unrealistic bone-length changes (thigh or lower-leg length jump)
- Sudden knee/ankle position jump exceeding plausible inter-frame distance
- Hip–knee–ankle anatomical consistency (knee should lie between hip and ankle in Y)
- Foot trajectory continuity (stance foot moves backward; swing foot moves forward)
- Visibility/presence confidence per landmark

**Safety rules:**
- Do not over-correct on one weak signal.
- Use `HYSTERESIS_MARGIN` (e.g. 0.20 in normalized coords) and `N_CONFIRM_FRAMES` (e.g. 3) before switching identity.
- Normal frames where `keepCost ≤ swapCost` must be returned unchanged.
- If confidence is too low to determine cost reliably, hold previous identity.

**Simpler practical version (two persistent leg tracks):**

Maintain `trackA` and `trackB`, each storing: knee/ankle position, velocity, visibility, missed count.
Each frame, compute cost for both possible assignments (left→A/right→B vs right→A/left→B).
Assign to lower cost only if improvement exceeds hysteresis margin for N consecutive frames.

```
allow identity switch only when:
  swappedCost + 0.20 < sameCost
  AND improvement persists for ≥ 3 consecutive frames
  AND neither tracked foot is in a backward stance phase
```

---

## 9. Temporal Smoothing Strategy

Apply temporal smoothing **after** landmark confidence validation and swap correction.

**Preferred filter order:**
1. **One Euro Filter** (already implemented in `PostureSkeletonSmoother`) — best for interactive/real-time.
2. **Kalman Filter** (already implemented in `AnkleKalmanFilter`) — better for velocity-based prediction.
3. **Exponential Moving Average** — fallback if other filters are unavailable.

**Minimum landmarks to smooth:**
- `leftKnee`, `leftAnkle`, `leftHeel`, `leftFootIndex`
- `rightKnee`, `rightAnkle`, `rightHeel`, `rightFootIndex`

**Avoid excessive smoothing.** High smoothing delays foot-strike timing. `RunningFormStrikeDetector` depends on accurate ankle Y timing; over-smoothing shifts the local minimum and causes missed or duplicate strikes.

---

## 10. External References and How To Use Them

### Sports2D
- **Use for:** filtering (One Euro, Butterworth), interpolation, outlier rejection, joint angle calculation methodology, confidence-based visualization, debug visualization structure.
- **Note:** Sports2D does not fully solve left/right swap correction. Use it mainly for the processing pipeline and visualization approach.

### GaitKeeper (`chrismarth/GaitKeeper`)
- **Use for:** MediaPipe BlazePose-based running metrics, ankle tracking with Kalman filter, foot-strike detection, cadence, stride-related metrics, metric confidence scoring.
- **License: MIT.** If any code or substantial implementation logic is reused or adapted, preserve the original copyright notice and include the MIT License text in `THIRD_PARTY_LICENSES.md` or `NOTICE`.
- `AnkleKalmanFilter.kt` is already ported from GaitKeeper — attribution comment is present in the file.

### running-form-analyzer (`henryczup/running-form-analyzer`)
- **Use for:** running-specific posture metrics (trunk angle, knee angle, arm swing), side-view handling, visual/audio feedback structure, feedback generation.
- `RunningFormStrikeDetector.kt` is ported from this project (temporal filter + local Y-minimum detection).

### myogait
- **Use for:** lateral label correction concepts — specifically the idea behind `correct_lateral_labels()`.
- **Do not copy blindly.** Adapt the concept to this project's Android/Kotlin architecture.

---

## 11. License / Attribution Notes

- **GaitKeeper** is licensed under the MIT License.
- `AnkleKalmanFilter.kt` was ported from GaitKeeper. Attribution is in the file header.
- If additional code or substantial logic is reused from GaitKeeper, add attribution to:
  - `android/THIRD_PARTY_LICENSES.md` (create if not present)
  - or `android/NOTICE`
- Do not remove existing attribution.
- Prefer reimplementing ideas in this project's own architecture over direct code copying.

---

## 12. Known Bugs or Risks

| Issue | Severity | Status |
|-------|----------|--------|
| MediaPipe left/right landmark identity swap during leg crossing | High | Open — core unsolved problem |
| Low-confidence landmarks causing incorrect angle values | High | Partially mitigated (visibility threshold in `PostureAngleCalculator`) |
| Over-smoothing may delay foot-strike detection | Medium | Risk — monitor when adding smoothing layers |
| Over-aggressive swap correction may flip correctly detected frames | Medium | Risk — use hysteresis + multi-frame confirmation |
| Side-view vs front-view: knee angle less reliable in non-side-view | Medium | Mitigated by side-profile silhouette guide in `PostureCaptureScreen` |
| Far-side landmark inaccuracy (occluded leg in side view) | Medium | Partially mitigated by best-side selection in `PostureAngleCalculator` |
| Performance on Android low-end devices (MediaPipe inference + analysis loop) | Medium | Not yet profiled |
| `PostureAutoencoderInference.kt` is a stub — Phase P-2 not started | Low | Known, by design |

---

## 13. Commands Used For Build / Test

```bash
# Compile Kotlin only (fast check)
cd android
./gradlew :app:compileDebugKotlin

# Full debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Unit tests
./gradlew test

# Instrumented tests (device required)
./gradlew connectedAndroidTest

# Full build (includes lint + test)
./gradlew build
```

Python test scripts (Mac side, requires `mediapipe`, `opencv-python`):
```bash
cd /Users/jun/Developer/RunWay
python3 docs/test-sample/visualize_tracking.py
python3 docs/test-sample/create_analysis_record.py
```

---

## 14. Next Recommended Steps

1. Read `docs/2026-06-06-posture-tracking-handoff.md` — it has the most recent implementation state and failed approaches.
2. Inspect `PostureSkeletonSmoother.kt` and `PostureLandmarkCorrector.kt` to understand the current stabilization approach.
3. Inspect `PosturePoseAnalyzer.kt` to find where to insert the new post-processing layer.
4. Design and implement `LandmarkConfidenceValidator` — reject frames below per-landmark visibility threshold.
5. Design and implement `LegSwapCorrector` — two-track cost-based assignment (see Section 8).
6. Implement `LandmarkSmoother` (thin wrapper over existing `PostureSkeletonSmoother`, applied after correction).
7. Update `PostureAngleCalculator` to receive corrected + validated landmarks.
8. Add debug logging / visualization: log swap events with frame index, cost values, and landmark positions.
9. Write unit tests for: normal frames (no change), swapped frames (swap applied), low-confidence (rejected), sudden jump (absorbed by filter).
10. Run `./gradlew :app:compileDebugKotlin` and `./gradlew assembleDebug` to verify build.
11. Generate new annotated video from `docs/test-sample/running_test.mov` and inspect the crossing phase visually.

---

## 15. Oracle VM / Deployment Context

The backend (Spring Boot) is deployed to an **Oracle Cloud (OCI) VM** running Ubuntu.

### CI/CD
- GitHub Actions workflow: `.github/workflows/deploy-backend.yml`
- Trigger: push to `main` with changes in `backend/**`
- Steps: build JAR → SCP to server → `systemctl restart runway-api`
- Secrets stored in GitHub Actions: `SERVER_HOST`, `SSH_PRIVATE_KEY`

### SSH Connection

```bash
# SSH format
ssh -i <path-to-private-key> ubuntu@<vm-public-ip>

# Example
ssh -i ~/.ssh/<oracle-key-name>.key ubuntu@<vm-public-ip>
```

- **Username:** `ubuntu`
- **VM public IP:** stored as `SERVER_HOST` GitHub secret (not stored in this file)
- **SSH private key:** stored as `SSH_PRIVATE_KEY` GitHub secret. Local key path: check `~/.ssh/` for the corresponding key file.

### Server Paths

```
/opt/runway/app/runway-api.jar   ← deployed JAR
```

### Service Management

```bash
# Check status
sudo systemctl status runway-api

# Restart
sudo systemctl restart runway-api

# View logs
sudo journalctl -u runway-api -n 50 --no-pager
tail -f /opt/runway/app/app.log    # if nohup redirect is used
```

### Port

- Backend runs on **port 8080** (default Spring Boot).
- If port 8080 is already in use before restart:

```bash
# Find the process
lsof -i :8080
ps aux | grep java

# Kill it
kill -9 <PID>
```

### Environment Variables (names only — no secret values)

The production profile (`application-prod.yml`) expects these to be set as environment variables or via systemd `EnvironmentFile`:

```
JWT_SECRET=<do not store value here>
SPRING_DATASOURCE_URL=<do not store value here>
SPRING_DATASOURCE_USERNAME=<do not store value here>
SPRING_DATASOURCE_PASSWORD=<do not store value here>
```

### Manual Restart (if systemd is unavailable)

```bash
cd /opt/runway/app
nohup java -jar runway-api.jar --spring.profiles.active=prod > app.log 2>&1 &
```

### Common Troubleshooting

```bash
# Check if service is running
sudo systemctl is-active runway-api

# Check recent logs
sudo journalctl -u runway-api -n 30 --no-pager

# Check port
lsof -i :8080

# Check Java processes
ps aux | grep java

# Rebuild and redeploy manually (from backend dir on VM or local)
./gradlew bootJar -x test
# then SCP jar to /opt/runway/app/ and systemctl restart
```

**Do not write actual secret values (DB password, JWT secret, access tokens) into this file.**

---

## 16. Fresh Session Restart Prompt

Copy and paste this into a new Claude session:

```
Read CLAUDE.md and docs/claude-context.md first.

Then inspect the current codebase and continue the running posture analysis task from the saved context.

Do not assume prior chat context exists.

Focus on improving MediaPipe Pose Landmarker stability for running analysis, especially the left/right lower-body landmark swap issue when legs overlap.

First, summarize:
1. What the current goal is
2. Which files are relevant
3. What you plan to change

Then proceed with the smallest safe implementation step.
```
