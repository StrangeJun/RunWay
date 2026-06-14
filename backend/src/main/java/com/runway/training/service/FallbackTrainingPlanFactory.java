package com.runway.training.service;

import com.runway.training.dto.TrainingPlanRequest;
import com.runway.training.dto.TrainingPlanResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FallbackTrainingPlanFactory {

    private static final int[] DAYS = {2, 3, 4, 5, 6, 7, 1};

    public TrainingPlanResponse create(TrainingPlanRequest request) {
        int trainingDays = request.getTrainingDays();
        double baseline = request.getCurrentWeeklyKm() != null && request.getCurrentWeeklyKm() > 0
                ? request.getCurrentWeeklyKm()
                : Math.max(10.0, request.getGoalDistanceKm() * 0.7);
        double plannedWeeklyKm = Math.min(
                Math.max(baseline, request.getGoalDistanceKm() * 0.8),
                baseline * 1.1
        );
        double easyDistance = roundHalf(plannedWeeklyKm / Math.max(trainingDays, 1) * 0.8);
        double longDistance = roundHalf(Math.min(
                request.getGoalDistanceKm() * 0.75,
                plannedWeeklyKm * 0.35
        ));

        List<TrainingPlanResponse.TrainingDay> days = new ArrayList<>();
        int completedRuns = 0;
        for (int i = 0; i < DAYS.length; i++) {
            boolean runDay = completedRuns < trainingDays &&
                    (i == 0 || i == 2 || i == 4 || i == 6 || trainingDays >= 5);
            if (!runDay) {
                days.add(rest(DAYS[i]));
                continue;
            }

            boolean lastRun = completedRuns == trainingDays - 1;
            String type = lastRun ? "LONG" : completedRuns == 1 && trainingDays >= 3 ? "TEMPO" : "EASY";
            double distance = lastRun ? Math.max(longDistance, easyDistance) : easyDistance;
            String pace = request.getTargetPace() == null || request.getTargetPace().isBlank()
                    ? ""
                    : adjustPace(request.getTargetPace(), type.equals("TEMPO") ? 15 : 45);
            String title = switch (type) {
                case "LONG" -> "롱런";
                case "TEMPO" -> "템포런";
                default -> "이지런";
            };
            String description = switch (type) {
                case "LONG" -> "대화 가능한 강도로 천천히 거리를 늘리세요.";
                case "TEMPO" -> "워밍업 후 목표 페이스보다 약간 여유 있게 달리세요.";
                default -> "편안한 호흡을 유지하며 회복 중심으로 달리세요.";
            };
            days.add(new TrainingPlanResponse.TrainingDay(
                    DAYS[i],
                    type,
                    title,
                    distance,
                    pace,
                    Math.max(20, (int) Math.round(distance * 7)),
                    description,
                    false
            ));
            completedRuns++;
        }

        return new TrainingPlanResponse(
                request.getGoalDistanceKm() + "km 목표 1주 훈련",
                "현재 훈련량에서 급격히 늘리지 않는 기본 계획입니다. 통증이 있으면 휴식하세요.",
                days,
                "FALLBACK"
        );
    }

    private TrainingPlanResponse.TrainingDay rest(int day) {
        return new TrainingPlanResponse.TrainingDay(
                day, "REST", "휴식", null, "", null,
                "가벼운 스트레칭이나 걷기로 회복하세요.", true
        );
    }

    private double roundHalf(double value) {
        return Math.max(1.0, Math.round(value * 2.0) / 2.0);
    }

    private String adjustPace(String pace, int secondsToAdd) {
        String[] parts = pace.split(":");
        int total = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]) + secondsToAdd;
        return "%d:%02d".formatted(total / 60, total % 60);
    }
}
