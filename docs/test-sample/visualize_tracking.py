"""
GaitKeeper-style running overlay visualization with running-form-analyzer metrics.

Visual style: GaitKeeper (chrismarth/GaitKeeper, MIT)
  - Both legs drawn: left=amber (#f59e0b), right=red (#ef4444)
  - Torso: blue (#3b82f6), arms: green (#10b981)
  - Downward strike arrow at ankle on foot-strike detection

Strike detection: running-form-analyzer (henryczup/running-form-analyzer)
  - Y-axis local maximum (ankle closest to ground = local max of Y)
  - 1D Kalman filter (measNoise=0.1, procNoise=0.01)
  - Adaptive threshold: 10% of motion range, EMA-smoothed, clamped [0.005, 0.1]
  - Min 400ms between strikes

Angle overlay: trunk lean, knee flex (both legs), shank angle, elbow angle

Usage:
    cd /Users/jun/Developer/RunWay
    python3 docs/test-sample/visualize_tracking.py
"""

import math
import cv2
import mediapipe as mp
import numpy as np
from collections import deque

# ── MediaPipe landmark indices ────────────────────────────────────────────────
NOSE          =  0
L_SH,  R_SH  = 11, 12
L_EL,  R_EL  = 13, 14
L_WR,  R_WR  = 15, 16
L_HIP, R_HIP = 23, 24
L_KN,  R_KN  = 25, 26
L_AN,  R_AN  = 27, 28

KEY_INDICES = [NOSE, L_SH, R_SH, L_EL, R_EL, L_WR, R_WR, L_HIP, R_HIP, L_KN, R_KN, L_AN, R_AN]

SKEL_NOSE  = 0
SKEL_L_SH  = 1;  SKEL_R_SH  = 2
SKEL_L_EL  = 3;  SKEL_R_EL  = 4
SKEL_L_WR  = 5;  SKEL_R_WR  = 6
SKEL_L_HIP = 7;  SKEL_R_HIP = 8
SKEL_L_KN  = 9;  SKEL_R_KN  = 10
SKEL_L_AN  = 11; SKEL_R_AN  = 12

# ── GaitKeeper colors (BGR) ───────────────────────────────────────────────────
C_TORSO     = (246, 130,  59)   # #3b82f6 blue
C_ARM       = (129, 185,  16)   # #10b981 green
C_LEFT_LEG  = ( 11, 158, 245)   # #f59e0b amber
C_RIGHT_LEG = ( 68,  68, 239)   # #ef4444 red
C_JOINT_BG  = ( 39,  24,  17)   # #111827 dark fill
C_JOINT_STK = (255, 255, 255)   # white stroke
SCORE_THRESH = 0.3

# ── Skeleton edges ─────────────────────────────────────────────────────────────
EDGES_TORSO = [
    (SKEL_L_SH,  SKEL_R_SH),
    (SKEL_L_SH,  SKEL_L_HIP),
    (SKEL_R_SH,  SKEL_R_HIP),
    (SKEL_L_HIP, SKEL_R_HIP),
]
EDGES_L_ARM  = [(SKEL_L_SH, SKEL_L_EL), (SKEL_L_EL, SKEL_L_WR)]
EDGES_R_ARM  = [(SKEL_R_SH, SKEL_R_EL), (SKEL_R_EL, SKEL_R_WR)]
EDGES_L_LEG  = [(SKEL_L_HIP, SKEL_L_KN), (SKEL_L_KN, SKEL_L_AN)]
EDGES_R_LEG  = [(SKEL_R_HIP, SKEL_R_KN), (SKEL_R_KN, SKEL_R_AN)]

# ── One Euro Filter ───────────────────────────────────────────────────────────
class OneDEuroFilter:
    def __init__(self, min_cutoff=1.0, beta=3.0, d_cutoff=1.0):
        self.min_cutoff = min_cutoff; self.beta = beta; self.d_cutoff = d_cutoff
        self.initialized = False; self.x_prev = 0.0; self.dx = 0.0; self.last = 0.0

    def _a(self, c, dt): r = 2*math.pi*c*dt; return r/(r+1)

    def filter(self, x, dt):
        if not self.initialized:
            self.initialized = True; self.x_prev = x; self.last = x; return x
        dx = (x - self.x_prev) / dt
        self.dx = self._a(self.d_cutoff, dt)*dx + (1-self._a(self.d_cutoff, dt))*self.dx
        c = self.min_cutoff + self.beta * abs(self.dx)
        self.last = self._a(c, dt)*x + (1-self._a(c, dt))*self.last
        self.x_prev = x; return self.last

class SkeletonSmoother:
    def __init__(self, n=13):
        self.fx = [OneDEuroFilter() for _ in range(n)]
        self.fy = [OneDEuroFilter() for _ in range(n)]
        self.vis = [0.0]*n; self.last_t = -1

    def smooth(self, pts, t_ms):
        dt = 1/30 if self.last_t<0 else max(0.001, min(0.5, (t_ms-self.last_t)/1000))
        self.last_t = t_ms
        out = []
        for i,(x,y,v) in enumerate(pts):
            ok = v >= SCORE_THRESH or not self.fx[i].initialized
            sx = self.fx[i].filter(x,dt) if ok else self.fx[i].last
            sy = self.fy[i].filter(y,dt) if ok else self.fy[i].last
            self.vis[i] += 0.4*(v-self.vis[i])
            out.append((sx, sy, self.vis[i]))
        return out

# ── running-form-analyzer FootStrikeDetector (1D Kalman + adaptive threshold) ─
class FootStrikeDetector:
    """
    Exact port of running-form-analyzer FootStrikeDetector.
    Source: AngleMetrics.__init__ uses StepMetrics(filter_type='temporal', detection_axis='y').

    Detects step via Y-axis LOCAL MINIMUM (ankle at its highest screen position =
    peak of swing phase). Uses a simple moving average (temporal filter, window=10)
    matching filter_type='temporal'. Adaptive threshold clamped to [0.005, 0.1].
    """
    def __init__(self, window_size=10, adaptive_window=50, min_interval_ms=400):
        self.window_size = window_size
        self.adaptive_window = adaptive_window
        self.min_interval_ms = min_interval_ms
        # Temporal filter (moving average) — matches filter_type='temporal'
        self.filter_buffer = deque(maxlen=window_size)
        # Detection
        self.positions = deque()
        self.motion_history = deque()
        self.threshold = 0.02
        self.last_strike_ms = -1

    def _temporal_filter(self, value):
        self.filter_buffer.append(value)
        if len(self.filter_buffer) < self.window_size:
            return value
        return sum(self.filter_buffer) / len(self.filter_buffer)

    def _update_threshold(self):
        if len(self.motion_history) < self.adaptive_window: return
        r = max(self.motion_history) - min(self.motion_history)
        self.threshold = max(0.005, min(0.1, 0.7*self.threshold + 0.3*r*0.1))

    def update(self, ankle_y, ankle_vis, t_ms):
        """Returns True on the frame a step event is detected (swing peak)."""
        if ankle_vis < 0.3: return False
        filtered = self._temporal_filter(ankle_y)

        if len(self.positions) >= self.window_size: self.positions.popleft()
        self.positions.append(filtered)
        if len(self.motion_history) >= self.adaptive_window: self.motion_history.popleft()
        self.motion_history.append(filtered)

        self._update_threshold()
        if len(self.positions) < 3: return False

        p0 = self.positions[-3]; p1 = self.positions[-2]; p2 = self.positions[-1]
        # Local minimum — matches original:
        #   positions[-2] < positions[-1] and positions[-2] < positions[-3]
        #   and positions[-1] - positions[-2] > threshold
        local_min = p1 < p0 and p1 < p2 and (p2 - p1) > self.threshold
        if not local_min: return False

        if self.last_strike_ms >= 0 and (t_ms - self.last_strike_ms) < self.min_interval_ms:
            return False

        self.last_strike_ms = t_ms
        return True

# ── Angle computation helpers ─────────────────────────────────────────────────
def three_point_angle(ax, ay, bx, by, cx, cy):
    """Angle at vertex B between rays BA and BC."""
    va = (ax-bx, ay-by); vc = (cx-bx, cy-by)
    dot = va[0]*vc[0] + va[1]*vc[1]
    mag = math.sqrt(va[0]**2+va[1]**2) * math.sqrt(vc[0]**2+vc[1]**2)
    if mag < 1e-6: return 180.0
    return math.degrees(math.acos(max(-1, min(1, dot/mag))))

def trunk_lean_angle(pts):
    """Forward lean of torso from vertical (positive = leaning forward)."""
    sh_mx = (pts[SKEL_L_SH][0]+pts[SKEL_R_SH][0])/2
    sh_my = (pts[SKEL_L_SH][1]+pts[SKEL_R_SH][1])/2
    hi_mx = (pts[SKEL_L_HIP][0]+pts[SKEL_R_HIP][0])/2
    hi_my = (pts[SKEL_L_HIP][1]+pts[SKEL_R_HIP][1])/2
    dx = sh_mx - hi_mx; dy = hi_my - sh_my  # dy positive = shoulder above hip
    return math.degrees(math.atan2(dx, dy))

def shank_angle(kx, ky, ax, ay):
    """Angle between shin (knee→ankle) and vertical [0,1]. 0° = perfectly vertical."""
    shin_x = ax - kx; shin_y = ay - ky
    mag = math.sqrt(shin_x**2 + shin_y**2)
    if mag < 1e-6: return 0.0
    return math.degrees(math.acos(max(-1, min(1, shin_y/mag))))

def compute_angles(pts):
    """Returns dict of angles from smoothed (normalized) skeleton points."""
    trunk = trunk_lean_angle(pts)

    knee_l = three_point_angle(
        pts[SKEL_L_HIP][0], pts[SKEL_L_HIP][1],
        pts[SKEL_L_KN][0],  pts[SKEL_L_KN][1],
        pts[SKEL_L_AN][0],  pts[SKEL_L_AN][1],
    )
    knee_r = three_point_angle(
        pts[SKEL_R_HIP][0], pts[SKEL_R_HIP][1],
        pts[SKEL_R_KN][0],  pts[SKEL_R_KN][1],
        pts[SKEL_R_AN][0],  pts[SKEL_R_AN][1],
    )
    shank_l = shank_angle(
        pts[SKEL_L_KN][0], pts[SKEL_L_KN][1],
        pts[SKEL_L_AN][0], pts[SKEL_L_AN][1],
    )
    shank_r = shank_angle(
        pts[SKEL_R_KN][0], pts[SKEL_R_KN][1],
        pts[SKEL_R_AN][0], pts[SKEL_R_AN][1],
    )
    elbow_l = three_point_angle(
        pts[SKEL_L_SH][0], pts[SKEL_L_SH][1],
        pts[SKEL_L_EL][0], pts[SKEL_L_EL][1],
        pts[SKEL_L_WR][0], pts[SKEL_L_WR][1],
    )
    elbow_r = three_point_angle(
        pts[SKEL_R_SH][0], pts[SKEL_R_SH][1],
        pts[SKEL_R_EL][0], pts[SKEL_R_EL][1],
        pts[SKEL_R_WR][0], pts[SKEL_R_WR][1],
    )

    # use better-visibility elbow
    l_vis = (pts[SKEL_L_SH][2]+pts[SKEL_L_EL][2]+pts[SKEL_L_WR][2])/3
    r_vis = (pts[SKEL_R_SH][2]+pts[SKEL_R_EL][2]+pts[SKEL_R_WR][2])/3
    elbow = elbow_l if l_vis >= r_vis else elbow_r

    return dict(trunk=trunk, knee_l=knee_l, knee_r=knee_r,
                shank_l=shank_l, shank_r=shank_r, elbow=elbow)

HIGH_VIS = 0.55   # confident: full draw
LOW_VIS  = 0.30   # minimum: draw dimmed

# ── AngleTracker: rolling peak/min for stance-phase assessment ────────────────
class AngleTracker:
    """
    Knee assessment needs PEAK extension (heel-strike angle, max over window).
    Shank assessment needs MIN angle (most vertical, min over window).
    Window=20 frames @ 10fps = 2s, covering ~2-4 strides.
    """
    def __init__(self, window=20):
        self.knee_l  = deque(maxlen=window)
        self.knee_r  = deque(maxlen=window)
        self.shank_l = deque(maxlen=window)
        self.shank_r = deque(maxlen=window)

    def update(self, angles):
        self.knee_l.append(angles['knee_l'])
        self.knee_r.append(angles['knee_r'])
        self.shank_l.append(angles['shank_l'])
        self.shank_r.append(angles['shank_r'])

    def peak_knee(self, side): return max(self.knee_l if side=='l' else self.knee_r, default=0)
    def min_shank(self, side): return min(self.shank_l if side=='l' else self.shank_r, default=0)

# ── Drawing helpers ────────────────────────────────────────────────────────────
def to_px(p, w, h): return (int(p[0]*w), int(p[1]*h))
def vis_ok(p): return p[2] >= LOW_VIS

def seg(frame, a, b, color, thick=3):
    if a[2] < LOW_VIS or b[2] < LOW_VIS: return
    min_vis = min(a[2], b[2])
    if min_vis < HIGH_VIS:
        dim = tuple(int(c * 0.45) for c in color)
        cv2.line(frame, a[:2], b[:2], dim, max(1, thick-1), cv2.LINE_AA)
        return
    cv2.line(frame, a[:2], b[:2], color, thick, cv2.LINE_AA)

def joint(frame, p, color, r=6):
    vis = p[2]
    if vis < LOW_VIS: return
    if vis < HIGH_VIS:
        # hollow circle: indicates uncertain landmark position
        cv2.circle(frame, p[:2], r, tuple(int(c*0.5) for c in color), 1, cv2.LINE_AA)
        return
    cv2.circle(frame, p[:2], r,   C_JOINT_BG,  -1, cv2.LINE_AA)
    cv2.circle(frame, p[:2], r,   C_JOINT_STK,  2, cv2.LINE_AA)
    cv2.circle(frame, p[:2], r-2, color,        -1, cv2.LINE_AA)

def draw_strike_arrow(frame, x, y, color, age_frames, max_age=8):
    """GaitKeeper-style downward strike arrow, alpha fades with age."""
    alpha = max(0.0, 1.0 - age_frames/max_age)
    if alpha <= 0: return
    x = int(x); y = int(y)
    overlay = frame.copy()
    arrow_h = 50; arrow_w = 22; tip_y = y + 12
    cv2.line(overlay, (x, tip_y-arrow_h), (x, tip_y-12), color, 4, cv2.LINE_AA)
    tri = np.array([[x, tip_y],[x-arrow_w//2, tip_y-16],[x+arrow_w//2, tip_y-16]])
    cv2.fillPoly(overlay, [tri], color)
    cv2.addWeighted(overlay, alpha, frame, 1-alpha, 0, frame)

def assess_knee(peak):
    """Assess based on peak extension angle (rolling max ≈ heel-strike angle)."""
    if peak > 155: return 'Good'
    elif peak >= 140: return 'OK'
    return 'Bad'

def assess_shank(min_a):
    """Assess based on min shank angle (rolling min ≈ most vertical = good form)."""
    if min_a <= 15: return 'Good'
    elif min_a <= 25: return 'OK'
    return 'Bad'

def assess_trunk(a):
    a = abs(a)
    if a < 1: return 'Bad'
    elif a < 5: return 'OK'
    elif a <= 15: return 'Good'
    elif a <= 20: return 'OK'
    return 'Bad'

def assess_elbow(a):
    if a < 50: return 'Bad'
    elif a < 60: return 'OK'
    elif a <= 100: return 'Good'
    elif a <= 110: return 'OK'
    return 'Bad'

def leg_vis(pts, hip_i, kn_i, an_i):
    """Average visibility of hip+knee+ankle for one side."""
    return (pts[hip_i][2] + pts[kn_i][2] + pts[an_i][2]) / 3

def draw_angle_panel(frame, angles, tracker, pts, strikes_l, strikes_r):
    """
    Real-time angles with assessment based on rolling peak (knee) / min (shank).
    Low-visibility joints flagged with '?' — don't trust those angles.
    """
    pad = 10; line_h = 21; panel_w = 290

    peak_kl = tracker.peak_knee('l');  peak_kr = tracker.peak_knee('r')
    min_sl  = tracker.min_shank('l'); min_sr  = tracker.min_shank('r')

    vis_l = leg_vis(pts, SKEL_L_HIP, SKEL_L_KN, SKEL_L_AN)
    vis_r = leg_vis(pts, SKEL_R_HIP, SKEL_R_KN, SKEL_R_AN)
    flag_l = ' ' if vis_l >= HIGH_VIS else '?'
    flag_r = ' ' if vis_r >= HIGH_VIS else '?'

    step_l = "[*]" if strikes_l < 3 else "[ ]"
    step_r = "[*]" if strikes_r < 3 else "[ ]"

    lines = [
        (f"Trunk:   {angles['trunk']:+.1f}deg  {assess_trunk(angles['trunk'])}",         C_TORSO),
        (f"Elbow:   {angles['elbow']:.1f}deg  {assess_elbow(angles['elbow'])}",           C_ARM),
        (f"KneeL:{flag_l}{angles['knee_l']:5.1f}deg  {assess_knee(peak_kl)} (pk:{peak_kl:.0f})",  C_LEFT_LEG),
        (f"KneeR:{flag_r}{angles['knee_r']:5.1f}deg  {assess_knee(peak_kr)} (pk:{peak_kr:.0f})",  C_RIGHT_LEG),
        (f"ShankL:{flag_l}{angles['shank_l']:4.1f}deg  {assess_shank(min_sl)} (mn:{min_sl:.0f})", C_LEFT_LEG),
        (f"ShankR:{flag_r}{angles['shank_r']:4.1f}deg  {assess_shank(min_sr)} (mn:{min_sr:.0f})", C_RIGHT_LEG),
        (f"Step L:{step_l}  Step R:{step_r}",                                             C_JOINT_STK),
    ]
    panel_h = pad*2 + line_h * len(lines)
    overlay = frame.copy()
    cv2.rectangle(overlay, (0, 0), (panel_w, panel_h), (0, 0, 0), -1)
    cv2.addWeighted(overlay, 0.55, frame, 0.45, 0, frame)
    for i, (text, color) in enumerate(lines):
        y = pad + (i+1)*line_h
        cv2.putText(frame, text, (pad, y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.41, color, 1, cv2.LINE_AA)

# ── Main ───────────────────────────────────────────────────────────────────────
def main():
    VIDEO_IN  = "docs/test-sample/running_test.mov"
    VIDEO_OUT = "docs/test-sample/running_test_analyzed_gaitkeeper.mp4"
    MODEL     = "android/app/src/main/assets/pose_landmarker_lite.task"
    TARGET_FPS = 10

    cap = cv2.VideoCapture(VIDEO_IN)
    if not cap.isOpened(): raise RuntimeError(f"Cannot open: {VIDEO_IN}")

    native_fps   = cap.get(cv2.CAP_PROP_FPS) or 30.0
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    vid_w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    vid_h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    step  = max(1, int(native_fps/TARGET_FPS))
    print(f"Video: {vid_w}×{vid_h}, {native_fps:.1f}fps, {total_frames} frames, step={step}")

    options = mp.tasks.vision.PoseLandmarkerOptions(
        base_options=mp.tasks.BaseOptions(model_asset_path=MODEL),
        running_mode=mp.tasks.vision.RunningMode.VIDEO,
        num_poses=1,
        min_pose_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    )

    # Pass 1: collect landmarks
    all_frames = []
    cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
    print("Pass 1: extracting landmarks…")
    with mp.tasks.vision.PoseLandmarker.create_from_options(options) as lmk:
        fi = 0
        while True:
            ret, bgr = cap.read()
            if not ret: break
            if fi % step == 0:
                t_ms = int(fi/native_fps*1000)
                res  = lmk.detect_for_video(
                    mp.Image(image_format=mp.ImageFormat.SRGB,
                             data=cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)), t_ms)
                if res.pose_landmarks:
                    lm  = res.pose_landmarks[0]
                    pts = [(lm[i].x, lm[i].y, lm[i].visibility) for i in KEY_INDICES]
                    all_frames.append((t_ms, bgr.copy(), pts))
            fi += 1
    print(f"  {len(all_frames)} frames collected")

    # Pass 2: smooth + strike detection + render
    smoother   = SkeletonSmoother(n=len(KEY_INDICES))
    detector_l = FootStrikeDetector()
    detector_r = FootStrikeDetector()
    tracker    = AngleTracker(window=20)

    l_strike_age = 999; r_strike_age = 999
    frozen_l = {'knee': 0.0, 'shank': 0.0}
    frozen_r = {'knee': 0.0, 'shank': 0.0}

    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out    = cv2.VideoWriter(VIDEO_OUT, fourcc, TARGET_FPS, (vid_w, vid_h))
    print("Pass 2: rendering…")

    for t_ms, bgr, raw_pts in all_frames:
        pts = smoother.smooth(raw_pts, t_ms)

        def px(idx): return (int(pts[idx][0]*vid_w), int(pts[idx][1]*vid_h), pts[idx][2])
        ppts = [px(i) for i in range(len(pts))]

        # running-form-analyzer strike detection (normalized Y)
        la_y = pts[SKEL_L_AN][1]; la_v = pts[SKEL_L_AN][2]
        ra_y = pts[SKEL_R_AN][1]; ra_v = pts[SKEL_R_AN][2]

        # Compute angles and update tracker
        angles = compute_angles(pts)
        tracker.update(angles)

        if detector_l.update(la_y, la_v, t_ms):
            l_strike_age = 0
            frozen_l['knee']  = angles['knee_l']
            frozen_l['shank'] = angles['shank_l']
        if detector_r.update(ra_y, ra_v, t_ms):
            r_strike_age = 0
            frozen_r['knee']  = angles['knee_r']
            frozen_r['shank'] = angles['shank_r']

        frame = bgr.copy()

        # ── Skeleton ───────────────────────────────────────────────────────────
        for a,b in EDGES_TORSO:
            seg(frame, ppts[a], ppts[b], C_TORSO, 3)
        for a,b in EDGES_L_ARM: seg(frame, ppts[a], ppts[b], C_ARM, 3)
        for a,b in EDGES_R_ARM: seg(frame, ppts[a], ppts[b], C_ARM, 3)
        for a,b in EDGES_L_LEG: seg(frame, ppts[a], ppts[b], C_LEFT_LEG, 4)
        for a,b in EDGES_R_LEG: seg(frame, ppts[a], ppts[b], C_RIGHT_LEG, 4)

        for i,p in enumerate(ppts):
            if   i in (SKEL_L_HIP, SKEL_L_KN, SKEL_L_AN): c = C_LEFT_LEG
            elif i in (SKEL_R_HIP, SKEL_R_KN, SKEL_R_AN): c = C_RIGHT_LEG
            elif i in (SKEL_L_SH, SKEL_L_EL, SKEL_L_WR,
                       SKEL_R_SH, SKEL_R_EL, SKEL_R_WR):  c = C_ARM
            else: c = C_TORSO
            joint(frame, p, c, r=6)

        # ── Strike arrows ──────────────────────────────────────────────────────
        if la_v >= SCORE_THRESH and l_strike_age < 8:
            draw_strike_arrow(frame, ppts[SKEL_L_AN][0], ppts[SKEL_L_AN][1]-15,
                              C_LEFT_LEG, l_strike_age)
        if ra_v >= SCORE_THRESH and r_strike_age < 8:
            draw_strike_arrow(frame, ppts[SKEL_R_AN][0], ppts[SKEL_R_AN][1]-15,
                              C_RIGHT_LEG, r_strike_age)

        # ── Angle panel ────────────────────────────────────────────────────────
        draw_angle_panel(frame, angles, tracker, pts, l_strike_age, r_strike_age)

        # ── Timestamp ──────────────────────────────────────────────────────────
        cv2.putText(frame, f"t={t_ms}ms",
            (vid_w-110, vid_h-10), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (180,180,180), 1, cv2.LINE_AA)

        l_strike_age += 1; r_strike_age += 1
        out.write(frame)

    cap.release(); out.release()
    print(f"\n✓ Written: {VIDEO_OUT}")

if __name__ == "__main__":
    main()
