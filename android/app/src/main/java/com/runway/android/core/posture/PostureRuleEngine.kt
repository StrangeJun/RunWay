package com.runway.android.core.posture

import javax.inject.Inject

class PostureRuleEngine @Inject constructor() : PostureEvaluator {

    override fun evaluate(frames: List<PostureFrameAngles>): PostureResult {
        if (frames.isEmpty()) return emptyResult()

        val landing = frames.filter { it.isLandingFrame }.takeIf { it.isNotEmpty() } ?: frames
        val all = frames

        val kneeMedian = landing.map { it.kneeFlexAngle }.median()
        val trunkMedian = all.map { it.trunkLeanAngle }.median()
        val elbowMedian = all.map { it.elbowAngle }.median()
        val hipMedian = all.map { it.hipExtensionAngle }.median()
        val overstrideMedian = landing.map { it.overstrideRatio }.median()

        val knee = evalKnee(kneeMedian)
        val trunk = evalTrunk(trunkMedian)
        val elbow = evalElbow(elbowMedian)
        val hip = evalHip(hipMedian)
        val overstride = evalOverstride(overstrideMedian)

        val overall = (knee.score * 0.30 + trunk.score * 0.25 + overstride.score * 0.20 +
                elbow.score * 0.15 + hip.score * 0.10).toInt()
        val grade = PostureResult.gradeFrom(overall)
        val feedback = buildOverallFeedback(overall, knee, trunk, overstride)

        return PostureResult(overall, grade, feedback, knee, trunk, elbow, hip, overstride)
    }

    private fun evalKnee(angle: Float): PostureCategoryResult {
        val idealMin = 155f; val idealMax = 170f
        val score = angleScore(angle, idealMin, idealMax)
        val feedback = when {
            angle < idealMin - 15f -> "착지 시 무릎이 너무 많이 구부러져 있습니다."
            angle < idealMin -> "착지 시 무릎을 조금 더 펴보세요."
            angle > idealMax + 15f -> "착지 시 무릎이 너무 펴져 있어 충격 흡수가 부족합니다."
            angle > idealMax -> "착지 시 무릎을 살짝 더 구부려 충격을 흡수해보세요."
            else -> "착지 시 무릎 각도가 이상적입니다."
        }
        val tip = if (score < 80) "무릎을 살짝 구부린 상태로 착지하면 관절 충격을 줄일 수 있습니다." else ""
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
            ratio <= 0.20f -> (100 - ((ratio - 0.10f) / 0.10f) * 40).toInt()
            ratio <= 0.30f -> (60 - ((ratio - 0.20f) / 0.10f) * 30).toInt()
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
        knee = PostureCategoryResult(0, 0f, 155f, 170f, "°", "분석 불가", ""),
        trunk = PostureCategoryResult(0, 0f, 5f, 10f, "°", "분석 불가", ""),
        elbow = PostureCategoryResult(0, 0f, 85f, 95f, "°", "분석 불가", ""),
        hip = PostureCategoryResult(0, 0f, 160f, 180f, "°", "분석 불가", ""),
        overstride = PostureCategoryResult(0, 0f, 0f, 0.10f, "%", "분석 불가", ""),
    )
}
