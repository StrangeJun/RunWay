package com.runway.android.core.posture

/**
 * Stateful port of running-form-analyzer/metrics/angle_metrics.py and step_metrics.py.
 */
class RunningFormMetricsPipeline {
    private val leftStrikeDetector = RunningFormStrikeDetector()
    private val rightStrikeDetector = RunningFormStrikeDetector()
    private var previous: PostureFrameAngles? = null

    fun process(
        landmarks: List<SkeletonPoint>,
        timestampMs: Long,
        aspectRatio: Float,
    ): PostureFrameAngles? {
        val current = PostureAngleCalculator.compute(landmarks, aspectRatio) ?: return null
        val carried = carryMissingAngles(current, previous)

        val leftStrike = landmarks.getOrNull(SKEL_L_ANKLE)
            ?.let { leftStrikeDetector.update(it, timestampMs) } == true
        val rightStrike = landmarks.getOrNull(SKEL_R_ANKLE)
            ?.let { rightStrikeDetector.update(it, timestampMs) } == true

        return carried.copy(
            timestampMs = timestampMs,
            landmarks = landmarks,
            leftFootStrike = leftStrike,
            rightFootStrike = rightStrike,
            isLandingFrame = leftStrike || rightStrike,
        ).also { previous = it }
    }

    private fun carryMissingAngles(
        current: PostureFrameAngles,
        old: PostureFrameAngles?,
    ): PostureFrameAngles {
        if (old == null) return current
        return current.copy(
            leftKneeAngle = current.leftKneeAngle ?: old.leftKneeAngle,
            rightKneeAngle = current.rightKneeAngle ?: old.rightKneeAngle,
            leftElbowAngle = current.leftElbowAngle ?: old.leftElbowAngle,
            rightElbowAngle = current.rightElbowAngle ?: old.rightElbowAngle,
            leftHipAngle = current.leftHipAngle ?: old.leftHipAngle,
            rightHipAngle = current.rightHipAngle ?: old.rightHipAngle,
            leftHipAnkleAngle = current.leftHipAnkleAngle ?: old.leftHipAnkleAngle,
            rightHipAnkleAngle = current.rightHipAnkleAngle ?: old.rightHipAnkleAngle,
            leftShankAngle = current.leftShankAngle ?: old.leftShankAngle,
            rightShankAngle = current.rightShankAngle ?: old.rightShankAngle,
            leftArmSwingAngle = current.leftArmSwingAngle ?: old.leftArmSwingAngle,
            rightArmSwingAngle = current.rightArmSwingAngle ?: old.rightArmSwingAngle,
        )
    }
}
