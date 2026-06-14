package com.runway.training.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.run.domain.RunningRecord;
import com.runway.run.domain.enums.RunningRecordStatus;
import com.runway.run.repository.RunningRecordRepository;
import com.runway.training.dto.TrainingRecommendationRequest;
import com.runway.training.dto.TrainingRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingRecommendationService {

    private static final int REQUIRED_RUNS = 3;
    private static final double REQUIRED_DISTANCE_METERS = 15_000.0;
    private static final int LOOKBACK_DAYS = 14;
    private static final double MIN_RUN_DISTANCE_METERS = 1_000.0;
    private static final int MIN_RUN_DURATION_SECONDS = 600;

    private final RunningRecordRepository runningRecordRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String model;

    public TrainingRecommendationResponse recommend(
            UUID userId,
            TrainingRecommendationRequest request
    ) {
        List<RunningRecord> records = runningRecordRepository
                .findTop20ByUserIdAndStatusOrderByStartedAtDesc(
                        userId, RunningRecordStatus.COMPLETED);
        TrainingContext context = buildContext(records, request, Instant.now());

        if (!apiKey.isBlank()) {
            try {
                TrainingRecommendationResponse ai = callGemini(context, request);
                if (isValid(ai, context, request)) {
                    return ai;
                }
                log.warn("Gemini training recommendation failed server validation");
            } catch (Exception e) {
                log.warn("Gemini training recommendation failed: {}", e.getMessage());
            }
        } else {
            log.error("GEMINI_API_KEY is not configured");
        }
        throw new RunwayException(ErrorCode.TRAINING_RECOMMENDATION_UNAVAILABLE);
    }

    private TrainingContext buildContext(
            List<RunningRecord> records,
            TrainingRecommendationRequest request,
            Instant now
    ) {
        List<RunningRecord> valid28 = records.stream()
                .filter(record -> !record.getStartedAt().isBefore(now.minus(Duration.ofDays(28))))
                .filter(this::isValidRun)
                .sorted(Comparator.comparing(RunningRecord::getStartedAt))
                .toList();
        List<RunningRecord> valid14 = valid28.stream()
                .filter(record -> !record.getStartedAt().isBefore(now.minus(Duration.ofDays(14))))
                .toList();
        List<RunningRecord> valid7 = valid28.stream()
                .filter(record -> !record.getStartedAt().isBefore(now.minus(Duration.ofDays(7))))
                .toList();

        double total14 = valid14.stream().mapToDouble(RunningRecord::getDistanceMeters).sum();
        double total28 = valid28.stream().mapToDouble(RunningRecord::getDistanceMeters).sum();
        double total7 = valid7.stream().mapToDouble(RunningRecord::getDistanceMeters).sum();
        double weightedPaceDistanceKm = valid28.stream()
                .filter(record -> record.getDurationSeconds() > 0 && record.getDistanceMeters() > 0)
                .mapToDouble(record -> record.getDistanceMeters() / 1_000.0)
                .sum();
        int totalDuration = valid28.stream()
                .filter(record -> record.getDurationSeconds() > 0 && record.getDistanceMeters() > 0)
                .mapToInt(RunningRecord::getDurationSeconds)
                .sum();
        Integer averagePace = weightedPaceDistanceKm > 0
                ? (int) Math.round(totalDuration / weightedPaceDistanceKm)
                : null;
        double averageDistance = valid28.isEmpty() ? 0.0 : total28 / valid28.size();
        double longest = valid28.stream().mapToDouble(RunningRecord::getDistanceMeters).max().orElse(0.0);
        double targetWeeklyDistance = targetWeeklyDistance(request);
        double plannedWeeklyDistance = plannedWeeklyDistance(targetWeeklyDistance, request, now);
        double minWeeklyDistance = roundHundred(plannedWeeklyDistance * 0.90);
        double maxWeeklyDistance = roundHundred(plannedWeeklyDistance * 1.05);
        double goalDistanceMeters = request.goalDistanceKm() == null
                ? 0.0 : request.goalDistanceKm() * 1_000.0;
        double maxLongRun = roundHundred(Math.min(
                maxWeeklyDistance * 0.40,
                Math.max(maxWeeklyDistance * 0.30, goalDistanceMeters * 1.10)
        ));
        int availableDayCount = request.availableDays() == null || request.availableDays().isEmpty()
                ? 7
                : (int) request.availableDays().stream().filter(day -> day >= 1 && day <= 7)
                .distinct().count();
        int maxRuns = Math.min(request.preferredRunsPerWeek(), availableDayCount);
        boolean qualityAllowed = valid14.size() >= REQUIRED_RUNS &&
                total14 >= REQUIRED_DISTANCE_METERS &&
                averagePace != null;
        long daysUntilGoal = request.goalDate() == null ? Long.MAX_VALUE
                : ChronoUnit.DAYS.between(
                        now.atZone(ZoneOffset.UTC).toLocalDate(),
                        request.goalDate()
                );
        int maxQualitySessions = !qualityAllowed ? 0
                : request.goalType() == TrainingRecommendationRequest.GoalType.RACE_TIME &&
                maxRuns >= 5 && daysUntilGoal > 21 ? 2 : 1;
        String confidence = valid14.size() >= REQUIRED_RUNS ? "PERSONALIZED"
                : valid14.isEmpty() ? "STARTER" : "LIMITED";

        return new TrainingContext(
                valid28, valid7.size(), valid14.size(), valid28.size(), total7, total14, total28,
                averagePace, averageDistance, targetWeeklyDistance, longest,
                maxRuns, minWeeklyDistance, maxWeeklyDistance, maxLongRun,
                qualityAllowed, maxQualitySessions, confidence
        );
    }

    private TrainingRecommendationResponse callGemini(
            TrainingContext context,
            TrainingRecommendationRequest request
    ) throws Exception {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt()))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt(context, request)))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", responseSchema()
                )
        );

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        String raw = RestClient.builder()
                .requestFactory(requestFactory)
                .build()
                .post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(raw);
        String json = root.path("candidates").path(0).path("content").path("parts")
                .path(0).path("text").asText();
        GeminiPlan plan = objectMapper.readValue(json, GeminiPlan.class);
        return toResponse("GEMINI", context, plan);
    }

    private TrainingRecommendationResponse toResponse(
            String source,
            TrainingContext context,
            GeminiPlan plan
    ) {
        List<TrainingRecommendationResponse.Session> sessions = plan.sessions().stream()
                .map(session -> new TrainingRecommendationResponse.Session(
                        session.dayOfWeek(),
                        session.type(),
                        session.title(),
                        session.durationMinutes(),
                        session.distanceKm() == null ? null : session.distanceKm() * 1_000.0,
                        session.targetPaceSecondsPerKm(),
                        session.guidance()
                ))
                .toList();
        double totalDistance = sessions.stream()
                .map(TrainingRecommendationResponse.Session::targetDistanceMeters)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        return new TrainingRecommendationResponse(
                source,
                context.confidence().equals("STARTER") ? "NO_DATA"
                        : context.confidence().equals("LIMITED") ? "INSUFFICIENT_DATA"
                        : "PERSONALIZED_READY",
                readiness(context),
                metrics(context),
                new TrainingRecommendationResponse.Plan(
                        plan.title(), plan.summary(), plan.goalAssessment(),
                        sessions.size(), totalDistance, sessions, plan.caution()
                )
        );
    }

    private boolean isValid(
            TrainingRecommendationResponse response,
            TrainingContext context,
            TrainingRecommendationRequest request
    ) {
        if (response == null || response.plan() == null || response.plan().sessions() == null) return false;
        List<TrainingRecommendationResponse.Session> sessions = response.plan().sessions();
        if (sessions.size() != context.maxRuns()) return false;
        if (sessions.stream().map(TrainingRecommendationResponse.Session::dayOfWeek).distinct().count()
                != sessions.size()) return false;
        if (request.availableDays() != null && !request.availableDays().isEmpty() &&
                sessions.stream().anyMatch(session -> !request.availableDays().contains(session.dayOfWeek()))) {
            return false;
        }
        double total = sessions.stream()
                .map(TrainingRecommendationResponse.Session::targetDistanceMeters)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        if (total < context.minWeeklyDistanceMeters() - 1.0 ||
                total > context.maxWeeklyDistanceMeters() + 1.0) return false;
        long qualityCount = sessions.stream().filter(session ->
                "TEMPO".equals(session.type()) || "INTERVAL".equals(session.type())).count();
        long intervalCount = sessions.stream().filter(session ->
                "INTERVAL".equals(session.type())).count();
        if ((!context.qualityAllowed() && qualityCount > 0) ||
                qualityCount > context.maxQualitySessions()) return false;
        if (context.qualityAllowed() &&
                request.goalType() == TrainingRecommendationRequest.GoalType.RACE_TIME &&
                (qualityCount != context.maxQualitySessions() || intervalCount != 1)) return false;
        return sessions.stream().allMatch(session ->
                session.dayOfWeek() >= 1 && session.dayOfWeek() <= 7 &&
                session.targetDistanceMeters() != null &&
                session.targetDistanceMeters() > 0 &&
                (!"LONG".equals(session.type()) ||
                        session.targetDistanceMeters() <= context.maxLongRunMeters() + 1.0));
    }

    private TrainingRecommendationResponse.Readiness readiness(TrainingContext context) {
        return new TrainingRecommendationResponse.Readiness(
                context.validRunCount14(), REQUIRED_RUNS, context.totalDistance14(),
                REQUIRED_DISTANCE_METERS, LOOKBACK_DAYS,
                context.validRunCount14() >= REQUIRED_RUNS &&
                        context.totalDistance14() >= REQUIRED_DISTANCE_METERS
        );
    }

    private TrainingRecommendationResponse.Metrics metrics(TrainingContext context) {
        return new TrainingRecommendationResponse.Metrics(
                context.averagePaceSecondsPerKm(), context.averageDistanceMeters(),
                context.weeklyDistanceMeters(), context.validRunCount7()
        );
    }

    private String systemPrompt() {
        return """
                당신은 성인 취미 러너를 위한 안전 중심 러닝 코치다.
                사용자의 목표와 최근 훈련 이력을 분석해 현실적인 1주 계획을 작성한다.
                제공되지 않은 건강 정보나 기록을 추측하지 말고 의료 진단을 하지 않는다.
                백엔드가 제공한 최대 러닝 횟수, 최대 주간 거리, 최대 롱런 거리와 고강도 허용 여부를 절대 넘지 않는다.
                사용자가 요청한 훈련 횟수와 가능한 요일을 충족하는 정확한 개수의 세션을 작성한다.
                STARTER 또는 LIMITED 데이터에서는 템포런과 인터벌을 사용하지 않고 페이스보다 RPE와 대화 테스트를 우선한다.
                대부분의 러닝은 RPE 2~4의 편안한 강도로 구성한다.
                PERSONALIZED 데이터의 RACE_TIME 목표에는 INTERVAL 세션을 정확히 1회 포함한다.
                허용 고강도가 2회면 나머지 1회는 TEMPO로 구성하고 두 고강도 사이에 EASY, RECOVERY 또는 휴식일을 둔다.
                목표 기록이 현재 능력보다 과도하면 목표 페이스를 강요하지 말고 현재 능력 기준의 짧은 인터벌을 구성한다.
                5K와 10K는 인터벌과 역치 능력, 하프는 역치와 점진적 롱런, 풀코스는 지구력과 마라톤 페이스 적응을 우선한다.
                롱런은 주간 거리의 30~40%% 안에서 구성하고 고강도 세션과 인접한 날에 배치하지 않는다.
                거리와 강도를 동시에 크게 증가시키지 않는다.
                대회가 7일 이내면 훈련량을 늘리지 않고, 8~21일이면 피로를 낮추는 계획을 우선한다.
                목표가 비현실적이면 훈련량을 억지로 늘리지 말고 goalAssessment에 반영한다.
                반드시 제공된 JSON Schema에 맞는 한국어 JSON만 반환한다.
                """;
    }

    private String userPrompt(TrainingContext c, TrainingRecommendationRequest r) throws Exception {
        List<Map<String, Object>> recentRuns = c.records().stream().map(record -> {
            Map<String, Object> run = new LinkedHashMap<>();
            run.put("date", record.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate().toString());
            run.put("distanceKm", round(record.getDistanceMeters() / 1_000.0));
            run.put("durationSeconds", record.getDurationSeconds());
            run.put("paceSecondsPerKm", record.getAvgPaceSecondsPerKm());
            return run;
        }).toList();
        long daysUntilGoal = r.goalDate() == null ? -1
                : ChronoUnit.DAYS.between(LocalDate.now(ZoneOffset.UTC), r.goalDate());
        return """
                [목표]
                목표 유형: %s
                목표 거리(km): %s
                목표 날짜: %s
                목표 기록(초): %s
                주당 희망 횟수: %d
                가능한 요일(1=월요일, 7=일요일): %s

                [분석]
                데이터 신뢰도: %s
                최근 7일 유효 러닝: %d회
                최근 7일 총거리(km): %.1f
                최근 14일 유효 러닝: %d회
                최근 28일 유효 러닝: %d회
                최근 28일 총거리(km): %.1f
                목표 달성을 위한 최소 주간 거리(km): %.1f
                평균 페이스(초/km): %s
                평균 러닝 거리(km): %.1f
                최근 최장 거리(km): %.1f
                목표일까지 남은 일수: %d

                [절대 안전 제한]
                이번 주 생성할 러닝 세션 수: 정확히 %d회
                이번 주 총거리 범위(km): %.1f~%.1f
                최대 롱런 거리(km): %.1f
                고강도 허용: %s
                최대 고강도 세션: %d회

                [최근 기록]
                %s
                """.formatted(
                r.goalType(), r.goalDistanceKm(), r.goalDate(), r.goalTimeSeconds(),
                r.preferredRunsPerWeek(), r.availableDays(), c.confidence(),
                c.validRunCount7(), c.totalDistance7() / 1_000.0,
                c.validRunCount14(), c.validRunCount28(), c.totalDistance28() / 1_000.0,
                c.weeklyDistanceMeters() / 1_000.0, c.averagePaceSecondsPerKm(),
                c.averageDistanceMeters() / 1_000.0, c.longestRunMeters() / 1_000.0,
                daysUntilGoal, c.maxRuns(), c.minWeeklyDistanceMeters() / 1_000.0,
                c.maxWeeklyDistanceMeters() / 1_000.0,
                c.maxLongRunMeters() / 1_000.0, c.qualityAllowed(),
                c.maxQualitySessions(), objectMapper.writeValueAsString(recentRuns)
        );
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> session = Map.of(
                "type", "object",
                "required", List.of("dayOfWeek", "type", "title", "distanceKm", "guidance"),
                "properties", Map.of(
                        "dayOfWeek", Map.of("type", "integer", "minimum", 1, "maximum", 7),
                        "type", Map.of("type", "string", "enum",
                                List.of("EASY", "RECOVERY", "LONG", "TEMPO", "INTERVAL")),
                        "title", Map.of("type", "string"),
                        "distanceKm", Map.of("type", "number", "minimum", 0.1),
                        "durationMinutes", Map.of("type", List.of("integer", "null")),
                        "targetPaceSecondsPerKm", Map.of("type", List.of("integer", "null")),
                        "guidance", Map.of("type", "string")
                )
        );
        return Map.of(
                "type", "object",
                "required", List.of("title", "summary", "goalAssessment", "sessions", "caution"),
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "summary", Map.of("type", "string"),
                        "goalAssessment", Map.of("type", "string", "enum",
                                List.of("ACHIEVABLE", "CHALLENGING", "INSUFFICIENT_DATA")),
                        "sessions", Map.of("type", "array", "items", session),
                        "caution", Map.of("type", "string")
                )
        );
    }

    private boolean isValidRun(RunningRecord record) {
        return record.getDistanceMeters() >= MIN_RUN_DISTANCE_METERS ||
                record.getDurationSeconds() >= MIN_RUN_DURATION_SECONDS;
    }

    double targetWeeklyDistance(TrainingRecommendationRequest request) {
        double distanceKm = request.goalDistanceKm() == null ? 5.0 : request.goalDistanceKm();
        double weeklyKm;
        if (request.goalType() == TrainingRecommendationRequest.GoalType.FITNESS) {
            weeklyKm = Math.max(15.0, request.preferredRunsPerWeek() * 5.0);
        } else {
            double[] distances = {5.0, 10.0, 21.1, 42.2};
            double[] completionMileage = {20.0, 30.0, 45.0, 60.0};
            double[] raceMileage = {30.0, 40.0, 55.0, 75.0};
            weeklyKm = interpolateMileage(
                    distanceKm,
                    distances,
                    request.goalType() == TrainingRecommendationRequest.GoalType.RACE_TIME
                            ? raceMileage : completionMileage
            );
        }

        if (request.goalType() == TrainingRecommendationRequest.GoalType.RACE_TIME &&
                request.goalTimeSeconds() != null && distanceKm > 0) {
            weeklyKm *= performanceMileageFactor(distanceKm, request.goalTimeSeconds());
        }
        return roundHundred(weeklyKm * 1_000.0);
    }

    private double interpolateMileage(double distanceKm, double[] distances, double[] mileages) {
        if (distanceKm <= distances[0]) return mileages[0];
        for (int i = 1; i < distances.length; i++) {
            if (distanceKm <= distances[i]) {
                double ratio = (distanceKm - distances[i - 1]) /
                        (distances[i] - distances[i - 1]);
                return mileages[i - 1] + ratio * (mileages[i] - mileages[i - 1]);
            }
        }
        double ultraMultiplier = mileages[mileages.length - 1] < 70.0
                ? 1.5 : 1.7;
        return Math.max(mileages[mileages.length - 1], distanceKm * ultraMultiplier);
    }

    private double performanceMileageFactor(double distanceKm, int goalTimeSeconds) {
        double[] distances = {5.0, 10.0, 21.1, 42.2};
        double[] completionTimes = {1_800.0, 3_600.0, 7_800.0, 18_000.0};
        double[] competitiveTimes = {1_200.0, 2_400.0, 5_400.0, 12_600.0};
        double completionTime = interpolateValue(distanceKm, distances, completionTimes);
        double competitiveTime = interpolateValue(distanceKm, distances, competitiveTimes);
        if (goalTimeSeconds >= completionTime) return 1.0;
        if (goalTimeSeconds <= competitiveTime) return 1.25;
        double difficulty = (completionTime - goalTimeSeconds) /
                (completionTime - competitiveTime);
        return 1.0 + difficulty * 0.25;
    }

    private double interpolateValue(double distanceKm, double[] distances, double[] values) {
        if (distanceKm <= distances[0]) return values[0] * distanceKm / distances[0];
        for (int i = 1; i < distances.length; i++) {
            if (distanceKm <= distances[i]) {
                double ratio = (distanceKm - distances[i - 1]) /
                        (distances[i] - distances[i - 1]);
                return values[i - 1] + ratio * (values[i] - values[i - 1]);
            }
        }
        return values[values.length - 1] * distanceKm / distances[distances.length - 1];
    }

    private double plannedWeeklyDistance(
            double targetWeeklyDistance,
            TrainingRecommendationRequest request,
            Instant now
    ) {
        if (request.goalDate() == null) return targetWeeklyDistance;
        long days = ChronoUnit.DAYS.between(
                now.atZone(ZoneOffset.UTC).toLocalDate(),
                request.goalDate()
        );
        double phaseFactor = days > 84 ? 0.70
                : days > 56 ? 0.80
                : days > 21 ? 0.90
                : days > 7 ? 0.80
                : 0.60;
        return roundHundred(targetWeeklyDistance * phaseFactor);
    }

    private double roundHundred(double value) {
        return Math.floor(value / 100.0) * 100.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record TrainingContext(
            List<RunningRecord> records,
            int validRunCount7,
            int validRunCount14,
            int validRunCount28,
            double totalDistance7,
            double totalDistance14,
            double totalDistance28,
            Integer averagePaceSecondsPerKm,
            double averageDistanceMeters,
            double weeklyDistanceMeters,
            double longestRunMeters,
            int maxRuns,
            double minWeeklyDistanceMeters,
            double maxWeeklyDistanceMeters,
            double maxLongRunMeters,
            boolean qualityAllowed,
            int maxQualitySessions,
            String confidence
    ) {}

    private record GeminiPlan(
            String title,
            String summary,
            String goalAssessment,
            List<GeminiSession> sessions,
            String caution
    ) {}

    private record GeminiSession(
            int dayOfWeek,
            String type,
            String title,
            Double distanceKm,
            Integer durationMinutes,
            Integer targetPaceSecondsPerKm,
            String guidance
    ) {}
}
