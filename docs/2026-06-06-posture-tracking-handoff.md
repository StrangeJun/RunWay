# Posture Tracking Handoff - 2026-06-06

This document summarizes the current posture-analysis tracking work, what was changed, what still fails, and what should be handed to Claude or another engineer for the next attempt.

## Current User-Reported Problem

The running posture overlay still has unstable right-leg tracking around the leg-crossing phase.

Observed failure:

- The target should keep following the right ankle/right leg.
- At the exact middle crossing point, when the right foot should move backward after ground contact, the left leg comes forward.
- The tracker then tends to attach the right-side point to the left leg candidate.
- The user specifically described: while following the right foot, as soon as the left foot comes forward, the point jumps to the left leg.
- Knee and ankle points also appear visually slightly ahead of the real knee/ankle position.

In short: this is not just jitter. It is a left/right identity-switch problem during side-view gait crossing.

## Repository State

Working directory:

```text
/Users/jun/Developer/RunWay/android
```

Current branch at the time of this handoff:

```text
main...origin/main
```

Uncommitted files related to this task:

```text
app/src/main/java/com/runway/android/core/posture/PostureSkeletonSmoother.kt
app/src/main/java/com/runway/android/ui/posture/PostureVideoPlayerCard.kt
```

Unrelated untracked file:

```text
design/physical_test-Photoroom.png
```

Do not assume the posture-tracking changes are committed. Check `git status` first.

## Files Changed

### `PostureSkeletonSmoother.kt`

Path:

```text
android/app/src/main/java/com/runway/android/core/posture/PostureSkeletonSmoother.kt
```

Intent:

- Add stronger stabilization for knees and ankles than other joints.
- Hold previous knee/ankle when visibility is low.
- Clamp large frame-to-frame jumps.
- Reject structurally implausible knee/ankle candidates using hip-knee-ankle geometry.

Important additions:

- `kneeAlpha = 0.3f`
- `kneeMaxDelta = 0.08f`
- `kneeLowVisThreshold = 0.65f`
- `ankleAlpha = 0.55f`
- `ankleMaxDelta = 0.18f`
- `ankleLowVisThreshold = 0.62f`
- `stabilizeLegCandidates`
- `stabilizeKnee`
- `stabilizeAnkle`
- `isKneeStructurallyPlausible`
- `isAnkleStructurallyPlausible`

What was removed:

- An earlier lower-leg swap function was removed because it could make identity switches worse. The current approach should not blindly swap left/right lower legs.

Known limitation:

- This smoother is landmark-local and frame-local. It reduces jitter but cannot reliably solve leg identity switches when MediaPipe itself outputs right ankle as the forward left-leg point during crossing.

### `PostureVideoPlayerCard.kt`

Path:

```text
android/app/src/main/java/com/runway/android/ui/posture/PostureVideoPlayerCard.kt
```

Intent:

- Use a `LockedLegTracker` to keep one selected visible side stable during replay.
- Draw both legs in different colors so identity switches are visible.
- Apply display-only x correction to knee/ankle points because the overlay appeared slightly ahead of the real joints.

Important changes:

- `preferredVisibleSide()` chooses the side with higher average visibility over the analyzed frames.
- `LockedLegTracker(trackLeftSide = preferredSide == VisibleSide.Left)` is remembered per video.
- On each current frame:

```kotlin
val smoothed = smoother.smooth(it.pts)
it.copy(pts = legTracker.track(smoothed))
```

- On seek/replay, both `smoother.reset()` and `legTracker.reset()` are called.

Overlay color convention:

- Left leg: orange `Color(0xFFFFB020)`
- Right leg: green `Color(0xFF00E676)`
- Labels: `L` and `R` near ankle points
- Highlighted side is drawn thicker.

Display-only correction:

```kotlin
val backwards = if (facingRight) -1f else 1f
val correctionX = when (index) {
    SKEL_L_KNEE, SKEL_R_KNEE -> 0.026f * backwards
    SKEL_L_ANKLE, SKEL_R_ANKLE -> 0.038f * backwards
    else -> 0f
}
```

This only changes where the overlay is drawn. It does not change scoring or saved posture frame data.

## Current Tracker Attempt

`LockedLegTracker` keeps:

- `previousKnee`
- `previousAnkle`
- `kneeVelocityX/Y`
- `ankleVelocityX/Y`

It predicts the next target knee/ankle from previous position plus velocity.

Current candidate selection:

- Only checks the MediaPipe target-side candidate, not both legs.
- If the target-side candidate is rejected, it falls back to the predicted position.

Candidate validity currently checks:

- knee visibility >= `0.58`
- ankle visibility >= `0.58`
- knee distance to predicted knee <= `0.34`
- ankle distance to predicted ankle <= `0.46`
- lower leg length <= thigh length * `2.35`
- direction gate for backward stride

Direction gate logic:

```kotlin
private fun movesAgainstBackwardStride(
    current: SkeletonPoint,
    previous: SkeletonPoint,
    predicted: SkeletonPoint,
    velocityX: Float,
    facingRight: Boolean,
): Boolean {
    val backwardSign = if (facingRight) -1f else 1f
    val velocityBackward = velocityX * backwardSign
    val predictedBackward = (predicted.x - previous.x) * backwardSign
    val currentBackward = (current.x - previous.x) * backwardSign

    return velocityBackward > 0.015f &&
        predictedBackward > 0f &&
        currentBackward < -0.07f
}
```

Why this was attempted:

- When the runner faces right, a right foot moving backward should move left on screen.
- If the tracker predicts backward motion but the new candidate jumps forward, it is probably the opposite leg.

Why this is still insufficient:

- It only uses x-direction and previous velocity.
- During gait crossing, velocity may not yet be strong enough to trigger the gate.
- MediaPipe's right/left landmark identity can already be wrong before the gate sees a large enough jump.
- The fallback predicted point can freeze or lag if detections remain rejected.
- This does not model gait phase, occlusion, or physical foot contact timing.

## Generated Sample Videos

Source video:

```text
docs/test-sample/running_test.mov
```

Latest generated sample:

```text
docs/test-sample/running_test_analyzed_direction_gate.mp4
docs/test-sample/running_test_analyzed_direction_gate_preview.jpg
```

The latest sample uses:

- MediaPipe pose landmarker lite model
- right-leg lock
- direction gate
- right leg in green
- left leg in orange
- labels near ankle points
- display x correction for knee/ankle

Earlier generated videos may exist in the same folder, depending on previous attempts:

```text
running_test_analyzed.mp4
running_test_analyzed_stable.mp4
running_test_analyzed_single_leg.mp4
running_test_analyzed_locked_leg.mp4
running_test_analyzed_tracked_leg.mp4
running_test_analyzed_strict_right.mp4
running_test_analyzed_fast_right.mp4
running_test_analyzed_dual_color.mp4
```

Note: Some earlier files may not be present if the folder was cleaned. Always inspect `docs/test-sample`.

## Build Verification

The Android Kotlin compile passed after the latest code changes:

```bash
./gradlew :app:compileDebugKotlin
```

Result:

```text
BUILD SUCCESSFUL
```

No full app test suite or visual Android device test was completed after the latest tracker changes.

## What Not To Repeat

Avoid relying only on:

- EMA smoothing
- Visibility thresholds
- Single-frame plausibility checks
- Blind left/right swapping
- A simple x-direction gate

These reduce noise but do not fully solve identity switching during side-view leg crossing.

## Recommended Next Approach

Treat this as a tracking-by-detection/data-association problem, not a smoothing problem.

Recommended implementation direction:

1. Track both legs as two persistent tracks, not just "MediaPipe left" and "MediaPipe right".
2. For every frame, build two observed leg candidates from MediaPipe:
   - left hip/knee/ankle
   - right hip/knee/ankle
3. Assign observations to persistent tracks using a cost function, rather than trusting MediaPipe's left/right label.
4. Cost function should include:
   - distance to predicted knee/ankle
   - velocity consistency
   - lower-leg length consistency
   - hip side consistency
   - foot y/ground-contact phase
   - visibility confidence
   - penalty for switching identity unless cost improvement is very large
5. Use a Kalman filter or alpha-beta filter per keypoint/track.
6. Add an occlusion state:
   - If a candidate is implausible during crossing, continue prediction for a short window instead of switching.
   - Allow reacquisition only after the candidate matches predicted location, velocity, and body geometry.
7. Consider gait phase:
   - Stance/backward phase and swing/forward phase should have different expected motion.
   - Foot near ground should not suddenly become the forward swinging foot.
8. If only one side is needed for scoring, choose the near-side track once at the beginning and keep that persistent track ID.

Simpler practical version:

- Maintain two `LegTrack`s: `trackA`, `trackB`.
- Each has knee/ankle state, velocity, last visibility, and missed count.
- For each frame, compute both possible assignments:
  - observed left -> trackA, observed right -> trackB
  - observed right -> trackA, observed left -> trackB
- Pick the lower total cost only if it beats the current identity by a strong hysteresis margin.
- Otherwise keep the previous identity assignment.

Example hysteresis:

```text
allow identity switch only when:
swappedCost + 0.20 < sameCost
and the improvement persists for 3 consecutive frames
and neither tracked foot is in a backward stance phase
```

## Suggested Claude Prompt

Use this prompt for the next implementation attempt:

```text
You are working in /Users/jun/Developer/RunWay/android on the RunWay Android app.

The posture analysis overlay has a right/left leg identity-switch problem during side-view running gait crossing. The user wants the right leg, especially the right ankle, to remain tracked. Current smoothing and a simple direction gate are not enough: when the right foot should move backward after contact, the left leg comes forward and the right ankle point jumps to the left leg.

Please inspect:
- app/src/main/java/com/runway/android/core/posture/PostureSkeletonSmoother.kt
- app/src/main/java/com/runway/android/ui/posture/PostureVideoPlayerCard.kt
- docs/2026-06-06-posture-tracking-handoff.md
- docs/test-sample/running_test.mov
- docs/test-sample/running_test_analyzed_direction_gate.mp4

Do not solve this with only EMA smoothing or a blind left/right swap. Treat it as a tracking-by-detection problem.

Implement persistent two-leg tracking:
1. Maintain two persistent leg tracks with knee/ankle position, velocity, visibility, missed count, and stable identity.
2. For each frame, create observed left-leg and right-leg candidates from MediaPipe output.
3. Assign observations to tracks using a cost function with distance-to-prediction, velocity consistency, limb length consistency, visibility, hip consistency, and switching penalty.
4. Add hysteresis so track identity can switch only after a strong, persistent cost advantage for multiple frames.
5. Add occlusion handling so a track can keep predicting briefly through crossing instead of attaching to the other leg.
6. Keep the near-side/right-side selected track stable for overlay/scoring.
7. Preserve the current dual-color overlay or improve it so left/right confusion is visually obvious.
8. Run ./gradlew :app:compileDebugKotlin.
9. Generate a new sample overlay video from docs/test-sample/running_test.mov so the crossing phase can be inspected.

The latest generated sample is:
docs/test-sample/running_test_analyzed_direction_gate.mp4

The desired result is that the right ankle does not jump to the left leg when the left leg swings forward during the crossing phase.
```

## Final Notes

The current code compiles, but the user is not satisfied with the visual tracking result. The next engineer should prioritize robust identity persistence over additional smoothing. The current direction-gate implementation can be used as a reference, but it should probably be replaced by a full two-track assignment system.
