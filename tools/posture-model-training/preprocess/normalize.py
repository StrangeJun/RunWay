"""
Phase P-2 데이터 수집 파이프라인 — Step 2: 정규화 및 시퀀스 생성

사용법:
    python normalize.py --input_dir ./raw_csv --output ./dataset/good_posture_sequences.npy

출력: shape [N, T, 5] numpy array (N=시퀀스 수, T=프레임 수, 5=각도 종류)
  columns order: [knee, trunk, elbow, hip, overstride]

정규화 방법:
  - 각 각도 차원을 훈련 세트 평균/표준편차로 z-score 정규화
  - 가시성(visibility) < 0.6 프레임 제외
  - T=70 고정 (짧으면 패딩, 길면 균등 샘플링)
"""

import argparse
import os
import numpy as np
import pandas as pd

TARGET_LEN = 70
COLS = ["knee", "trunk", "elbow", "hip", "overstride"]


def pad_or_sample(arr: np.ndarray, target: int) -> np.ndarray:
    n = len(arr)
    if n == target:
        return arr
    if n < target:
        pad = np.tile(arr[-1:], (target - n, 1))
        return np.concatenate([arr, pad], axis=0)
    indices = np.linspace(0, n - 1, target, dtype=int)
    return arr[indices]


def load_sequences(input_dir: str) -> list[np.ndarray]:
    seqs = []
    for fname in os.listdir(input_dir):
        if not fname.endswith(".csv"):
            continue
        df = pd.read_csv(os.path.join(input_dir, fname))
        df = df[df["visibility"] >= 0.6]
        if len(df) < 30:
            continue
        seq = df[COLS].values.astype(np.float32)
        seqs.append(pad_or_sample(seq, TARGET_LEN))
    return seqs


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input_dir", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--stats_out", default=None,
                        help="Save normalization stats (mean/std) for inference-time use")
    args = parser.parse_args()

    seqs = load_sequences(args.input_dir)
    if not seqs:
        raise ValueError("No valid sequences found")
    arr = np.stack(seqs)  # [N, T, 5]

    mean = arr.reshape(-1, 5).mean(axis=0)
    std = arr.reshape(-1, 5).std(axis=0).clip(min=1e-6)
    arr_norm = (arr - mean) / std

    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    np.save(args.output, arr_norm)
    print(f"Saved {arr_norm.shape} to {args.output}")

    if args.stats_out:
        np.savez(args.stats_out, mean=mean, std=std)
        print(f"Saved normalization stats to {args.stats_out}")


if __name__ == "__main__":
    main()
