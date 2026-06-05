package com.runway.android.core.posture

import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PostureRuleEngine @Inject constructor() : PostureEvaluator {

    override fun evaluate(frames: List<PostureFrameAngles>): PostureResult {
        if (frames.isEmpty()) return emptyResult()

        val landing = frames.filter { it.isLandingFrame }
        val hasLanding = landing.isNotEmpty()
        val landingOrAll = if (hasLanding) landing else frames

        val kneeMedian = landingOrAll.map { it.kneeFlexAngle }.median()
        val trunkMedian = frames.map { it.trunkLeanAngle }.median()
        val elbowMedian = frames.map { it.elbowAngle }.median()
        val hipMedian = frames.map { it.hipExtensionAngle }.median()

        val knee = evalKnee(kneeMedian)
        val trunk = evalTrunk(trunkMedian)
        val elbow = evalElbow(elbowMedian)
        val hip = evalHip(hipMedian)

        // Overstride is only reliable from landing frames; without them skip it from the score
        // and redistribute its 20% weight proportionally to the remaining categories.
        val overstride: PostureCategoryResult
        val overall: Int
        if (hasLanding) {
            overstride = evalOverstride(landing.map { it.overstrideRatio }.median())
            overall = (knee.score * 0.30 + trunk.score * 0.25 + overstride.score * 0.20 +
                    elbow.score * 0.15 + hip.score * 0.10).roundToInt()
        } else {
            overstride = PostureCategoryResult(
                score = 50, measuredValue = 0f, idealMin = 0f, idealMax = 0.10f, unit = "%",
                feedback = "착지 프레임이 감지되지 않았습니다. 측면에서 촬영되었는지 확인해주세요.",
                tip = "",
            )
            // Renormalized weights (30/25/15/10 → 37.5/31.25/18.75/12.5)
            overall = (knee.score * 0.375 + trunk.score * 0.3125 +
                    elbow.score * 0.1875 + hip.score * 0.125).roundToInt()
        }

        val grade = PostureResult.gradeFrom(overall)
        val feedback = buildOverallFeedback(overall, knee, trunk, overstride)

        val cadence = evalCadence(frames)
        val verticalOscillation = evalVerticalOscillation(frames)

        return PostureResult(overall, grade, feedback, knee, trunk, elbow, hip, overstride,
            cadence, verticalOscillation)
    }

    private fun evalKnee(angle: Float): PostureCategoryResult {
        // Biomechanically sound landing range for running: 135–165° (hip-knee-ankle).
        // Elite runners commonly land at 130–155°; the old 155–170° was too strict and
        // penalised athletes who use healthy knee flexion for shock absorption.
        val idealMin = 135f; val idealMax = 165f
        val score = angleScore(angle, idealMin, idealMax)
        val feedback = when {
            angle < idealMin - 15f -> "착지 시 무릎이 너무 많이 구부러져 있습니다. 보폭을 조금 줄여보세요."
            angle < idealMin -> "착지 시 무릎이 약간 많이 구부러져 있습니다. 조금 더 펴보세요."
            angle > idealMax + 15f -> "착지 시 무릎이 너무 펴져 있어 충격 흡수가 부족합니다."
            angle > idealMax -> "착지 시 무릎을 살짝 더 구부려 충격을 흡수해보세요."
            else -> "착지 시 무릎 각도가 이상적입니다."
        }
        val tip = if (score < 80) "무릎을 살짝 구부린 상태(135~165°)로 착지하면 관절 충격을 효과적으로 분산시킬 수 있습니다." else ""
        return PostureCategoryResult(score, angle, idealMin, idealMax, "°", feedback, tip)
    }

    private fun evalTrunk(angle: Float): PostureCategoryResult {
        val idealMin = 5f; val idealMax = 10f
        val score = angleScore(angle, idealMin, idealMax)
        val feedback = when {
            angle < 0f -> "상체가 뒤로 기울어져 있습니다. 전방으로 기울여 보세요."
            angle < idealMin -> "상체 기울기가 부족합니다. 약 5~10도 앞으로 기울여 보세요."
            angle > idealMax + 10f -> "상체가 너무 앞으로 기울어져 있어 허리에 부담이 됩니다."
            angle > idealMax -> "상체 기울기가 조금 과합니다."
            else -> "상체 기울기가 이상적입니다."
        }
        val tip = if (score < 80) "전방 기울기는 추진력과 효율을 높여줍니다." else ""
        return PostureCategoryResult(score, angle, idealMin, idealMax, "°", feedback, tip)
    }

    private fun evalElbow(angle: Float): PostureCategoryResult {
        val idealMin = 85f; val idealMax = 95f
        val score = angleScore(angle, idealMin, idealMax)
        val feedback = when {
            angle < idealMin - 15f -> "팔꿈치가 너무 많이 구부러져 있어 경직됩니다."
            angle < idealMin -> "팔꿈치를 약 90도로 유지해보세요."
            angle > idealMax + 25f -> "팔꿈치가 너무 펴져 있어 에너지 손실이 발생합니다."
            angle > idealMax -> "팔꿈치를 약 90도로 줄여보세요."
            else -> "팔꿈치 각도가 이상적입니다."
        }
        val tip = if (score < 80) "팔꿈치를 90도 유지하면 리듬감 있는 팔 스윙이 가능합니다." else ""
        return PostureCategoryResult(score, angle, idealMin, idealMax, "°", feedback, tip)
    }

    private fun evalHip(angle: Float): PostureCategoryResult {
        val idealMin = 160f; val idealMax = 180f
        val score = angleScore(angle, idealMin, idealMax)
        val feedback = when {
            angle < idealMin - 15f -> "고관절 신전이 크게 부족합니다. 뒤 발 차기를 강화해보세요."
            angle < idealMin -> "push-off 시 고관절을 조금 더 신전시켜 보세요."
            else -> "고관절 신전이 적절합니다."
        }
        val tip = if (score < 80) "완전한 고관절 신전은 추진력을 높여줍니다." else ""
        return PostureCategoryResult(score, angle, idealMin, idealMax, "°", feedback, tip)
    }

    private fun evalOverstride(ratio: Float): PostureCategoryResult {
        val score = when {
            ratio <= 0.10f -> 100
            ratio <= 0.20f -> (100 - ((ratio - 0.10f) / 0.10f) * 40).toInt().coerceIn(0, 100)
            ratio <= 0.30f -> (60 - ((ratio - 0.20f) / 0.10f) * 30).toInt().coerceIn(0, 100)
            else -> (30 - ((ratio - 0.30f) / 0.10f) * 30).toInt().coerceAtLeast(0)
        }
        val pct = (ratio * 100).toInt()
        val feedback = when {
            ratio <= 0.10f -> "착지 위치가 이상적입니다."
            ratio <= 0.20f -> "착지 위치가 약간 앞쪽입니다. 보폭을 조금 줄여보세요."
            ratio <= 0.30f -> "오버스트라이드가 감지됩니다. 보폭을 줄이고 케이던스를 높여보세요."
            else -> "착지 위치가 몸 앞쪽으로 많이 나와 있습니다. 충격과 부상 위험이 높습니다."
        }
        val tip = if (score < 80) "발이 엉덩이 아래에 가깝게 착지하면 제동력을 줄일 수 있습니다." else ""
        return PostureCategoryResult(score, ratio, 0f, 0.10f, "%", feedback, tip)
    }

    // ── Reference metrics (not included in overall score) ────────────────────

    // Detect ground contacts from near-side ankle Y peaks (Y increases downward →
    // a peak means the ankle is near the ground). Multiply by 2 to get total steps/min
    // since we only see one leg's contacts in a side-profile video.
    private fun evalCadence(frames: List<PostureFrameAngles>): PostureCategoryResult {
        val idealMin = 170f; val idealMax = 180f
        val noData = PostureCategoryResult(0, 0f, idealMin, idealMax, "spm",
            "케이던스 측정을 위해 더 긴 구간이 필요합니다.", "")

        val durationMs = (frames.lastOrNull()?.timestampMs ?: 0L) -
                         (frames.firstOrNull()?.timestampMs ?: 0L)
        if (frames.size < 8 || durationMs < 2000L) return noData

        val ankleY = frames.map { it.nearAnkleY }

        // Local-maximum detection with a 5-frame minimum interval to suppress
        // sub-peaks within a single ground-contact plateau (5 frames = 500ms at 10fps,
        // allowing detection up to ~240 spm while preventing double-counting).
        val minPeakY = 0.55f
        val minGapFrames = 5
        var stepCount = 0
        var lastPeakIdx = -minGapFrames

        for (i in 1 until ankleY.size - 1) {
            if (i - lastPeakIdx < minGapFrames) continue
            if (ankleY[i] > ankleY[i - 1] && ankleY[i] >= ankleY[i + 1] && ankleY[i] > minPeakY) {
                stepCount++
                lastPeakIdx = i
            }
        }
        if (stepCount < 3) return noData

        val spm = stepCount * 2f * 60_000f / durationMs
        val score = angleScore(spm, idealMin, idealMax)
        val feedback = when {
            spm < 150f -> "케이던스가 매우 낮습니다. 보폭을 줄이고 발놀림을 빠르게 해보세요."
            spm < idealMin -> "케이던스 ${spm.roundToInt()}spm은 낮습니다. 170spm 이상을 목표로 해보세요."
            spm > idealMax + 10f -> "케이던스 ${spm.roundToInt()}spm으로 리듬이 좋습니다."
            else -> "케이던스 ${spm.roundToInt()}spm으로 이상적입니다."
        }
        val tip = if (score < 80) "케이던스를 높이면 오버스트라이드가 줄고 부상 위험이 낮아집니다." else ""
        return PostureCategoryResult(score, spm, idealMin, idealMax, "spm", feedback, tip)
    }

    // Vertical oscillation: IQR of hip mid-Y (robust to outlier frames), normalized
    // by estimated body height. Hip-to-ankle distance ≈ 52 % of full body height.
    private fun evalVerticalOscillation(frames: List<PostureFrameAngles>): PostureCategoryResult {
        val idealMin = 4f; val idealMax = 8f
        val noData = PostureCategoryResult(0, 0f, idealMin, idealMax, "%",
            "수직진폭 측정을 위해 더 긴 구간이 필요합니다.", "")
        if (frames.size < 8) return noData

        val hipY = frames.map { it.hipMidY }.sorted()

        val q25 = hipY[(hipY.size * 0.25f).toInt()]
        val q75 = hipY[(hipY.size * 0.75f).toInt()]
        val amplitudeNorm = q75 - q25

        // Leg length estimate from landing frames; fall back to mean difference
        val legLength = frames.filter { it.isLandingFrame }
            .mapNotNull { f -> (f.nearAnkleY - f.hipMidY).takeIf { it > 0f } }
            .takeIf { it.isNotEmpty() }?.average()?.toFloat()
            ?: (frames.map { it.nearAnkleY }.average() -
                frames.map { it.hipMidY }.average()).toFloat().coerceAtLeast(0.15f)

        val bodyHeightNorm = legLength / 0.52f
        val oscPct = (amplitudeNorm / bodyHeightNorm * 100f).coerceAtLeast(0f)

        val score = angleScore(oscPct, idealMin, idealMax)
        val feedback = when {
            oscPct < 2f -> "수직진폭이 매우 작습니다."
            oscPct <= idealMax -> "수직진폭이 이상적입니다. 에너지 효율이 좋습니다."
            oscPct <= 12f -> "수직진폭이 약간 큽니다. 상체를 안정화해보세요."
            else -> "상하 진폭이 큽니다. 앞으로 나아가는 에너지가 낭비되고 있습니다."
        }
        val tip = if (oscPct > idealMax) "코어 강화와 자세 안정화로 불필요한 바운싱을 줄일 수 있습니다." else ""
        return PostureCategoryResult(score, oscPct, idealMin, idealMax, "%", feedback, tip)
    }

    private fun buildOverallFeedback(
        score: Int,
        knee: PostureCategoryResult,
        trunk: PostureCategoryResult,
        overstride: PostureCategoryResult,
    ): String {
        val weakest = listOf(
            "무릎 굴곡" to knee.score,
            "상체 기울기" to trunk.score,
            "오버스트라이드" to overstride.score,
        ).minByOrNull { it.second }

        return when {
            score >= 90 -> "런닝 자세가 매우 훌륭합니다. 현재 자세를 유지하세요!"
            score >= 75 -> "전반적으로 좋은 자세입니다. ${weakest?.first} 부분을 보완하면 더 좋아집니다."
            score >= 60 -> "${weakest?.first} 개선이 필요합니다. 꾸준히 연습해 보세요."
            else -> "자세 개선이 필요합니다. 각 항목의 피드백을 참고해 연습해 보세요."
        }
    }

    private fun angleScore(value: Float, idealMin: Float, idealMax: Float): Int {
        if (value in idealMin..idealMax) return 100
        val deviation = if (value < idealMin) idealMin - value else value - idealMax
        return when {
            deviation <= 5f -> (90 - (deviation / 5f) * 20).toInt()
            deviation <= 15f -> (70 - ((deviation - 5f) / 10f) * 30).toInt()
            else -> (40 - ((deviation - 15f) / 15f) * 40).toInt().coerceAtLeast(0)
        }
    }

    private fun List<Float>.median(): Float {
        if (isEmpty()) return 0f
        val sorted = sorted()
        return if (sorted.size % 2 == 0)
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
        else
            sorted[sorted.size / 2]
    }

    private fun emptyResult() = PostureResult(
        overallScore = 0,
        grade = "D",
        overallFeedback = "분석 가능한 프레임이 없습니다. 다시 촬영해주세요.",
        knee = PostureCategoryResult(0, 0f, 135f, 165f, "°", "분석 불가", ""),
        trunk = PostureCategoryResult(0, 0f, 5f, 10f, "°", "분석 불가", ""),
        elbow = PostureCategoryResult(0, 0f, 85f, 95f, "°", "분석 불가", ""),
        hip = PostureCategoryResult(0, 0f, 160f, 180f, "°", "분석 불가", ""),
        overstride = PostureCategoryResult(0, 0f, 0f, 0.10f, "%", "분석 불가", ""),
        cadence = PostureCategoryResult(0, 0f, 170f, 180f, "spm", "분석 불가", ""),
        verticalOscillation = PostureCategoryResult(0, 0f, 4f, 8f, "%", "분석 불가", ""),
    )
}
