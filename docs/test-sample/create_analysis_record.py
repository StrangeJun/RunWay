"""
Runs posture analysis on a local video (Mac-side MediaPipe) and inserts the
result directly into the Android app's Room SQLite database via ADB.

Ports:
  PostureAngleCalculator.kt  → compute_angles()
  PostureRuleEngine.kt       → evaluate()
  PostureAnalysisViewModel   → to_entity() + DB insert

Usage:
    cd /Users/jun/Developer/RunWay
    python3 docs/test-sample/create_analysis_record.py
"""

import json, math, os, sqlite3, subprocess, tempfile, time, uuid
import cv2
import mediapipe as mp
import numpy as np
from collections import deque
from dataclasses import dataclass, field
from typing import List, Optional

# ── Config ────────────────────────────────────────────────────────────────────
VIDEO_IN   = "docs/test-sample/running_test.mov"
MODEL      = "android/app/src/main/assets/pose_landmarker_lite.task"
TARGET_FPS = 10
PACKAGE    = "com.runway.android"
DB_NAME    = "posture_database"

# MediaPipe landmark indices
NOSE       = 0
L_SH, R_SH = 11, 12
L_EL, R_EL = 13, 14
L_WR, R_WR = 15, 16
L_HIP, R_HIP = 23, 24
L_KN, R_KN  = 25, 26
L_AN, R_AN  = 27, 28

KEY_INDICES = [NOSE, L_SH, R_SH, L_EL, R_EL, L_WR, R_WR, L_HIP, R_HIP, L_KN, R_KN, L_AN, R_AN]

# ── Data classes ──────────────────────────────────────────────────────────────
@dataclass
class FrameAngles:
    knee_flex:       float
    trunk_lean:      float
    elbow:           float
    hip_extension:   float
    overstride_ratio: float
    is_landing:      bool
    visibility:      float
    timestamp_ms:    int
    hip_mid_y:       float
    near_ankle_y:    float

@dataclass
class SkeletonPoint:
    x: float; y: float; v: float

@dataclass
class VideoFrame:
    t: int
    pts: List[SkeletonPoint]

@dataclass
class CategoryResult:
    score: int; value: float; ideal_min: float; ideal_max: float
    unit: str; feedback: str; tip: str

# ── PostureAngleCalculator port ───────────────────────────────────────────────
def _avg_vis(lm, *indices):
    return sum(lm[i][2] for i in indices) / len(indices)

def _angle3(ax, ay, bx, by, cx, cy):
    vax, vay = ax-bx, ay-by
    vcx, vcy = cx-bx, cy-by
    dot = vax*vcx + vay*vcy
    mag = math.sqrt(vax**2+vay**2) * math.sqrt(vcx**2+vcy**2)
    if mag < 1e-6: return 180.0
    return math.degrees(math.acos(max(-1.0, min(1.0, dot/mag))))

def _trunk_lean(lm):
    sh_mx = (lm[L_SH][0]+lm[R_SH][0])/2; sh_my = (lm[L_SH][1]+lm[R_SH][1])/2
    hi_mx = (lm[L_HIP][0]+lm[R_HIP][0])/2; hi_my = (lm[L_HIP][1]+lm[R_HIP][1])/2
    dx = sh_mx - hi_mx; dy = hi_my - sh_my
    return math.degrees(math.atan2(dx, dy))

def compute_angles(lm, t_ms) -> Optional[FrameAngles]:
    """Port of PostureAngleCalculator.compute()."""
    left_vis  = _avg_vis(lm, L_SH, L_EL, L_WR, L_HIP, L_KN, L_AN)
    right_vis = _avg_vis(lm, R_SH, R_EL, R_WR, R_HIP, R_KN, R_AN)
    best_vis  = max(left_vis, right_vis)
    if best_vis < 0.55: return None

    use_left = left_vis >= right_vis
    S = L_SH if use_left else R_SH
    E = L_EL if use_left else R_EL
    W = L_WR if use_left else R_WR
    H = L_HIP if use_left else R_HIP
    K = L_KN  if use_left else R_KN
    A = L_AN  if use_left else R_AN

    knee  = _angle3(lm[H][0], lm[H][1], lm[K][0], lm[K][1], lm[A][0], lm[A][1])
    elbow = _angle3(lm[S][0], lm[S][1], lm[E][0], lm[E][1], lm[W][0], lm[W][1])
    hip   = _angle3(lm[S][0], lm[S][1], lm[H][0], lm[H][1], lm[K][0], lm[K][1])
    trunk = _trunk_lean(lm)

    hip_mid_x     = (lm[L_HIP][0] + lm[R_HIP][0]) / 2
    hip_mid_y     = (lm[L_HIP][1] + lm[R_HIP][1]) / 2
    shoulder_mid_y= (lm[L_SH][1]  + lm[R_SH][1])  / 2
    facing_right  = lm[NOSE][0] > hip_mid_x

    l_ax, r_ax = lm[L_AN][0], lm[R_AN][0]
    if facing_right:
        leading_ax = max(l_ax, r_ax)
        leading_ay = lm[L_AN][1] if l_ax >= r_ax else lm[R_AN][1]
    else:
        leading_ax = min(l_ax, r_ax)
        leading_ay = lm[L_AN][1] if l_ax <= r_ax else lm[R_AN][1]

    body_h = max(abs(leading_ay - shoulder_mid_y), 0.01)
    overstride = max(0.0, abs(leading_ax - hip_mid_x) / body_h)
    is_landing = leading_ay > 0.65 and leading_ay > hip_mid_y

    near_ankle_y = lm[A][1]

    return FrameAngles(
        knee_flex=knee, trunk_lean=trunk, elbow=elbow, hip_extension=hip,
        overstride_ratio=overstride, is_landing=is_landing, visibility=best_vis,
        timestamp_ms=t_ms, hip_mid_y=hip_mid_y, near_ankle_y=near_ankle_y,
    )

# ── PostureRuleEngine port ────────────────────────────────────────────────────
def _median(lst):
    if not lst: return 0.0
    s = sorted(lst); n = len(s)
    return (s[n//2-1]+s[n//2])/2 if n%2==0 else s[n//2]

def _angle_score(v, ideal_min, ideal_max):
    if ideal_min <= v <= ideal_max: return 100
    dev = ideal_min - v if v < ideal_min else v - ideal_max
    if dev <= 5:  return int(90 - (dev/5)*20)
    if dev <= 15: return int(70 - ((dev-5)/10)*30)
    return max(0, int(40 - ((dev-15)/15)*40))

def _eval_knee(angle):
    mn, mx = 135.0, 165.0
    score = _angle_score(angle, mn, mx)
    if angle < mn-15:  fb = "착지 시 무릎이 너무 많이 구부러져 있습니다. 보폭을 조금 줄여보세요."
    elif angle < mn:   fb = "착지 시 무릎이 약간 많이 구부러져 있습니다. 조금 더 펴보세요."
    elif angle > mx+15:fb = "착지 시 무릎이 너무 펴져 있어 충격 흡수가 부족합니다."
    elif angle > mx:   fb = "착지 시 무릎을 살짝 더 구부려 충격을 흡수해보세요."
    else:              fb = "착지 시 무릎 각도가 이상적입니다."
    tip = "무릎을 살짝 구부린 상태(135~165°)로 착지하면 관절 충격을 효과적으로 분산시킬 수 있습니다." if score < 80 else ""
    return CategoryResult(score, angle, mn, mx, "°", fb, tip)

def _eval_trunk(angle):
    mn, mx = 5.0, 10.0
    score = _angle_score(angle, mn, mx)
    if angle < 0:      fb = "상체가 뒤로 기울어져 있습니다. 전방으로 기울여 보세요."
    elif angle < mn:   fb = "상체 기울기가 부족합니다. 약 5~10도 앞으로 기울여 보세요."
    elif angle > mx+10:fb = "상체가 너무 앞으로 기울어져 있어 허리에 부담이 됩니다."
    elif angle > mx:   fb = "상체 기울기가 조금 과합니다."
    else:              fb = "상체 기울기가 이상적입니다."
    tip = "전방 기울기는 추진력과 효율을 높여줍니다." if score < 80 else ""
    return CategoryResult(score, angle, mn, mx, "°", fb, tip)

def _eval_elbow(angle):
    mn, mx = 85.0, 95.0
    score = _angle_score(angle, mn, mx)
    if angle < mn-15:  fb = "팔꿈치가 너무 많이 구부러져 있어 경직됩니다."
    elif angle < mn:   fb = "팔꿈치를 약 90도로 유지해보세요."
    elif angle > mx+25:fb = "팔꿈치가 너무 펴져 있어 에너지 손실이 발생합니다."
    elif angle > mx:   fb = "팔꿈치를 약 90도로 줄여보세요."
    else:              fb = "팔꿈치 각도가 이상적입니다."
    tip = "팔꿈치를 90도 유지하면 리듬감 있는 팔 스윙이 가능합니다." if score < 80 else ""
    return CategoryResult(score, angle, mn, mx, "°", fb, tip)

def _eval_hip(angle):
    mn, mx = 160.0, 180.0
    score = _angle_score(angle, mn, mx)
    if angle < mn-15: fb = "고관절 신전이 크게 부족합니다. 뒤 발 차기를 강화해보세요."
    elif angle < mn:  fb = "push-off 시 고관절을 조금 더 신전시켜 보세요."
    else:             fb = "고관절 신전이 적절합니다."
    tip = "완전한 고관절 신전은 추진력을 높여줍니다." if score < 80 else ""
    return CategoryResult(score, angle, mn, mx, "°", fb, tip)

def _eval_overstride(ratio):
    if ratio <= 0.10:   score = 100
    elif ratio <= 0.20: score = max(0, int(100 - ((ratio-0.10)/0.10)*40))
    elif ratio <= 0.30: score = max(0, int(60  - ((ratio-0.20)/0.10)*30))
    else:               score = max(0, int(30  - ((ratio-0.30)/0.10)*30))
    if ratio <= 0.10:   fb = "착지 위치가 이상적입니다."
    elif ratio <= 0.20: fb = "착지 위치가 약간 앞쪽입니다. 보폭을 조금 줄여보세요."
    elif ratio <= 0.30: fb = "오버스트라이드가 감지됩니다. 보폭을 줄이고 케이던스를 높여보세요."
    else:               fb = "착지 위치가 몸 앞쪽으로 많이 나와 있습니다. 충격과 부상 위험이 높습니다."
    tip = "발이 엉덩이 아래에 가깝게 착지하면 제동력을 줄일 수 있습니다." if score < 80 else ""
    return CategoryResult(score, ratio, 0.0, 0.10, "%", fb, tip)

def _eval_cadence(frames: List[FrameAngles]):
    mn, mx = 170.0, 180.0
    no_data = CategoryResult(0, 0.0, mn, mx, "spm", "케이던스 측정을 위해 더 긴 구간이 필요합니다.", "")
    if len(frames) < 8: return no_data
    dur_ms = frames[-1].timestamp_ms - frames[0].timestamp_ms
    if dur_ms < 2000: return no_data

    ankle_y = [f.near_ankle_y for f in frames]
    min_peak_y = 0.55; min_gap = 5; step_count = 0; last_peak = -min_gap
    for i in range(1, len(ankle_y)-1):
        if i - last_peak < min_gap: continue
        if ankle_y[i] > ankle_y[i-1] and ankle_y[i] >= ankle_y[i+1] and ankle_y[i] > min_peak_y:
            step_count += 1; last_peak = i
    if step_count < 3: return no_data

    spm = step_count * 2.0 * 60000.0 / dur_ms
    score = _angle_score(spm, mn, mx)
    if spm < 150:   fb = "케이던스가 매우 낮습니다. 보폭을 줄이고 발놀림을 빠르게 해보세요."
    elif spm < mn:  fb = f"케이던스 {int(spm)}spm은 낮습니다. 170spm 이상을 목표로 해보세요."
    else:           fb = f"케이던스 {int(spm)}spm으로 이상적입니다."
    tip = "케이던스를 높이면 오버스트라이드가 줄고 부상 위험이 낮아집니다." if score < 80 else ""
    return CategoryResult(score, spm, mn, mx, "spm", fb, tip)

def _eval_vert_osc(frames: List[FrameAngles]):
    mn, mx = 4.0, 8.0
    no_data = CategoryResult(0, 0.0, mn, mx, "%", "수직진폭 측정을 위해 더 긴 구간이 필요합니다.", "")
    if len(frames) < 8: return no_data

    hip_y = sorted(f.hip_mid_y for f in frames)
    q25 = hip_y[int(len(hip_y)*0.25)]; q75 = hip_y[int(len(hip_y)*0.75)]
    amp = q75 - q25

    landing = [f for f in frames if f.is_landing]
    if landing:
        leg_len = sum(max(0.0, f.near_ankle_y - f.hip_mid_y) for f in landing) / len(landing)
    else:
        leg_len = max(0.15, sum(f.near_ankle_y for f in frames)/len(frames) -
                            sum(f.hip_mid_y for f in frames)/len(frames))
    body_h = leg_len / 0.52
    osc = max(0.0, amp / body_h * 100.0)

    score = _angle_score(osc, mn, mx)
    if osc < 2:       fb = "수직진폭이 매우 작습니다."
    elif osc <= mx:   fb = "수직진폭이 이상적입니다. 에너지 효율이 좋습니다."
    elif osc <= 12:   fb = "수직진폭이 약간 큽니다. 상체를 안정화해보세요."
    else:             fb = "상하 진폭이 큽니다. 앞으로 나아가는 에너지가 낭비되고 있습니다."
    tip = "코어 강화와 자세 안정화로 불필요한 바운싱을 줄일 수 있습니다." if osc > mx else ""
    return CategoryResult(score, osc, mn, mx, "%", fb, tip)

def evaluate(frames: List[FrameAngles]):
    landing     = [f for f in frames if f.is_landing]
    has_landing = len(landing) > 0
    land_or_all = landing if has_landing else frames

    knee_med  = _median([f.knee_flex     for f in land_or_all])
    trunk_med = _median([f.trunk_lean    for f in frames])
    elbow_med = _median([f.elbow         for f in frames])
    hip_med   = _median([f.hip_extension for f in frames])

    knee   = _eval_knee(knee_med)
    trunk  = _eval_trunk(trunk_med)
    elbow  = _eval_elbow(elbow_med)
    hip    = _eval_hip(hip_med)

    if has_landing:
        overstride = _eval_overstride(_median([f.overstride_ratio for f in landing]))
        overall    = round(knee.score*0.30 + trunk.score*0.25 + overstride.score*0.20 +
                           elbow.score*0.15 + hip.score*0.10)
    else:
        overstride = CategoryResult(50, 0.0, 0.0, 0.10, "%",
            "착지 프레임이 감지되지 않았습니다. 측면에서 촬영되었는지 확인해주세요.", "")
        overall    = round(knee.score*0.375 + trunk.score*0.3125 +
                           elbow.score*0.1875 + hip.score*0.125)

    grade_map = [(90,'S'),(75,'A'),(60,'B'),(45,'C')]
    grade = next((g for s,g in grade_map if overall>=s), 'D')

    weakest = min([('무릎 굴곡',knee.score),('상체 기울기',trunk.score),('오버스트라이드',overstride.score)],
                  key=lambda x: x[1])
    if overall >= 90:   fb = "런닝 자세가 매우 훌륭합니다. 현재 자세를 유지하세요!"
    elif overall >= 75: fb = f"전반적으로 좋은 자세입니다. {weakest[0]} 부분을 보완하면 더 좋아집니다."
    elif overall >= 60: fb = f"{weakest[0]} 개선이 필요합니다. 꾸준히 연습해 보세요."
    else:               fb = "자세 개선이 필요합니다. 각 항목의 피드백을 참고해 연습해 보세요."

    cadence = _eval_cadence(frames)
    vert    = _eval_vert_osc(frames)

    return dict(overall=overall, grade=grade, feedback=fb,
                knee=knee, trunk=trunk, elbow=elbow, hip=hip,
                overstride=overstride, cadence=cadence, vert=vert)

# ── MediaPipe pipeline ────────────────────────────────────────────────────────
def run_mediapipe(video_path, model_path, target_fps=10):
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened(): raise RuntimeError(f"Cannot open {video_path}")
    native_fps   = cap.get(cv2.CAP_PROP_FPS) or 30.0
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    vid_w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    vid_h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    step  = max(1, int(native_fps / target_fps))
    print(f"  Video: {vid_w}x{vid_h}, {native_fps:.1f}fps, {total_frames} frames, step={step}")

    options = mp.tasks.vision.PoseLandmarkerOptions(
        base_options=mp.tasks.BaseOptions(model_asset_path=model_path),
        running_mode=mp.tasks.vision.RunningMode.VIDEO,
        num_poses=1,
        min_pose_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    )

    angle_frames: List[FrameAngles] = []
    video_frames: List[VideoFrame]  = []

    with mp.tasks.vision.PoseLandmarker.create_from_options(options) as lmk:
        fi = 0
        while True:
            ret, bgr = cap.read()
            if not ret: break
            if fi % step == 0:
                t_ms = int(fi / native_fps * 1000)
                res  = lmk.detect_for_video(
                    mp.Image(image_format=mp.ImageFormat.SRGB,
                             data=cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)), t_ms)
                if res.pose_landmarks:
                    lm_raw = res.pose_landmarks[0]
                    # Full landmark list for angle computation
                    lm = [(lm_raw[i].x, lm_raw[i].y, lm_raw[i].visibility) for i in range(len(lm_raw))]
                    # 13-point skeleton for video replay
                    skel = [SkeletonPoint(lm_raw[i].x, lm_raw[i].y, lm_raw[i].visibility)
                            for i in KEY_INDICES]
                    video_frames.append(VideoFrame(t=t_ms, pts=skel))
                    angles = compute_angles(lm, t_ms)
                    if angles:
                        angle_frames.append(angles)
            fi += 1
    cap.release()
    print(f"  {len(angle_frames)} angle frames, {len(video_frames)} video frames")
    return angle_frames, video_frames, vid_w, vid_h

# ── ADB helpers ───────────────────────────────────────────────────────────────
def adb(*args, **kwargs):
    return subprocess.run(['adb', *args], capture_output=True, text=True, **kwargs)

def adb_shell(cmd):
    return subprocess.run(['adb', 'shell', cmd], capture_output=True, text=True)

def push_video_to_internal(local_path, analysis_id):
    """Push video to app's internal files/posture/ via stdin pipe to run-as dd."""
    adb_shell(f"run-as {PACKAGE} mkdir -p files/posture")
    dest = f"files/posture/{analysis_id}.mp4"
    print("  Pushing video to device...")
    with open(local_path, 'rb') as f:
        r = subprocess.run(
            ['adb', 'shell', f'run-as {PACKAGE} dd of={dest}'],
            stdin=f, capture_output=True,
        )
    if r.returncode != 0:
        raise RuntimeError(f"push_video_to_internal failed: {r.stderr.decode()}")
    # Verify the file landed
    ls = adb_shell(f"run-as {PACKAGE} ls -la {dest}")
    if ls.returncode != 0:
        raise RuntimeError(f"Video not found after push: {dest}")
    print(f"  {ls.stdout.strip()}")
    internal_path = f"/data/data/{PACKAGE}/{dest}"
    return internal_path

def _exec_out_cat(remote_path, local_path):
    """Stream a file from device via exec-out cat."""
    with open(local_path, 'wb') as f:
        r = subprocess.run(['adb', 'exec-out', f'run-as {PACKAGE} cat {remote_path}'],
                           stdout=f, stderr=subprocess.PIPE)
    return r.returncode == 0 and os.path.getsize(local_path) > 0

def pull_db(tmp_dir):
    """Pull posture_database (+ WAL if present) from app sandbox via exec-out cat."""
    local_db  = os.path.join(tmp_dir, DB_NAME)
    local_wal = os.path.join(tmp_dir, DB_NAME + '-wal')
    local_shm = os.path.join(tmp_dir, DB_NAME + '-shm')

    if not _exec_out_cat(f'databases/{DB_NAME}', local_db):
        raise RuntimeError('pull_db: failed to pull main DB file')

    # Pull WAL / SHM so we can checkpoint locally (ignore failure — may not exist)
    _exec_out_cat(f'databases/{DB_NAME}-wal', local_wal)
    _exec_out_cat(f'databases/{DB_NAME}-shm', local_shm)

    # Merge WAL into main DB and switch to DELETE journal mode so the pushed file
    # is fully self-contained (no WAL dependency on device side).
    conn = sqlite3.connect(local_db)
    conn.execute('PRAGMA wal_checkpoint(TRUNCATE)')
    conn.execute('PRAGMA journal_mode=DELETE')
    conn.commit(); conn.close()

    # Remove the local WAL/SHM — we'll push only the checkpointed main file
    for p in (local_wal, local_shm):
        if os.path.exists(p): os.remove(p)

    return local_db

def push_db(local_db):
    """Push modified DB back via stdin pipe to run-as dd."""
    # Backup current DB on device first
    adb_shell(f"run-as {PACKAGE} cp databases/{DB_NAME} databases/{DB_NAME}.bak")
    with open(local_db, 'rb') as f:
        subprocess.run(
            ['adb', 'shell', f'run-as {PACKAGE} dd of=databases/{DB_NAME}'],
            stdin=f, check=True, capture_output=True,
        )
    # Delete stale WAL/SHM so Room re-creates them cleanly
    adb_shell(f"run-as {PACKAGE} rm -f databases/{DB_NAME}-wal databases/{DB_NAME}-shm")

def insert_record(local_db, row: dict):
    conn = sqlite3.connect(local_db)
    cols = ", ".join(row.keys())
    placeholders = ", ".join("?" for _ in row)
    conn.execute(f"INSERT OR REPLACE INTO posture_analyses ({cols}) VALUES ({placeholders})",
                 list(row.values()))
    conn.commit()
    conn.close()

# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    print("=== RunWay Posture Analysis Record Creator ===\n")

    print("[1/5] Running MediaPipe analysis...")
    angle_frames, video_frames, vid_w, vid_h = run_mediapipe(VIDEO_IN, MODEL, TARGET_FPS)
    if not angle_frames:
        print("ERROR: No poses detected. Aborting.")
        return

    print("[2/5] Evaluating posture...")
    result = evaluate(angle_frames)
    r = result
    print(f"  Score: {r['overall']} ({r['grade']})  Knee:{r['knee'].score}  Trunk:{r['trunk'].score}  "
          f"Elbow:{r['elbow'].score}  Hip:{r['hip'].score}  Overstride:{r['overstride'].score}")
    print(f"  Cadence: {r['cadence'].value:.1f}spm  VertOsc: {r['vert'].value:.1f}%")

    print("[3/5] Building JSON payload...")
    # Serialize video frames matching Gson's PostureVideoFrame / SkeletonPoint field names
    frames_json = json.dumps([
        {"t": vf.t, "pts": [{"x": p.x, "y": p.y, "v": p.v} for p in vf.pts]}
        for vf in video_frames
    ])

    analysis_id = str(uuid.uuid4())
    created_at  = int(time.time() * 1000)
    print(f"  Analysis ID: {analysis_id}")

    print("[4/5] Copying video to device internal storage...")
    internal_video_path = push_video_to_internal(VIDEO_IN, analysis_id)

    print("[5/5] Inserting into Room DB...")
    row = dict(
        id                  = analysis_id,
        createdAt           = created_at,
        overallScore        = r['overall'],
        grade               = r['grade'],
        overallFeedback     = r['feedback'],
        kneeScore           = r['knee'].score,
        kneeMeasuredAngle   = r['knee'].value,
        kneeFeedback        = r['knee'].feedback,
        kneeTip             = r['knee'].tip,
        trunkScore          = r['trunk'].score,
        trunkMeasuredAngle  = r['trunk'].value,
        trunkFeedback       = r['trunk'].feedback,
        trunkTip            = r['trunk'].tip,
        elbowScore          = r['elbow'].score,
        elbowMeasuredAngle  = r['elbow'].value,
        elbowFeedback       = r['elbow'].feedback,
        elbowTip            = r['elbow'].tip,
        hipScore            = r['hip'].score,
        hipMeasuredAngle    = r['hip'].value,
        hipFeedback         = r['hip'].feedback,
        hipTip              = r['hip'].tip,
        overstrideScore     = r['overstride'].score,
        overstrideRatio     = r['overstride'].value,
        overstrideFeedback  = r['overstride'].feedback,
        overstrideTip       = r['overstride'].tip,
        cadenceScore        = r['cadence'].score,
        cadenceSpm          = r['cadence'].value,
        cadenceFeedback     = r['cadence'].feedback,
        cadenceTip          = r['cadence'].tip,
        verticalOscScore    = r['vert'].score,
        verticalOscPercent  = r['vert'].value,
        verticalOscFeedback = r['vert'].feedback,
        verticalOscTip      = r['vert'].tip,
        videoPath           = internal_video_path,
        videoFramesJson     = frames_json,
        videoWidth          = vid_w,
        videoHeight         = vid_h,
    )

    with tempfile.TemporaryDirectory() as tmp_dir:
        local_db = pull_db(tmp_dir)
        insert_record(local_db, row)
        push_db(local_db)

    print(f"\nDone! Record inserted: score={r['overall']} grade={r['grade']}")
    print("Restart or re-open the app to see it in the analysis history.")

if __name__ == "__main__":
    main()
