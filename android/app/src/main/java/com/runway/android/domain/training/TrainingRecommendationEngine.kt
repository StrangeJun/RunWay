package com.runway.android.domain.training

import java.time.Duration
import java.time.Instant
import kotlin.math.floor
import kotlin.math.roundToInt

object TrainingRecommendationEngine {
    private const val MAX_WEEKLY_INCREASE_RATIO = 1.10
    private const val CONSERVATIVE_WEEKLY_INCREASE_RATIO = 1.03

    fun calculate(
        runs: List<TrainingRunSample>,
        now: Instant = Instant.now(),
    ): TrainingRecommendation {
        val completedRuns = runs.filter { it.status.equals("completed", ignoreCase = true) }
        val recentRuns = completedRuns
            .filter { it.startedAt >= now.minus(Duration.ofDays(MIN_LOOKBACK_DAYS.toLong())) }
            .filter(::isValidRun)
        val recentDistance = recentRuns.sumOf(TrainingRunSample::distanceMeters)
        val isReady = recentRuns.size >= MIN_RUN_COUNT_FOR_PERSONALIZED_PLAN &&
            recentDistance >= MIN_TOTAL_DISTANCE_METERS_FOR_PERSONALIZED_PLAN
        val readiness = TrainingReadiness(
            validRunCount = recentRuns.size,
            totalDistanceMeters = recentDistance,
            isReady = isReady,
        )

        if (!isReady) {
            val status = if (completedRuns.isEmpty()) {
                TrainingRecommendationStatus.NO_DATA
            } else {
                TrainingRecommendationStatus.INSUFFICIENT_DATA
            }
            return TrainingRecommendation(
                status = status,
                readiness = readiness,
                plan = beginnerPlan(status),
            )
        }

        val metrics = calculateMetrics(recentRuns)
        val advancedRuns = completedRuns
            .filter {
                it.startedAt >= now.minus(Duration.ofDays(MIN_LOOKBACK_DAYS_FOR_ADVANCED_PLAN.toLong()))
            }
            .filter(::isValidRun)
        val advancedReady = advancedRuns.size >= MIN_RUN_COUNT_FOR_ADVANCED_PLAN &&
            advancedRuns.sumOf(TrainingRunSample::distanceMeters) >=
            MIN_TOTAL_DISTANCE_METERS_FOR_ADVANCED_PLAN
        val inconsistent = hasInconsistentData(recentRuns)

        return TrainingRecommendation(
            status = TrainingRecommendationStatus.PERSONALIZED_READY,
            readiness = readiness,
            metrics = metrics,
            plan = personalizedPlan(
                metrics = metrics,
                advancedReady = advancedReady,
                conservative = inconsistent || metrics.runsPerWeek < 3.0,
            ),
        )
    }

    private fun isValidRun(run: TrainingRunSample): Boolean =
        run.distanceMeters >= MIN_SINGLE_RUN_DISTANCE_METERS ||
            run.durationSeconds >= MIN_SINGLE_RUN_DURATION_SECONDS

    private fun beginnerPlan(status: TrainingRecommendationStatus): TrainingPlan {
        val description = when (status) {
            TrainingRecommendationStatus.NO_DATA ->
                "아직 러닝 기록이 부족해요. 우선 가벼운 기본 플랜을 추천해드릴게요."
            else ->
                "맞춤 훈련 추천을 위해 최근 2주 내 5회 이상, 총 15km 이상 러닝 기록이 필요합니다."
        }
        return TrainingPlan(
            title = "가볍게 시작하는 1주 플랜",
            description = description,
            weeklyRuns = 3,
            totalWeeklyDistanceMeters = 0.0,
            sessions = listOf(
                TrainingSession(
                    type = TrainingSessionType.EASY,
                    title = "걷기 + 천천히 달리기",
                    targetDurationMinutes = 20,
                    guidanceText = "대화가 가능한 강도로 걷기와 달리기를 번갈아 진행하세요.",
                ),
                TrainingSession(
                    type = TrainingSessionType.RECOVERY,
                    title = "가벼운 회복 러닝",
                    targetDurationMinutes = 20,
                    guidanceText = "피로가 남아 있다면 빠른 걷기로 바꿔도 괜찮아요.",
                ),
                TrainingSession(
                    type = TrainingSessionType.LONG,
                    title = "편안한 지속 러닝",
                    targetDurationMinutes = 30,
                    guidanceText = "속도보다 편안하게 움직이는 시간을 늘리는 데 집중하세요.",
                ),
            ),
        )
    }

    private fun calculateMetrics(runs: List<TrainingRunSample>): TrainingMetrics {
        val totalDistance = runs.sumOf(TrainingRunSample::distanceMeters)
        val paceRuns = runs.filter { it.distanceMeters > 0.0 && it.durationSeconds > 0 }
        val paceDistanceKm = paceRuns.sumOf(TrainingRunSample::distanceMeters) / 1_000.0
        val averagePace = if (paceDistanceKm > 0.0) {
            (paceRuns.sumOf(TrainingRunSample::durationSeconds) / paceDistanceKm).roundToInt()
        } else {
            null
        }
        return TrainingMetrics(
            averagePaceSecondsPerKm = averagePace,
            averageDistanceMeters = totalDistance / runs.size,
            weeklyDistanceMeters = totalDistance / 2.0,
            runsPerWeek = runs.size / 2.0,
        )
    }

    private fun personalizedPlan(
        metrics: TrainingMetrics,
        advancedReady: Boolean,
        conservative: Boolean,
    ): TrainingPlan {
        val increaseRatio = if (conservative) {
            CONSERVATIVE_WEEKLY_INCREASE_RATIO
        } else {
            MAX_WEEKLY_INCREASE_RATIO
        }
        val weeklyTarget = floorToHundred(metrics.weeklyDistanceMeters * increaseRatio)
        val includeTempo = advancedReady && !conservative && metrics.averagePaceSecondsPerKm != null
        val weights = if (includeTempo) {
            listOf(
                TrainingSessionType.RECOVERY to 0.20,
                TrainingSessionType.EASY to 0.25,
                TrainingSessionType.TEMPO to 0.20,
                TrainingSessionType.LONG to 0.35,
            )
        } else {
            listOf(
                TrainingSessionType.RECOVERY to 0.22,
                TrainingSessionType.EASY to 0.33,
                TrainingSessionType.LONG to 0.45,
            )
        }
        val distances = weights.map { (_, weight) -> floorToHundred(weeklyTarget * weight) }
        val sessions = weights.mapIndexed { index, (type, _) ->
            createSession(
                type = type,
                distanceMeters = distances[index],
                averagePaceSecondsPerKm = metrics.averagePaceSecondsPerKm,
            )
        }

        return TrainingPlan(
            title = "이번 주 맞춤 훈련",
            description = "최근 러닝 기록을 바탕으로 이번 주 훈련을 추천했어요.",
            weeklyRuns = sessions.size,
            totalWeeklyDistanceMeters = sessions.sumOf { it.targetDistanceMeters ?: 0.0 },
            sessions = sessions,
        )
    }

    private fun createSession(
        type: TrainingSessionType,
        distanceMeters: Double,
        averagePaceSecondsPerKm: Int?,
    ): TrainingSession {
        val paceOffset = when (type) {
            TrainingSessionType.RECOVERY -> 75
            TrainingSessionType.EASY -> 45
            TrainingSessionType.LONG -> 60
            TrainingSessionType.TEMPO -> 0
            TrainingSessionType.INTERVAL -> 0
        }
        val targetPace = averagePaceSecondsPerKm?.plus(paceOffset)
        val duration = targetPace?.let {
            ((distanceMeters / 1_000.0) * it / 60.0).roundToInt().coerceAtLeast(10)
        }
        val title = when (type) {
            TrainingSessionType.RECOVERY -> "회복 러닝"
            TrainingSessionType.EASY -> "이지런"
            TrainingSessionType.LONG -> "롱런"
            TrainingSessionType.TEMPO -> "가벼운 템포런"
            TrainingSessionType.INTERVAL -> "인터벌"
        }
        val guidance = when (type) {
            TrainingSessionType.RECOVERY -> "호흡이 편안한 강도를 유지하고 피로하면 걷기로 전환하세요."
            TrainingSessionType.EASY -> "대화가 가능한 편안한 페이스로 달리세요."
            TrainingSessionType.LONG -> "속도를 올리지 말고 일정하고 편안한 강도를 유지하세요."
            TrainingSessionType.TEMPO -> "워밍업 후 평균 페이스 부근으로 짧게 진행하고 무리하지 마세요."
            TrainingSessionType.INTERVAL -> "충분히 회복하며 반복하고 통증이 있으면 중단하세요."
        }
        return TrainingSession(
            type = type,
            title = title,
            targetDurationMinutes = duration,
            targetDistanceMeters = distanceMeters,
            targetPaceText = targetPace?.let(::formatPace),
            guidanceText = guidance,
        )
    }

    private fun hasInconsistentData(runs: List<TrainingRunSample>): Boolean {
        val paces = runs.mapNotNull { run ->
            if (run.distanceMeters <= 0.0 || run.durationSeconds <= 0) return@mapNotNull null
            (run.durationSeconds / (run.distanceMeters / 1_000.0))
                .takeIf { it in 180.0..1_200.0 }
        }
        val paceVariesWidely = paces.size >= 3 &&
            (paces.maxOrNull() ?: 0.0) / (paces.minOrNull() ?: 1.0) > 1.45
        val averageDistance = runs.sumOf(TrainingRunSample::distanceMeters) / runs.size
        val distanceVariesWidely = runs.maxOf(TrainingRunSample::distanceMeters) >
            averageDistance * 2.5
        return paceVariesWidely || distanceVariesWidely
    }

    private fun floorToHundred(value: Double): Double = floor(value / 100.0) * 100.0

    private fun formatPace(seconds: Int): String =
        "%d'%02d\"/km".format(seconds / 60, seconds % 60)
}
