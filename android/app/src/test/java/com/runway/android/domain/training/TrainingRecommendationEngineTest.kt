package com.runway.android.domain.training

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingRecommendationEngineTest {
    private val now = Instant.parse("2026-06-14T00:00:00Z")

    @Test
    fun `no data returns beginner plan`() {
        val recommendation = TrainingRecommendationEngine.calculate(emptyList(), now)

        assertEquals(TrainingRecommendationStatus.NO_DATA, recommendation.status)
        assertEquals(3, recommendation.plan.weeklyRuns)
        assertFalse(
            recommendation.plan.sessions.any {
                it.type == TrainingSessionType.TEMPO || it.type == TrainingSessionType.INTERVAL
            },
        )
    }

    @Test
    fun `insufficient data exposes readiness progress`() {
        val recommendation = TrainingRecommendationEngine.calculate(
            listOf(
                run(daysAgo = 1, distanceMeters = 3_000.0),
                run(daysAgo = 3, distanceMeters = 4_000.0),
            ),
            now,
        )

        assertEquals(TrainingRecommendationStatus.INSUFFICIENT_DATA, recommendation.status)
        assertEquals(2, recommendation.readiness.validRunCount)
        assertEquals(7_000.0, recommendation.readiness.totalDistanceMeters, 0.0)
        assertFalse(recommendation.readiness.isReady)
    }

    @Test
    fun `five valid runs and fifteen kilometers unlock personalized plan`() {
        val recommendation = TrainingRecommendationEngine.calculate(
            (1..5).map { run(daysAgo = it, distanceMeters = 3_000.0) },
            now,
        )

        assertEquals(TrainingRecommendationStatus.PERSONALIZED_READY, recommendation.status)
        assertTrue(recommendation.readiness.isReady)
        assertEquals(5, recommendation.readiness.validRunCount)
        assertEquals(15_000.0, recommendation.readiness.totalDistanceMeters, 0.0)
    }

    @Test
    fun `run is valid when distance or duration threshold is met`() {
        val recommendation = TrainingRecommendationEngine.calculate(
            listOf(
                run(daysAgo = 1, distanceMeters = 1_000.0, durationSeconds = 300),
                run(daysAgo = 2, distanceMeters = 500.0, durationSeconds = 600),
                run(daysAgo = 3, distanceMeters = 500.0, durationSeconds = 599),
            ),
            now,
        )

        assertEquals(2, recommendation.readiness.validRunCount)
        assertEquals(1_500.0, recommendation.readiness.totalDistanceMeters, 0.0)
    }

    @Test
    fun `runs outside fourteen days do not count toward readiness`() {
        val recommendation = TrainingRecommendationEngine.calculate(
            listOf(
                run(daysAgo = 15, distanceMeters = 10_000.0),
                run(daysAgo = 1, distanceMeters = 3_000.0),
            ),
            now,
        )

        assertEquals(1, recommendation.readiness.validRunCount)
        assertEquals(3_000.0, recommendation.readiness.totalDistanceMeters, 0.0)
    }

    @Test
    fun `personalized plan uses pace and stays below ten percent increase`() {
        val runs = (1..6).map {
            run(daysAgo = it, distanceMeters = 3_000.0, durationSeconds = 1_800)
        }
        val recommendation = TrainingRecommendationEngine.calculate(runs, now)
        val metrics = requireNotNull(recommendation.metrics)

        assertEquals(600, metrics.averagePaceSecondsPerKm)
        assertEquals(3_000.0, metrics.averageDistanceMeters, 0.0)
        assertEquals(9_000.0, metrics.weeklyDistanceMeters, 0.0)
        assertEquals(3.0, metrics.runsPerWeek, 0.0)
        assertTrue(
            recommendation.plan.totalWeeklyDistanceMeters <=
                metrics.weeklyDistanceMeters * 1.10,
        )
    }

    private fun run(
        daysAgo: Int,
        distanceMeters: Double,
        durationSeconds: Int = 1_800,
    ) = TrainingRunSample(
        runId = "run-$daysAgo-$distanceMeters",
        status = "COMPLETED",
        startedAt = now.minusSeconds(daysAgo * 86_400L),
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
    )
}
