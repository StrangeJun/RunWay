package com.runway.android.core.posture

data class PostureResult(
    val overallScore: Int,
    val grade: String,
    val overallFeedback: String,
    val knee: PostureCategoryResult,
    val trunk: PostureCategoryResult,
    val elbow: PostureCategoryResult,
    val hip: PostureCategoryResult,
    val overstride: PostureCategoryResult,
    // Reference metrics: computed from bilateral signals, not included in overall score
    val cadence: PostureCategoryResult,
    val verticalOscillation: PostureCategoryResult,
) {
    companion object {
        fun gradeFrom(score: Int): String = when {
            score >= 90 -> "S"
            score >= 75 -> "A"
            score >= 60 -> "B"
            score >= 45 -> "C"
            else -> "D"
        }
    }
}
