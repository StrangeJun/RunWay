package com.runway.android.core.posture

interface PostureEvaluator {
    fun evaluate(frames: List<PostureFrameAngles>): PostureResult
}
