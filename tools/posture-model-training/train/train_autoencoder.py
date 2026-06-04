"""
Phase P-2 — Autoencoder 학습

아키텍처 (spec section 7):
  Encoder: Dense(32, relu) → Dense(16, relu) → latent(8)
  Decoder: Dense(16, relu) → Dense(32, relu) → Dense(5, linear)
  입력/출력: shape [T, 5] (T=70 고정, 5=각도 종류)

사용법:
    python train_autoencoder.py \
      --data ../dataset/good_posture_sequences.npy \
      --output ./posture_autoencoder.h5

필요 패키지:
    pip install tensorflow numpy
"""

import argparse
import numpy as np
import tensorflow as tf
from tensorflow import keras

INPUT_DIM = 5
LATENT_DIM = 8


def build_autoencoder() -> keras.Model:
    x = inp = keras.Input(shape=(INPUT_DIM,))
    x = keras.layers.Dense(32, activation="relu")(x)
    x = keras.layers.Dense(16, activation="relu")(x)
    x = keras.layers.Dense(LATENT_DIM, name="latent")(x)
    x = keras.layers.Dense(16, activation="relu")(x)
    x = keras.layers.Dense(32, activation="relu")(x)
    x = keras.layers.Dense(INPUT_DIM, activation="linear", name="reconstruction")(x)
    return keras.Model(inp, x, name="posture_autoencoder")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, help="Path to .npy shape [N, T, 5]")
    parser.add_argument("--output", default="posture_autoencoder.h5")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--batch_size", type=int, default=32)
    args = parser.parse_args()

    seqs = np.load(args.data)                        # [N, T, 5]
    n, t, d = seqs.shape
    flat = seqs.reshape(-1, d).astype(np.float32)   # [N*T, 5]
    print(f"Training on {n} sequences ({flat.shape[0]} frame samples, dim={d})")

    model = build_autoencoder()
    model.summary()
    model.compile(optimizer="adam", loss="mse")

    val_split = 0.1
    model.fit(
        flat, flat,
        epochs=args.epochs,
        batch_size=args.batch_size,
        validation_split=val_split,
        callbacks=[
            keras.callbacks.EarlyStopping(patience=10, restore_best_weights=True),
            keras.callbacks.ReduceLROnPlateau(factor=0.5, patience=5),
        ],
    )
    model.save(args.output)
    print(f"Model saved → {args.output}")


if __name__ == "__main__":
    main()
