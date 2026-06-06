package com.runway.android.core.posture

import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Aggregates the metrics produced by the running-form-analyzer port.
 *
 * The upstream project returns Good / Need Improvement / Bad rather than a
 * numeric overall score. Pathfinder converts those states into a continuous
 * score so values within the ideal range receive 90-100 points.
 */
class PostureRuleEngine @Inject constructor() : PostureEvaluator {

    override fun evaluate(frames: List<PostureFrameAngles>): PostureResult {
        if (frames.isEmpty()) return emptyResult()

        val strikeKnees = buildList {
            frames.forEach { frame ->
                if (frame.leftFootStrike) frame.leftKneeAngle?.let(::add)
                if (frame.rightFootStrike) frame.rightKneeAngle?.let(::add)
                if (frame.isLandingFrame && !frame.leftFootStrike && !frame.rightFootStrike) {
                    add(frame.kneeFlexAngle)
                }
            }
        }
        val strikeHipAnkle = buildList {
            frames.forEach { frame ->
                if (frame.leftFootStrike) frame.leftHipAnkleAngle?.let(::add)
                if (frame.rightFootStrike) frame.rightHipAnkleAngle?.let(::add)
                if (frame.isLandingFrame && !frame.leftFootStrike && !frame.rightFootStrike) {
                    add(frame.overstrideRatio)
                }
            }
        }
        val strikeShanks = buildList {
            frames.forEach { frame ->
                if (frame.leftFootStrike) frame.leftShankAngle?.let(::add)
                if (frame.rightFootStrike) frame.rightShankAngle?.let(::add)
                if (frame.isLandingFrame && !frame.leftFootStrike && !frame.rightFootStrike) {
                    add(frame.shankAngle)
                }
            }
        }

        val kneeValue = strikeKnees.lastOrNull()
            ?: frames.map { it.kneeFlexAngle }.median()
        val trunkValue = frames.map { abs(it.trunkLeanAngle) }.median()
        val elbowValue = frames.map { it.elbowAngle }.median()
        val hipSwingValue = maxCompletedSwing(frames)
        val hipAnkleValue = strikeHipAnkle.lastOrNull()
            ?: frames.map { it.overstrideRatio }.median()
        val shankValue = strikeShanks.lastOrNull() ?: 0f

        val knee = assessKnee(kneeValue)
        val trunk = assessTrunk(trunkValue)
        val elbow = assessElbow(elbowValue)
        val hip = assessHipSwing(hipSwingValue)
        val footStrike = assessHipAnkle(hipAnkleValue, shankValue, strikeHipAnkle.isNotEmpty())
        val cadence = assessCadence(frames)
        val verticalOscillation = assessVerticalOscillation(frames)

        val scored = listOf(knee, trunk, elbow, hip)
        val overall = scored.map { it.score }.average().roundToInt()
        val grade = PostureResult.gradeFrom(overall)
        val weakest = listOf(
            "무릎 각도" to knee,
            "상체 기울기" to trunk,
            "팔꿈치 각도" to elbow,
            "고관절 가동범위" to hip,
        ).minBy { it.second.score }

        return PostureResult(
            overallScore = overall,
            grade = grade,
            overallFeedback = when {
                overall >= 90 -> "원본 분석 기준에서 전반적으로 좋은 자세입니다."
                overall >= 60 -> "${weakest.first} 항목을 우선 개선해보세요."
                else -> "${weakest.first} 항목에 지속적인 교정이 필요합니다."
            },
            knee = knee,
            trunk = trunk,
            elbow = elbow,
            hip = hip,
            overstride = footStrike,
            cadence = cadence,
            verticalOscillation = verticalOscillation,
        )
    }

    private fun assessKnee(value: Float): PostureCategoryResult = category(
        value = value,
        idealMin = 135f,
        idealMax = 180f,
        unit = "°",
        assessment = when {
            value > 135f -> Assessment.GOOD
            value >= 126f -> Assessment.NEEDS_IMPROVEMENT
            else -> Assessment.BAD
        },
        good = "착지 순간 무릎 각도가 좋습니다.",
        needsImprovement = "착지 순간 무릎 각도가 조금 작습니다.",
        bad = "착지 순간 무릎이 과도하게 굽혀집니다.",
    )

    private fun assessTrunk(value: Float): PostureCategoryResult = category(
        value = value,
        idealMin = 5f,
        idealMax = 15f,
        unit = "°",
        assessment = when {
            value in 5f..15f -> Assessment.GOOD
            value in 1f..<5f || value in 15f..17f -> Assessment.NEEDS_IMPROVEMENT
            else -> Assessment.BAD
        },
        good = "상체 기울기가 좋습니다.",
        needsImprovement = "상체 기울기를 5~15도로 조정해보세요.",
        bad = "상체 기울기가 권장 범위를 크게 벗어났습니다.",
    )

    private fun assessElbow(value: Float): PostureCategoryResult = category(
        value = value,
        idealMin = 60f,
        idealMax = 90f,
        unit = "°",
        assessment = when {
            value in 60f..90f -> Assessment.GOOD
            value in 53f..<60f || value in 90f..95f -> Assessment.NEEDS_IMPROVEMENT
            else -> Assessment.BAD
        },
        good = "팔꿈치 각도가 좋습니다.",
        needsImprovement = "팔꿈치 각도를 60~90도로 조정해보세요.",
        bad = "팔꿈치 각도가 권장 범위를 크게 벗어났습니다.",
    )

    private fun assessHipSwing(value: Float): PostureCategoryResult = category(
        value = value,
        idealMin = 29f,
        idealMax = 41f,
        unit = "°",
        assessment = when {
            value in 29f..<41f -> Assessment.GOOD
            value >= 41f -> Assessment.NEEDS_IMPROVEMENT
            else -> Assessment.BAD
        },
        good = "고관절 스윙 범위가 좋습니다.",
        needsImprovement = "고관절 스윙 범위가 다소 큽니다.",
        bad = "고관절 스윙 범위가 부족합니다.",
    )

    private fun assessHipAnkle(
        value: Float,
        shankValue: Float,
        hasStrike: Boolean,
    ): PostureCategoryResult {
        if (!hasStrike) {
            return PostureCategoryResult(
                score = 20,
                measuredValue = value,
                idealMin = 0f,
                idealMax = 15f,
                unit = "°",
                feedback = "원본 방식의 착지 이벤트를 감지하지 못했습니다.",
                tip = "전신이 보이는 측면 영상을 10초 이상 촬영하세요.",
            )
        }
        val hipAnkleAssessment = when {
            value in 0f..15f -> Assessment.GOOD
            value <= 20f -> Assessment.NEEDS_IMPROVEMENT
            else -> Assessment.BAD
        }
        val shankAssessment = when {
            shankValue in 0f..10f -> Assessment.GOOD
            shankValue <= 15f -> Assessment.NEEDS_IMPROVEMENT
            else -> Assessment.BAD
        }
        val assessment = minOf(hipAnkleAssessment, shankAssessment)
        return category(
            value = value,
            idealMin = 0f,
            idealMax = 15f,
            unit = "°",
            assessment = assessment,
            good = "착지 순간 발이 골반 아래에 가깝고 정강이 각도가 좋습니다.",
            needsImprovement = "착지 위치나 정강이 각도를 조금 조정해보세요.",
            bad = "착지 위치가 골반에서 멀거나 정강이가 과도하게 기울어져 있습니다.",
        )
    }

    private fun assessCadence(frames: List<PostureFrameAngles>): PostureCategoryResult {
        val spm = cadenceFromSingleLegCycle(frames)
            ?: cadenceFromAlternatingAnkles(frames)
            ?: cadenceFromStrikeEvents(frames)
        if (spm == null) {
            return PostureCategoryResult(
                0, 0f, 170f, 180f, "spm",
                "케이던스 측정을 위한 좌우 다리 주기가 부족합니다.", "",
            )
        }
        val assessment = when {
            spm in 170f..180f -> Assessment.GOOD
            spm in 160f..<170f || spm in 180f..190f -> Assessment.NEEDS_IMPROVEMENT
            else -> Assessment.BAD
        }
        return category(
            spm, 170f, 180f, "spm", assessment,
            "케이던스가 좋습니다.",
            "케이던스를 170~180spm에 가깝게 조정해보세요.",
            "케이던스가 권장 범위에서 크게 벗어났습니다.",
        )
    }

    private fun cadenceFromSingleLegCycle(frames: List<PostureFrameAngles>): Float? {
        val left = relativeAnkleSignal(frames, SKEL_L_HIP, SKEL_L_ANKLE)
        val right = relativeAnkleSignal(frames, SKEL_R_HIP, SKEL_R_ANKLE)
        val signal = listOf(left, right)
            .filter { it.size >= MIN_CADENCE_SIGNAL_FRAMES }
            .maxByOrNull { it.size } ?: return null

        val smoothed = signal.indices.map { index ->
            val from = (index - 1).coerceAtLeast(0)
            val to = (index + 1).coerceAtMost(signal.lastIndex)
            signal[index].first to (from..to).map { signal[it].second }.average().toFloat()
        }
        val values = smoothed.map { it.second }
        val range = (values.maxOrNull() ?: 0f) - (values.minOrNull() ?: 0f)
        if (range < MIN_ANKLE_SWING_RANGE) return null
        val peakThreshold = (values.maxOrNull() ?: 0f) - range * 0.35f

        val peaks = mutableListOf<Long>()
        for (index in 1 until smoothed.lastIndex) {
            val previous = smoothed[index - 1].second
            val current = smoothed[index].second
            val next = smoothed[index + 1].second
            val timestamp = smoothed[index].first
            if (current >= peakThreshold && current > previous && current >= next) {
                if (peaks.isEmpty() || timestamp - peaks.last() >= MIN_STRIDE_INTERVAL_MS) {
                    peaks += timestamp
                }
            }
        }

        val strideIntervals = peaks.zipWithNext { first, second -> second - first }
            .filter { it in MIN_STRIDE_INTERVAL_MS..MAX_STRIDE_INTERVAL_MS }
        if (strideIntervals.size < 2) return null
        return 120_000f / strideIntervals.medianLong()
    }

    private fun relativeAnkleSignal(
        frames: List<PostureFrameAngles>,
        hipIndex: Int,
        ankleIndex: Int,
    ): List<Pair<Long, Float>> = frames.mapNotNull { frame ->
        val hip = frame.landmarks.getOrNull(hipIndex)
        val ankle = frame.landmarks.getOrNull(ankleIndex)
        if (hip == null || ankle == null || minOf(hip.v, ankle.v) < 0.3f) {
            null
        } else {
            frame.timestampMs to (ankle.x - hip.x)
        }
    }

    /**
     * Each sign change of leftAnkleY - rightAnkleY represents the next leg
     * taking over in the gait cycle. Median transition spacing is robust to a
     * missed frame and does not depend on how much of the video was analyzed.
     */
    private fun cadenceFromAlternatingAnkles(frames: List<PostureFrameAngles>): Float? {
        val signals = frames.mapNotNull { frame ->
            val left = frame.landmarks.getOrNull(SKEL_L_ANKLE)
            val right = frame.landmarks.getOrNull(SKEL_R_ANKLE)
            if (left == null || right == null || left.v < 0.3f || right.v < 0.3f) {
                null
            } else {
                frame.timestampMs to (left.y - right.y)
            }
        }
        if (signals.size < 6) return null

        val smoothed = signals.indices.map { index ->
            val from = (index - 1).coerceAtLeast(0)
            val to = (index + 1).coerceAtMost(signals.lastIndex)
            val value = (from..to).map { signals[it].second }.average().toFloat()
            signals[index].first to value
        }

        val range = (smoothed.maxOf { it.second } - smoothed.minOf { it.second })
        if (range < 0.03f) return null
        val hysteresis = (range * 0.12f).coerceIn(0.008f, 0.04f)
        val transitions = mutableListOf<Long>()
        var phase = 0

        smoothed.forEach { (timestamp, value) ->
            when {
                value >= hysteresis && phase <= 0 -> {
                    if (phase < 0) transitions += timestamp
                    phase = 1
                }
                value <= -hysteresis && phase >= 0 -> {
                    if (phase > 0) transitions += timestamp
                    phase = -1
                }
            }
        }

        val stepIntervals = transitions.zipWithNext { first, second -> second - first }
            .filter { it in MIN_STEP_INTERVAL_MS..MAX_STEP_INTERVAL_MS }
        if (stepIntervals.size < 2) return null
        return 60_000f / stepIntervals.medianLong()
    }

    private fun cadenceFromStrikeEvents(frames: List<PostureFrameAngles>): Float? {
        val timestamps = buildList {
            frames.forEach { frame ->
                if (frame.leftFootStrike) add(frame.timestampMs)
                if (frame.rightFootStrike) add(frame.timestampMs)
            }
        }.sorted()
        val intervals = timestamps.zipWithNext { first, second -> second - first }
            .filter { it in MIN_STEP_INTERVAL_MS..MAX_STEP_INTERVAL_MS }
        if (intervals.size < 2) return null
        return 60_000f / intervals.medianLong()
    }

    private fun assessVerticalOscillation(frames: List<PostureFrameAngles>): PostureCategoryResult {
        if (frames.size < 10) {
            return PostureCategoryResult(
                0, 0f, 0f, 6.5f, "%",
                "수직진폭 측정을 위한 프레임이 부족합니다.", "",
            )
        }
        val hipWindow = frames.map { it.hipMidY }
        val movingAverages = hipWindow.windowed(5).map { it.average().toFloat() }
        val oscillation = ((movingAverages.maxOrNull() ?: 0f) -
            (movingAverages.minOrNull() ?: 0f)) / 2f * 100f
        val assessment = when {
            oscillation < 6.5f -> Assessment.GOOD
            oscillation < 8f -> Assessment.NEEDS_IMPROVEMENT
            else -> Assessment.BAD
        }
        return category(
            oscillation, 0f, 6.5f, "%", assessment,
            "수직진폭이 좋습니다.",
            "수직진폭을 조금 줄여보세요.",
            "수직진폭이 커서 에너지 손실이 발생할 수 있습니다.",
        )
    }

    private fun maxCompletedSwing(frames: List<PostureFrameAngles>): Float {
        val leftAngles = frames.mapNotNull { it.leftHipAngle }
        val rightAngles = frames.mapNotNull { it.rightHipAngle }
        if (leftAngles.isNotEmpty() || rightAngles.isNotEmpty()) {
            return maxOf(
                completedSwing(leftAngles),
                completedSwing(rightAngles),
            )
        }
        return frames.maxOfOrNull { abs(it.hipExtensionAngle) } ?: 0f
    }

    private fun completedSwing(values: List<Float>): Float {
        var previous = 0f
        var direction = 0
        var currentForward = 0f
        var currentBackward = 0f
        var completedForward = 0f
        var completedBackward = 0f

        values.forEach { value ->
            when {
                value > previous -> {
                    if (direction == -1) {
                        completedBackward = currentBackward
                        currentBackward = 0f
                    }
                    direction = 1
                    currentForward = maxOf(currentForward, value)
                }
                value < previous -> {
                    if (direction == 1) {
                        completedForward = currentForward
                        currentForward = 0f
                    }
                    direction = -1
                    currentBackward = maxOf(currentBackward, abs(value))
                }
            }
            previous = value
        }
        return maxOf(completedForward, completedBackward)
    }

    private fun category(
        value: Float,
        idealMin: Float,
        idealMax: Float,
        unit: String,
        assessment: Assessment,
        good: String,
        needsImprovement: String,
        bad: String,
    ) = PostureCategoryResult(
        score = categoryScore(value, idealMin, idealMax, assessment),
        measuredValue = value,
        idealMin = idealMin,
        idealMax = idealMax,
        unit = unit,
        feedback = when (assessment) {
            Assessment.GOOD -> good
            Assessment.NEEDS_IMPROVEMENT -> needsImprovement
            Assessment.BAD -> bad
        },
        tip = "",
    )

    private fun categoryScore(
        value: Float,
        idealMin: Float,
        idealMax: Float,
        assessment: Assessment,
    ): Int {
        val range = (idealMax - idealMin).coerceAtLeast(1f)
        return when (assessment) {
            Assessment.GOOD -> {
                val center = (idealMin + idealMax) / 2f
                val halfRange = range / 2f
                val closeness = (1f - abs(value - center) / halfRange).coerceIn(0f, 1f)
                (90f + closeness * 10f).roundToInt()
            }
            Assessment.NEEDS_IMPROVEMENT -> {
                val distance = when {
                    value < idealMin -> idealMin - value
                    value > idealMax -> value - idealMax
                    else -> 0f
                }
                (89f - distance / range * 24f).roundToInt().coerceIn(65, 89)
            }
            Assessment.BAD -> {
                val distance = minOf(abs(value - idealMin), abs(value - idealMax))
                (64f - distance / range * 34f).roundToInt().coerceIn(30, 64)
            }
        }
    }

    private fun List<Float>.median(): Float {
        if (isEmpty()) return 0f
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2f
        } else {
            sorted[middle]
        }
    }

    private fun List<Long>.medianLong(): Float {
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle].toFloat()
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2f
        }
    }

    private fun emptyResult() = PostureResult(
        overallScore = 0,
        grade = "D",
        overallFeedback = "분석 가능한 프레임이 없습니다. 다시 촬영해주세요.",
        knee = PostureCategoryResult(0, 0f, 135f, 180f, "°", "분석 불가", ""),
        trunk = PostureCategoryResult(0, 0f, 5f, 15f, "°", "분석 불가", ""),
        elbow = PostureCategoryResult(0, 0f, 60f, 90f, "°", "분석 불가", ""),
        hip = PostureCategoryResult(0, 0f, 29f, 41f, "°", "분석 불가", ""),
        overstride = PostureCategoryResult(0, 0f, 0f, 15f, "°", "분석 불가", ""),
        cadence = PostureCategoryResult(0, 0f, 170f, 180f, "spm", "분석 불가", ""),
        verticalOscillation = PostureCategoryResult(0, 0f, 0f, 6.5f, "%", "분석 불가", ""),
    )

    private enum class Assessment {
        BAD,
        NEEDS_IMPROVEMENT,
        GOOD,
    }

    private companion object {
        const val MIN_STEP_INTERVAL_MS = 250L
        const val MAX_STEP_INTERVAL_MS = 1_000L
        const val MIN_STRIDE_INTERVAL_MS = 450L
        const val MAX_STRIDE_INTERVAL_MS = 1_500L
        const val MIN_CADENCE_SIGNAL_FRAMES = 12
        const val MIN_ANKLE_SWING_RANGE = 0.05f
    }
}
