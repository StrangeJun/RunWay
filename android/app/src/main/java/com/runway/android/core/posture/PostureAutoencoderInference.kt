package com.runway.android.core.posture

// Phase P-2: replace PostureRuleEngine with this class once posture_autoencoder.tflite is trained.
// Training pipeline: tools/posture-model-training/train/train_autoencoder.py
// Swap in PostureModule.kt: bind PostureAutoencoderInference instead of PostureRuleEngine.
//
// Architecture: Dense autoencoder trained ONLY on good-posture sequences.
//   Encoder: Dense(32,relu) → Dense(16,relu) → latent(8)
//   Decoder: Dense(16,relu) → Dense(32,relu) → Dense(5,linear)
// Reconstruction error (MSE per dimension) → per-category score.
class PostureAutoencoderInference : PostureEvaluator {

    override fun evaluate(frames: List<PostureFrameAngles>): PostureResult {
        throw UnsupportedOperationException(
            "PostureAutoencoderInference requires posture_autoencoder.tflite. " +
            "Use PostureRuleEngine until Phase P-2 model is ready."
        )
    }
}
