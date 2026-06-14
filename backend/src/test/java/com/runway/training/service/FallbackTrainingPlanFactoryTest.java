package com.runway.training.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runway.training.dto.TrainingPlanRequest;
import com.runway.training.dto.TrainingPlanResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackTrainingPlanFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FallbackTrainingPlanFactory factory = new FallbackTrainingPlanFactory();

    @Test
    void createsSevenDaysAndRequestedNumberOfRunDays() throws Exception {
        TrainingPlanRequest request = objectMapper.readValue("""
                {
                  "goalDistanceKm": 42.195,
                  "goalTimeMinutes": 240,
                  "targetPace": "5:41",
                  "currentWeeklyKm": 20,
                  "trainingDays": 4,
                  "experienceLevel": "INTERMEDIATE"
                }
                """, TrainingPlanRequest.class);

        TrainingPlanResponse response = factory.create(request);

        assertThat(response.days()).hasSize(7);
        assertThat(response.days().stream().filter(day -> !day.restDay())).hasSize(4);
        assertThat(response.days())
                .allMatch(day -> day.dayOfWeek() >= 1 && day.dayOfWeek() <= 7);
        assertThat(response.source()).isEqualTo("FALLBACK");
    }
}
