"""
Phase P-2 데이터 수집 파이프라인 — Step 1: 영상에서 관절 각도 추출

사용법:
    python extract_landmarks.py --input_dir ./good_posture_videos --output_dir ./raw_csv

출력: 각 영상별 CSV (columns: knee, trunk, elbow, hip, overstride, is_landing, visibility)

필요 패키지:
    pip install mediapipe opencv-python numpy pandas
"""

import argparse
import csv
import math
import os
import cv2
import mediapipe as mp
import numpy as np

MP_POSE = mp.solutions.pose
LANDMARK = mp.solutions.pose.PoseLandmark


def angle_three_points(a, b, c):
    ax, ay = a.x - b.x, a.y - b.y
    cx, cy = c.x - b.x, c.y - b.y
    dot = ax * cx + ay * cy
    mag_a = math.sqrt(ax ** 2 + ay ** 2)
    mag_c = math.sqrt(cx ** 2 + cy ** 2)
    if mag_a < 1e-6 or mag_c < 1e-6:
        return 180.0
    cos_angle = max(-1.0, min(1.0, dot / (mag_a * mag_c)))
    return math.degrees(math.acos(cos_angle))


def trunk_lean(ls, rs, lh, rh):
    sx = (ls.x + rs.x) / 2
    sy = (ls.y + rs.y) / 2
    hx = (lh.x + rh.x) / 2
    hy = (lh.y + rh.y) / 2
    dx = sx - hx
    dy = hy - sy
    return math.degrees(math.atan2(dx, dy))


def extract_from_video(video_path: str, target_fps: int = 7) -> list[dict]:
    cap = cv2.VideoCapture(video_path)
    src_fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    frame_interval = max(1, round(src_fps / target_fps))
    rows = []
    frame_idx = 0
    with MP_POSE.Pose(static_image_mode=False, min_detection_confidence=0.6) as pose:
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
            if frame_idx % frame_interval == 0:
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                result = pose.process(rgb)
                if result.pose_landmarks:
                    lm = result.pose_landmarks.landmark
                    vis_keys = [11, 13, 15, 23, 25, 27]
                    avg_vis = sum(lm[i].visibility for i in vis_keys) / len(vis_keys)
                    if avg_vis >= 0.6:
                        knee = angle_three_points(lm[23], lm[25], lm[27])
                        trunk = trunk_lean(lm[11], lm[12], lm[23], lm[24])
                        elbow = angle_three_points(lm[11], lm[13], lm[15])
                        hip = angle_three_points(lm[11], lm[23], lm[25])
                        lax, rax = lm[27].x, lm[28].x
                        lead_x = max(lax, rax)
                        lead_y = lm[27].y if lax >= rax else lm[28].y
                        hip_x = (lm[23].x + lm[24].x) / 2
                        sh_y = (lm[11].y + lm[12].y) / 2
                        body_h = max(abs(lead_y - sh_y), 0.01)
                        overstride = max(0.0, (lead_x - hip_x) / body_h)
                        hip_y = (lm[23].y + lm[24].y) / 2
                        is_landing = lead_y > 0.65 and lead_y > hip_y
                        rows.append({
                            "knee": knee, "trunk": trunk, "elbow": elbow,
                            "hip": hip, "overstride": overstride,
                            "is_landing": int(is_landing), "visibility": avg_vis,
                        })
            frame_idx += 1
    cap.release()
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input_dir", required=True)
    parser.add_argument("--output_dir", required=True)
    parser.add_argument("--fps", type=int, default=7)
    args = parser.parse_args()
    os.makedirs(args.output_dir, exist_ok=True)
    for fname in os.listdir(args.input_dir):
        if not fname.lower().endswith((".mp4", ".mov", ".avi")):
            continue
        path = os.path.join(args.input_dir, fname)
        rows = extract_from_video(path, args.fps)
        if len(rows) < 30:
            print(f"  SKIP {fname}: only {len(rows)} valid frames")
            continue
        out = os.path.join(args.output_dir, fname.rsplit(".", 1)[0] + ".csv")
        with open(out, "w", newline="") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
            w.writeheader()
            w.writerows(rows)
        print(f"  OK {fname}: {len(rows)} frames → {out}")


if __name__ == "__main__":
    main()
