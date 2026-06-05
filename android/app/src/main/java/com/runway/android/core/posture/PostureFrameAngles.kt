package com.runway.android.core.posture

data class PostureFrameAngles(
    val kneeFlexAngle: Float,
    val trunkLeanAngle: Float,
    val elbowAngle: Float,
    val hipExtensionAngle: Float,
    val overstrideRatio: Float,
    val isLandingFrame: Boolean,
    val visibility: Float,
    val timestampMs: Long = 0L,
    val landmarks: List<SkeletonPoint> = emptyList(),
)
