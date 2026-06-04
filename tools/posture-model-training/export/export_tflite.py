"""
Phase P-2 — .h5 Keras 모델 → .tflite 변환

사용법:
    python export_tflite.py \
      --model ./posture_autoencoder.h5 \
      --output ../assets/posture_autoencoder.tflite \
      [--quantize]   # int8 양자화 (모델 크기 ~5MB 목표)

출력 파일을 android/app/src/main/assets/에 복사하면 앱에서 로드 가능.
PostureModule.kt에서 PostureAutoencoderInference를 바인딩하도록 변경 필요.
"""

import argparse
import numpy as np
import tensorflow as tf


def export(model_path: str, output_path: str, quantize: bool = False):
    model = tf.keras.models.load_model(model_path)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    if quantize:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.int8]
    tflite_model = converter.convert()
    with open(output_path, "wb") as f:
        f.write(tflite_model)
    size_mb = len(tflite_model) / (1024 * 1024)
    print(f"Exported {output_path}  ({size_mb:.2f} MB)")
    if size_mb > 5.0:
        print("WARNING: model exceeds 5MB target. Consider --quantize flag.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--quantize", action="store_true")
    args = parser.parse_args()
    export(args.model, args.output, args.quantize)


if __name__ == "__main__":
    main()
