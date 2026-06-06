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
    // Bilateral signals for cadence and vertical-oscillation reference metrics
    val hipMidY: Float = 0f,     // avg of left+right hip Y; tracks vertical bounce
    val nearAnkleY: Float = 0f,  // camera-side ankle Y; peaks mark ground contacts
    // running-form-analyzer additions
    val shankAngle: Float = 0f,      // shin angle from vertical at landing (0° = perfectly vertical)
    val armSwingAngle: Float = 0f,   // upper-arm vs torso angle
    val leftKneeAngle: Float? = null,
    val rightKneeAngle: Float? = null,
    val leftElbowAngle: Float? = null,
    val rightElbowAngle: Float? = null,
    val leftHipAngle: Float? = null,
    val rightHipAngle: Float? = null,
    val leftHipAnkleAngle: Float? = null,
    val rightHipAnkleAngle: Float? = null,
    val leftShankAngle: Float? = null,
    val rightShankAngle: Float? = null,
    val leftArmSwingAngle: Float? = null,
    val rightArmSwingAngle: Float? = null,
    val leftFootStrike: Boolean = false,
    val rightFootStrike: Boolean = false,
)
