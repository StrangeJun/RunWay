"""
analyze_and_inject.py
---------------------
1. Analyze running_test.mov with MediaPipe (same settings as Android app)
2. Compute PostureResult using the same scoring logic as PostureRuleEngine.kt
3. Push the video to the device and insert the analysis into Room DB
"""

import cv2
import json
import math
import os
import shutil
import sqlite3
import subprocess
import sys
import time
import uuid
from dataclasses import dataclass, field
from typing import Optional

import mediapipe as mp
from mediapipe.tasks import python as mp_python
from mediapipe.tasks.python import vision as mp_vision

# ──────────────────────────────────────────────────────────────
# Config
# ──────────────────────────────────────────────────────────────
SCRIPT_DIR   = os.path.dirname(os.path.abspath(__file__))
VIDEO_PATH   = os.path.join(SCRIPT_DIR, "running_test.mov")
MODEL_PATH   = os.path.join(
    SCRIPT_DIR, "../../android/app/src/main/assets/pose_landmarker_lite.task"
)
PACKAGE      = "com.runway.android"
DB_NAME      = "posture_database"
DB_REMOTE    = f"/data/data/{PACKAGE}/databases/{DB_NAME}"
VIDEO_REMOTE_DIR = f"/data/data/{PACKAGE}/files/posture"
TARGET_FPS   = 10
VIS_THRESHOLD = 0.55   # bestVis threshold in PostureAngleCalculator

# MediaPipe landmark indices (same as KEY_LANDMARK_INDICES in Android)
KEY_INDICES = [0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28]
# names for reference: nose, L_shoulder, R_shoulder, L_elbow, R_elbow,
#                      L_wrist, R_wrist, L_hip, R_hip, L_knee, R_knee, L_ankle, R_ankle

# MediaPipe named landmark indices
NOSE=0; L_SH=11; R_SH=12; L_EL=13; R_EL=14; L_WR=15; R_WR=16
L_HIP=23; R_HIP=24; L_KN=25; R_KN=26; L_AN=27; R_AN=28


# ──────────────────────────────────────────────────────────────
# Geometry helpers (mirror of PostureAngleCalculator.kt)
# ──────────────────────────────────────────────────────────────
def three_point_angle(a, b, c):
    ax, ay = a.x - b.x, a.y - b.y
    cx, cy = c.x - b.x, c.y - b.y
    dot  = ax * cx + ay * cy
    magA = math.sqrt(ax*ax + ay*ay)
    magC = math.sqrt(cx*cx + cy*cy)
    if magA < 1e-6 or magC < 1e-6:
        return 180.0
    cos_angle = max(-1.0, min(1.0, dot / (magA * magC)))
    return math.degrees(math.acos(cos_angle))

def trunk_lean_angle(ls, rs, lh, rh):
    shoulder_mid_x = (ls.x + rs.x) / 2
    shoulder_mid_y = (ls.y + rs.y) / 2
    hip_mid_x      = (lh.x + rh.x) / 2
    hip_mid_y      = (lh.y + rh.y) / 2
    dx = shoulder_mid_x - hip_mid_x
    dy = hip_mid_y - shoulder_mid_y
    return math.degrees(math.atan2(dx, dy))

def avg_vis(landmarks, *indices):
    return sum(landmarks[i].visibility for i in indices) / len(indices)


# ──────────────────────────────────────────────────────────────
# PostureRuleEngine port (mirrors PostureRuleEngine.kt exactly)
# ──────────────────────────────────────────────────────────────
@dataclass
class CategoryResult:
    score: int
    measured_value: float
    ideal_min: float
    ideal_max: float
    unit: str
    feedback: str
    tip: str

@dataclass
class PostureResult:
    overall_score: int
    grade: str
    overall_feedback: str
    knee: CategoryResult
    trunk: CategoryResult
    elbow: CategoryResult
    hip: CategoryResult
    overstride: CategoryResult
    cadence: CategoryResult
    vertical_osc: CategoryResult

def angle_score(value, ideal_min, ideal_max):
    if ideal_min <= value <= ideal_max:
        return 100
    deviation = (ideal_min - value) if value < ideal_min else (value - ideal_max)
    if deviation <= 5:
        return int(90 - (deviation / 5) * 20)
    elif deviation <= 15:
        return int(70 - ((deviation - 5) / 10) * 30)
    else:
        return max(0, int(40 - ((deviation - 15) / 15) * 40))

def median(lst):
    if not lst: return 0.0
    s = sorted(lst)
    n = len(s)
    return (s[n//2 - 1] + s[n//2]) / 2.0 if n % 2 == 0 else s[n//2]

def grade_from(score):
    if score >= 90: return "S"
    if score >= 75: return "A"
    if score >= 60: return "B"
    if score >= 45: return "C"
    return "D"

def eval_knee(angle):
    ideal_min, ideal_max = 135.0, 165.0
    score = angle_score(angle, ideal_min, ideal_max)
    if   angle < ideal_min - 15: fb = "착지 시 무릎이 너무 많이 구부러져 있습니다. 보폭을 조금 줄여보세요."
    elif angle < ideal_min:      fb = "착지 시 무릎이 약간 많이 구부러져 있습니다. 조금 더 펴보세요."
    elif angle > ideal_max + 15: fb = "착지 시 무릎이 너무 펴져 있어 충격 흡수가 부족합니다."
    elif angle > ideal_max:      fb = "착지 시 무릎을 살짝 더 구부려 충격을 흡수해보세요."
    else:                        fb = "착지 시 무릎 각도가 이상적입니다."
    tip = "무릎을 살짝 구부린 상태(135~165°)로 착지하면 관절 충격을 효과적으로 분산시킬 수 있습니다." if score < 80 else ""
    return CategoryResult(score, angle, ideal_min, ideal_max, "°", fb, tip)

def eval_trunk(angle):
    ideal_min, ideal_max = 5.0, 10.0
    score = angle_score(angle, ideal_min, ideal_max)
    if   angle < 0:              fb = "상체가 뒤로 기울어져 있습니다. 전방으로 기울여 보세요."
    elif angle < ideal_min:      fb = "상체 기울기가 부족합니다. 약 5~10도 앞으로 기울여 보세요."
    elif angle > ideal_max + 10: fb = "상체가 너무 앞으로 기울어져 있어 허리에 부담이 됩니다."
    elif angle > ideal_max:      fb = "상체 기울기가 조금 과합니다."
    else:                        fb = "상체 기울기가 이상적입니다."
    tip = "전방 기울기는 추진력과 효율을 높여줍니다." if score < 80 else ""
    return CategoryResult(score, angle, ideal_min, ideal_max, "°", fb, tip)

def eval_elbow(angle):
    ideal_min, ideal_max = 85.0, 95.0
    score = angle_score(angle, ideal_min, ideal_max)
    if   angle < ideal_min - 15: fb = "팔꿈치가 너무 많이 구부러져 있어 경직됩니다."
    elif angle < ideal_min:      fb = "팔꿈치를 약 90도로 유지해보세요."
    elif angle > ideal_max + 25: fb = "팔꿈치가 너무 펴져 있어 에너지 손실이 발생합니다."
    elif angle > ideal_max:      fb = "팔꿈치를 약 90도로 줄여보세요."
    else:                        fb = "팔꿈치 각도가 이상적입니다."
    tip = "팔꿈치를 90도 유지하면 리듬감 있는 팔 스윙이 가능합니다." if score < 80 else ""
    return CategoryResult(score, angle, ideal_min, ideal_max, "°", fb, tip)

def eval_hip(angle):
    ideal_min, ideal_max = 160.0, 180.0
    score = angle_score(angle, ideal_min, ideal_max)
    if   angle < ideal_min - 15: fb = "고관절 신전이 크게 부족합니다. 뒤 발 차기를 강화해보세요."
    elif angle < ideal_min:      fb = "push-off 시 고관절을 조금 더 신전시켜 보세요."
    else:                        fb = "고관절 신전이 적절합니다."
    tip = "완전한 고관절 신전은 추진력을 높여줍니다." if score < 80 else ""
    return CategoryResult(score, angle, ideal_min, ideal_max, "°", fb, tip)

def eval_overstride(ratio):
    if   ratio <= 0.10: score = 100
    elif ratio <= 0.20: score = max(0, int(100 - ((ratio - 0.10) / 0.10) * 40))
    elif ratio <= 0.30: score = max(0, int(60  - ((ratio - 0.20) / 0.10) * 30))
    else:               score = max(0, int(30  - ((ratio - 0.30) / 0.10) * 30))
    if   ratio <= 0.10: fb = "착지 위치가 이상적입니다."
    elif ratio <= 0.20: fb = "착지 위치가 약간 앞쪽입니다. 보폭을 조금 줄여보세요."
    elif ratio <= 0.30: fb = "오버스트라이드가 감지됩니다. 보폭을 줄이고 케이던스를 높여보세요."
    else:               fb = "착지 위치가 몸 앞쪽으로 많이 나와 있습니다. 충격과 부상 위험이 높습니다."
    tip = "발이 엉덩이 아래에 가깝게 착지하면 제동력을 줄일 수 있습니다." if score < 80 else ""
    return CategoryResult(score, ratio, 0.0, 0.10, "%", fb, tip)

def build_overall_feedback(score, knee, trunk, overstride):
    weakest = min([("무릎 굴곡", knee.score), ("상체 기울기", trunk.score),
                   ("오버스트라이드", overstride.score)], key=lambda x: x[1])
    if score >= 90: return "런닝 자세가 매우 훌륭합니다. 현재 자세를 유지하세요!"
    if score >= 75: return f"전반적으로 좋은 자세입니다. {weakest[0]} 부분을 보완하면 더 좋아집니다."
    if score >= 60: return f"{weakest[0]} 개선이 필요합니다. 꾸준히 연습해 보세요."
    return "자세 개선이 필요합니다. 각 항목의 피드백을 참고해 연습해 보세요."

def eval_cadence(frames_with_timing):
    """frames_with_timing: list of dicts with 'near_ankle_y' and 'time_ms' keys."""
    ideal_min, ideal_max = 170.0, 180.0
    no_data = CategoryResult(0, 0.0, ideal_min, ideal_max, "spm",
                             "케이던스 측정을 위해 더 긴 구간이 필요합니다.", "")
    if len(frames_with_timing) < 8:
        return no_data
    duration_ms = frames_with_timing[-1]['time_ms'] - frames_with_timing[0]['time_ms']
    if duration_ms < 2000:
        return no_data

    ankle_y = [f['near_ankle_y'] for f in frames_with_timing]
    min_peak_y = 0.55
    min_gap = 5  # 500ms at 10fps → max detectable cadence ~240 spm
    step_count = 0
    last_peak = -min_gap
    for i in range(1, len(ankle_y) - 1):
        if i - last_peak < min_gap:
            continue
        if ankle_y[i] > ankle_y[i-1] and ankle_y[i] >= ankle_y[i+1] and ankle_y[i] > min_peak_y:
            step_count += 1
            last_peak = i
    if step_count < 3:
        return no_data

    spm = step_count * 2.0 * 60000.0 / duration_ms
    score = angle_score(spm, ideal_min, ideal_max)
    if   spm < 150:       fb = "케이던스가 매우 낮습니다. 보폭을 줄이고 발놀림을 빠르게 해보세요."
    elif spm < ideal_min: fb = f"케이던스 {round(spm)}spm은 낮습니다. 170spm 이상을 목표로 해보세요."
    elif spm > ideal_max + 10: fb = f"케이던스 {round(spm)}spm으로 리듬이 좋습니다."
    else:                 fb = f"케이던스 {round(spm)}spm으로 이상적입니다."
    tip = "케이던스를 높이면 오버스트라이드가 줄고 부상 위험이 낮아집니다." if score < 80 else ""
    return CategoryResult(score, spm, ideal_min, ideal_max, "spm", fb, tip)


def eval_vertical_osc(frames_with_timing):
    """Uses 'hip_mid_y' and 'near_ankle_y' fields."""
    ideal_min, ideal_max = 4.0, 8.0
    no_data = CategoryResult(0, 0.0, ideal_min, ideal_max, "%",
                             "수직진폭 측정을 위해 더 긴 구간이 필요합니다.", "")
    if len(frames_with_timing) < 8:
        return no_data

    hip_y = sorted([f['hip_mid_y'] for f in frames_with_timing])
    q25 = hip_y[int(len(hip_y) * 0.25)]
    q75 = hip_y[int(len(hip_y) * 0.75)]
    amplitude = q75 - q25

    landing = [f for f in frames_with_timing if f['is_landing']]
    leg_lengths = [f['near_ankle_y'] - f['hip_mid_y']
                   for f in (landing if landing else frames_with_timing)
                   if f['near_ankle_y'] > f['hip_mid_y']]
    leg_len = sum(leg_lengths) / len(leg_lengths) if leg_lengths else 0.25
    leg_len = max(leg_len, 0.15)

    body_height = leg_len / 0.52
    osc_pct = max(amplitude / body_height * 100.0, 0.0)

    score = angle_score(osc_pct, ideal_min, ideal_max)
    if   osc_pct < 2:       fb = "수직진폭이 매우 작습니다."
    elif osc_pct <= ideal_max: fb = "수직진폭이 이상적입니다. 에너지 효율이 좋습니다."
    elif osc_pct <= 12:     fb = "수직진폭이 약간 큽니다. 상체를 안정화해보세요."
    else:                   fb = "상하 진폭이 큽니다. 앞으로 나아가는 에너지가 낭비되고 있습니다."
    tip = "코어 강화와 자세 안정화로 불필요한 바운싱을 줄일 수 있습니다." if osc_pct > ideal_max else ""
    return CategoryResult(score, osc_pct, ideal_min, ideal_max, "%", fb, tip)


def evaluate(frame_angles_list):
    landing = [f for f in frame_angles_list if f['is_landing']]
    has_landing = bool(landing)
    landing_or_all = landing if has_landing else frame_angles_list

    knee_median    = median([f['knee'] for f in landing_or_all])
    trunk_median   = median([f['trunk'] for f in frame_angles_list])
    elbow_median   = median([f['elbow'] for f in frame_angles_list])
    hip_median     = median([f['hip'] for f in frame_angles_list])

    knee     = eval_knee(knee_median)
    trunk    = eval_trunk(trunk_median)
    elbow    = eval_elbow(elbow_median)
    hip      = eval_hip(hip_median)

    if has_landing:
        overstride = eval_overstride(median([f['overstride'] for f in landing]))
        overall = round(knee.score*0.30 + trunk.score*0.25 + overstride.score*0.20 +
                        elbow.score*0.15 + hip.score*0.10)
    else:
        overstride = CategoryResult(50, 0.0, 0.0, 0.10, "%",
            "착지 프레임이 감지되지 않았습니다. 측면에서 촬영되었는지 확인해주세요.", "")
        overall = round(knee.score*0.375 + trunk.score*0.3125 +
                        elbow.score*0.1875 + hip.score*0.125)

    cadence     = eval_cadence(frame_angles_list)
    vertical_osc = eval_vertical_osc(frame_angles_list)

    grade    = grade_from(overall)
    feedback = build_overall_feedback(overall, knee, trunk, overstride)
    return PostureResult(overall, grade, feedback, knee, trunk, elbow, hip, overstride,
                         cadence, vertical_osc)


# ──────────────────────────────────────────────────────────────
# MediaPipe analysis
# ──────────────────────────────────────────────────────────────
def analyze_video(video_path, model_path, target_fps=10):
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        raise RuntimeError(f"Cannot open video: {video_path}")

    fps_native  = cap.get(cv2.CAP_PROP_FPS) or 30.0
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration_ms = int(total_frames / fps_native * 1000)
    vid_w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    vid_h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    print(f"Video: {vid_w}×{vid_h}, {fps_native:.1f}fps, {duration_ms}ms, {total_frames} frames")

    interval_ms  = max(1, 1000 // target_fps)
    step_frames  = max(1, int(fps_native / target_fps))

    base_opts = mp_python.BaseOptions(model_asset_path=model_path)
    options   = mp_vision.PoseLandmarkerOptions(
        base_options=base_opts,
        running_mode=mp_vision.RunningMode.VIDEO,
        num_poses=1,
        min_pose_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    )

    frame_angles  = []  # list of dicts with angle values
    video_frames  = []  # list of {t, pts} for JSON
    last_hash     = None

    with mp_vision.PoseLandmarker.create_from_options(options) as landmarker:
        frame_idx = 0
        while True:
            ret, bgr = cap.read()
            if not ret:
                break

            # Sample at target_fps
            if frame_idx % step_frames != 0:
                frame_idx += 1
                continue

            time_ms = int(frame_idx / fps_native * 1000)

            # Duplicate frame detection (cheap 5-pixel hash)
            h, w = bgr.shape[:2]
            content_hash = (
                int(bgr[h//4,   w//4,   0]) ^
                int(bgr[h//4,   3*w//4, 0]) ^
                int(bgr[h//2,   w//2,   0]) ^
                int(bgr[3*h//4, w//4,   0]) ^
                int(bgr[3*h//4, 3*w//4, 0])
            )
            if content_hash == last_hash:
                frame_idx += 1
                continue
            last_hash = content_hash

            rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
            result = landmarker.detect_for_video(mp_image, time_ms)

            if not result.pose_landmarks:
                frame_idx += 1
                continue

            lm = result.pose_landmarks[0]  # first pose

            # Skeleton points for overlay (13 key joints)
            skel_pts = []
            for idx in KEY_INDICES:
                p = lm[idx]
                skel_pts.append({"x": round(p.x, 5), "y": round(p.y, 5),
                                  "v": round(p.visibility, 4)})
            video_frames.append({"t": time_ms, "pts": skel_pts})

            # Determine dominant side (same logic as PostureAngleCalculator.kt)
            left_vis  = avg_vis(lm, L_SH, L_EL, L_WR, L_HIP, L_KN, L_AN)
            right_vis = avg_vis(lm, R_SH, R_EL, R_WR, R_HIP, R_KN, R_AN)
            best_vis  = max(left_vis, right_vis)
            if best_vis < VIS_THRESHOLD:
                frame_idx += 1
                continue

            use_left = left_vis >= right_vis
            S  = lm[L_SH  if use_left else R_SH]
            El = lm[L_EL  if use_left else R_EL]
            W  = lm[L_WR  if use_left else R_WR]
            H  = lm[L_HIP if use_left else R_HIP]
            K  = lm[L_KN  if use_left else R_KN]
            A  = lm[L_AN  if use_left else R_AN]

            knee_angle  = three_point_angle(H, K, A)
            elbow_angle = three_point_angle(S, El, W)
            hip_angle   = three_point_angle(S, H, K)
            trunk_angle = trunk_lean_angle(lm[L_SH], lm[R_SH], lm[L_HIP], lm[R_HIP])

            hip_mid_x      = (lm[L_HIP].x + lm[R_HIP].x) / 2
            hip_mid_y      = (lm[L_HIP].y + lm[R_HIP].y) / 2
            sh_mid_y       = (lm[L_SH].y  + lm[R_SH].y)  / 2
            facing_right   = lm[NOSE].x > hip_mid_x
            la_x, ra_x     = lm[L_AN].x, lm[R_AN].x
            if facing_right:
                leading_x  = max(la_x, ra_x)
                leading_y  = lm[L_AN].y if la_x >= ra_x else lm[R_AN].y
            else:
                leading_x  = min(la_x, ra_x)
                leading_y  = lm[L_AN].y if la_x <= ra_x else lm[R_AN].y
            body_h        = max(abs(leading_y - sh_mid_y), 0.01)
            overstride_r  = max(abs(leading_x - hip_mid_x) / body_h, 0.0)
            is_landing    = leading_y > 0.65 and leading_y > hip_mid_y

            near_ankle_y = lm[L_AN if use_left else R_AN].y
            hip_mid_y = (lm[L_HIP].y + lm[R_HIP].y) / 2.0
            frame_angles.append({
                'knee': knee_angle, 'trunk': trunk_angle,
                'elbow': elbow_angle, 'hip': hip_angle,
                'overstride': overstride_r, 'is_landing': is_landing,
                'near_ankle_y': near_ankle_y, 'hip_mid_y': hip_mid_y,
                'time_ms': time_ms,
            })

            frame_idx += 1

    cap.release()
    print(f"Analyzed frames: {len(video_frames)}, angle frames: {len(frame_angles)}")
    return frame_angles, video_frames, vid_w, vid_h


# ──────────────────────────────────────────────────────────────
# ADB helpers
# ──────────────────────────────────────────────────────────────
def adb(*args):
    cmd = ["adb"] + list(args)
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"ADB error: {r.stderr.strip()}")
    return r.stdout.strip()

def run_as(*args):
    return adb("shell", "run-as", PACKAGE, *args)


# ──────────────────────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────────────────────
def main():
    print("=== RunWay Posture Analysis Injector ===\n")

    # 1. Analyze video
    print("[1/5] Analyzing video with MediaPipe…")
    frame_angles, video_frames, vid_w, vid_h = analyze_video(VIDEO_PATH, MODEL_PATH, TARGET_FPS)
    if not frame_angles:
        print("ERROR: No pose detected. Check the video.")
        sys.exit(1)

    # 2. Compute scores
    print("[2/5] Computing scores…")
    result = evaluate(frame_angles)
    analysis_id = str(uuid.uuid4())
    created_at  = int(time.time() * 1000)

    print(f"  Overall: {result.overall_score} ({result.grade})")
    print(f"  Knee:      {result.knee.score}  ({result.knee.measured_value:.1f}°)")
    print(f"  Trunk:     {result.trunk.score}  ({result.trunk.measured_value:.1f}°)")
    print(f"  Elbow:     {result.elbow.score}  ({result.elbow.measured_value:.1f}°)")
    print(f"  Hip:       {result.hip.score}  ({result.hip.measured_value:.1f}°)")
    print(f"  Overstride:{result.overstride.score}  ({result.overstride.measured_value:.3f})")
    print(f"  Cadence:   {result.cadence.score}  ({result.cadence.measured_value:.0f} spm)")
    print(f"  VertOsc:   {result.vertical_osc.score}  ({result.vertical_osc.measured_value:.1f}%)")

    frames_json = json.dumps(video_frames, ensure_ascii=False)

    # 3. Push video to device
    print("[3/5] Pushing video to device…")
    remote_video = f"{VIDEO_REMOTE_DIR}/{analysis_id}.mp4"
    device_tmp   = f"/data/local/tmp/{analysis_id}.mp4"

    # Push to world-writable /data/local/tmp, then copy into app's private storage
    adb("push", VIDEO_PATH, device_tmp)
    adb("shell", f"run-as {PACKAGE} mkdir -p {VIDEO_REMOTE_DIR}")
    adb("shell", f"run-as {PACKAGE} cp {device_tmp} {remote_video}")
    adb("shell", f"rm {device_tmp}")
    print(f"  Video → {remote_video}")

    # 4. Insert into Room database
    print("[4/5] Inserting analysis into database…")
    # Force-stop so Room checkpoints WAL before we pull the db
    adb("shell", f"am force-stop {PACKAGE}")
    time.sleep(1.5)

    local_db     = f"/tmp/runway_posture_{analysis_id}.db"
    local_db_wal = local_db + "-wal"
    local_db_shm = local_db + "-shm"

    # Pull DB directly via exec-out cat (run-as can read its own files)
    with open(local_db, "wb") as f:
        subprocess.run(["adb", "exec-out", f"run-as {PACKAGE} cat {DB_REMOTE}"],
                       stdout=f, check=True)

    # Pull WAL + SHM if present (both needed to avoid lock errors)
    def pull_if_exists(remote, local):
        chk = subprocess.run(["adb", "shell", f"run-as {PACKAGE} ls {remote}"],
                              capture_output=True, text=True)
        exists = chk.returncode == 0 and bool(chk.stdout.strip()) and "No such" not in chk.stdout
        if exists:
            with open(local, "wb") as f:
                subprocess.run(["adb", "exec-out", f"run-as {PACKAGE} cat {remote}"],
                                stdout=f, check=True)
        return exists

    has_wal = pull_if_exists(f"{DB_REMOTE}-wal", local_db_wal)
    pull_if_exists(f"{DB_REMOTE}-shm", local_db_shm)
    print(f"  Pulled DB ({os.path.getsize(local_db)} bytes)" +
          (f" + WAL ({os.path.getsize(local_db_wal)} bytes)" if has_wal else ""))

    def esc(s):
        return str(s).replace("'", "''")

    insert_sql = f"""
INSERT INTO posture_analyses (
  id, createdAt, overallScore, grade, overallFeedback,
  kneeScore, kneeMeasuredAngle, kneeFeedback, kneeTip,
  trunkScore, trunkMeasuredAngle, trunkFeedback, trunkTip,
  elbowScore, elbowMeasuredAngle, elbowFeedback, elbowTip,
  hipScore, hipMeasuredAngle, hipFeedback, hipTip,
  overstrideScore, overstrideRatio, overstrideFeedback, overstrideTip,
  cadenceScore, cadenceSpm, cadenceFeedback, cadenceTip,
  verticalOscScore, verticalOscPercent, verticalOscFeedback, verticalOscTip,
  videoPath, videoFramesJson, videoWidth, videoHeight
) VALUES (
  '{esc(analysis_id)}', {created_at}, {result.overall_score}, '{esc(result.grade)}',
  '{esc(result.overall_feedback)}',
  {result.knee.score}, {result.knee.measured_value:.4f},
  '{esc(result.knee.feedback)}', '{esc(result.knee.tip)}',
  {result.trunk.score}, {result.trunk.measured_value:.4f},
  '{esc(result.trunk.feedback)}', '{esc(result.trunk.tip)}',
  {result.elbow.score}, {result.elbow.measured_value:.4f},
  '{esc(result.elbow.feedback)}', '{esc(result.elbow.tip)}',
  {result.hip.score}, {result.hip.measured_value:.4f},
  '{esc(result.hip.feedback)}', '{esc(result.hip.tip)}',
  {result.overstride.score}, {result.overstride.measured_value:.6f},
  '{esc(result.overstride.feedback)}', '{esc(result.overstride.tip)}',
  {result.cadence.score}, {result.cadence.measured_value:.2f},
  '{esc(result.cadence.feedback)}', '{esc(result.cadence.tip)}',
  {result.vertical_osc.score}, {result.vertical_osc.measured_value:.4f},
  '{esc(result.vertical_osc.feedback)}', '{esc(result.vertical_osc.tip)}',
  '{esc(remote_video)}', '{esc(frames_json)}',
  {vid_w}, {vid_h}
);
""".strip()

    # The SHM file holds a stale write-lock marker from the force-stopped app.
    # Deleting it locally lets sqlite3 create a fresh coordinator and proceed.
    if os.path.exists(local_db_shm):
        os.remove(local_db_shm)

    con = sqlite3.connect(local_db)
    con.execute("PRAGMA busy_timeout=5000")
    # Switch to DELETE journal mode: sqlite3 checkpoints any existing WAL into
    # the main file first, then all writes go directly to the main file.
    # This lets us push a single self-contained db without a separate WAL.
    con.execute("PRAGMA journal_mode=DELETE")

    # Ensure v3 schema columns exist. Room migration may still be in the WAL
    # (not yet checkpointed) when we pull the base DB, so we apply them here.
    existing_cols = {row[1] for row in con.execute("PRAGMA table_info(posture_analyses)").fetchall()}
    v3_cols = [
        ("cadenceScore",      "INTEGER NOT NULL DEFAULT 0"),
        ("cadenceSpm",        "REAL NOT NULL DEFAULT 0"),
        ("cadenceFeedback",   "TEXT NOT NULL DEFAULT ''"),
        ("cadenceTip",        "TEXT NOT NULL DEFAULT ''"),
        ("verticalOscScore",  "INTEGER NOT NULL DEFAULT 0"),
        ("verticalOscPercent","REAL NOT NULL DEFAULT 0"),
        ("verticalOscFeedback","TEXT NOT NULL DEFAULT ''"),
        ("verticalOscTip",    "TEXT NOT NULL DEFAULT ''"),
    ]
    for col, coldef in v3_cols:
        if col not in existing_cols:
            con.execute(f"ALTER TABLE posture_analyses ADD COLUMN {col} {coldef}")
            print(f"  Applied migration: added column {col}")
    # Stamp user_version=3 so Room doesn't re-run MIGRATION_2_3 on startup.
    con.execute("PRAGMA user_version = 3")
    # Update Room's identity hash to match the v3 schema.
    # The expected hash is read from the FATAL EXCEPTION log:
    #   "Expected identity hash: 8b2b7607aed7cfdd1b20e0d590f1154e"
    con.execute(
        "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
        ("8b2b7607aed7cfdd1b20e0d590f1154e",),
    )

    con.execute(insert_sql)
    con.commit()
    con.close()

    # Push modified DB back to device
    # adb push → /data/local/tmp (shell-writable), then run-as cp into app space
    device_tmp_new = "/data/local/tmp/runway_posture_new.db"
    adb("push", local_db, device_tmp_new)
    adb("shell", f"run-as {PACKAGE} cp {device_tmp_new} {DB_REMOTE}")
    # Remove any stale WAL/SHM so Room starts clean
    adb("shell", f"run-as {PACKAGE} rm -f {DB_REMOTE}-wal {DB_REMOTE}-shm")
    adb("shell", f"rm -f {device_tmp_new}")

    # Cleanup local temp files
    for f in [local_db, local_db_wal if has_wal else ""]:
        if f and os.path.exists(f):
            os.remove(f)

    # 5. Launch the app
    print("[5/5] Launching app…")
    adb("shell", f"monkey -p {PACKAGE} -c android.intent.category.LAUNCHER 1")

    print(f"\n✓ Done! Analysis ID: {analysis_id}")
    print(f"  Score: {result.overall_score} ({result.grade})")
    print("  → 앱에서 '자세' 탭 → 분석 이력에서 확인하세요.")


if __name__ == "__main__":
    main()
