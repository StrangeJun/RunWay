package com.runway.training.service;

import com.runway.training.dto.TrainingRecommendationRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingRecommendationServiceTest {

    private final TrainingRecommendationService service =
            new TrainingRecommendationService(null, null);

    @Test
    void completionMileageIncreasesWithRaceDistance() {
        assertThat(targetMileage(5.0, TrainingRecommendationRequest.GoalType.DISTANCE, null))
                .isEqualTo(20_000.0);
        assertThat(targetMileage(10.0, TrainingRecommendationRequest.GoalType.DISTANCE, null))
                .isEqualTo(30_000.0);
        assertThat(targetMileage(21.1, TrainingRecommendationRequest.GoalType.DISTANCE, null))
                .isEqualTo(45_000.0);
        assertThat(targetMileage(42.2, TrainingRecommendationRequest.GoalType.DISTANCE, null))
                .isEqualTo(60_000.0);
    }

    @Test
    void raceTimeGoalRequiresMoreMileageThanCompletionGoal() {
        double completion = targetMileage(
                42.2, TrainingRecommendationRequest.GoalType.DISTANCE, null);
        double fiveHourRace = targetMileage(
                42.2, TrainingRecommendationRequest.GoalType.RACE_TIME, 18_000);
        double threeHourRace = targetMileage(
                42.2, TrainingRecommendationRequest.GoalType.RACE_TIME, 10_800);

        assertThat(fiveHourRace).isEqualTo(75_000.0);
        assertThat(threeHourRace).isEqualTo(93_700.0);
        assertThat(threeHourRace).isGreaterThan(fiveHourRace).isGreaterThan(completion);
    }

    @Test
    void ultraMileageContinuesToScaleBeyondMarathon() {
        assertThat(targetMileage(100.0, TrainingRecommendationRequest.GoalType.DISTANCE, null))
                .isEqualTo(150_000.0);
        assertThat(targetMileage(100.0, TrainingRecommendationRequest.GoalType.RACE_TIME, null))
                .isEqualTo(170_000.0);
    }

    private double targetMileage(
            double distanceKm,
            TrainingRecommendationRequest.GoalType goalType,
            Integer goalTimeSeconds
    ) {
        return service.targetWeeklyDistance(new TrainingRecommendationRequest(
                goalType,
                distanceKm,
                LocalDate.now().plusMonths(4),
                goalTimeSeconds,
                5,
                List.of(1, 2, 3, 5, 7)
        ));
    }
}
