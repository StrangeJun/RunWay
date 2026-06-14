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

        double total14 = valid14.stream().mapToDouble(RunningRecord::getDistanceMeters).sum();
        double total28 = valid28.stream().mapToDouble(RunningRecord::getDistanceMeters).sum();
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
        double weeklyDistance = total28 / 4.0;
        double averageDistance = valid28.isEmpty() ? 0.0 : total28 / valid28.size();
        double longest = valid28.stream().mapToDouble(RunningRecord::getDistanceMeters).max().orElse(0.0);
        double maxWeeklyDistance = weeklyDistance > 0
                ? roundHundred(weeklyDistance * 1.10)
                : starterWeeklyDistance(request);
        double maxLongRun = longest > 0
                ? Math.min(roundHundred(longest * 1.10), maxWeeklyDistance * 0.50)
                : Math.min(maxWeeklyDistance * 0.45, 5_000.0);
        int maxRuns = Math.min(request.preferredRunsPerWeek(), Math.max(3, (int) Math.ceil(valid28.size() / 4.0) + 1));
        boolean qualityAllowed = valid14.size() >= REQUIRED_RUNS &&
                total14 >= REQUIRED_DISTANCE_METERS &&
                averagePace != null;
        String confidence = valid14.size() >= REQUIRED_RUNS ? "PERSONALIZED"
                : valid14.isEmpty() ? "STARTER" : "LIMITED";

        return new TrainingContext(
                valid28, valid14.size(), valid28.size(), total14, total28,
                averagePace, averageDistance, weeklyDistance, longest,
                maxRuns, maxWeeklyDistance, maxLongRun, qualityAllowed, confidence
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
        if (sessions.isEmpty() || sessions.size() > context.maxRuns()) return false;
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
        if (total > context.maxWeeklyDistanceMeters() + 1.0) return false;
        long qualityCount = sessions.stream().filter(session ->
                "TEMPO".equals(session.type()) || "INTERVAL".equals(session.type())).count();
        if ((!context.qualityAllowed() && qualityCount > 0) || qualityCount > 1) return false;
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
                context.weeklyDistanceMeters(), context.validRunCount28() / 4.0
        );
    }

    private String systemPrompt() {
        return """
                당신은 성인 취미 러너를 위한 안전 중심 러닝 코치다.
                사용자의 목표와 최근 훈련 이력을 분석해 현실적인 1주 계획을 작성한다.
                제공되지 않은 건강 정보나 기록을 추측하지 말고 의료 진단을 하지 않는다.
                백엔드가 제공한 최대 러닝 횟수, 최대 주간 거리, 최대 롱런 거리와 고강도 허용 여부를 절대 넘지 않는다.
                STARTER 또는 LIMITED 데이터에서는 템포런과 인터벌을 사용하지 않고 페이스보다 RPE와 대화 테스트를 우선한다.
                대부분의 러닝은 RPE 2~4의 편안한 강도로 구성한다.
                고강도 세션은 최대 1회이며 롱런과 인접한 날에 배치하지 않는다.
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
                최근 14일 유효 러닝: %d회
                최근 28일 유효 러닝: %d회
                최근 28일 총거리(km): %.1f
                주간 평균 거리(km): %.1f
                평균 페이스(초/km): %s
                평균 러닝 거리(km): %.1f
                최근 최장 거리(km): %.1f
                목표일까지 남은 일수: %d

                [절대 안전 제한]
                최대 러닝 횟수: %d
                최대 주간 총거리(km): %.1f
                최대 롱런 거리(km): %.1f
                고강도 허용: %s
                최대 고강도 세션: %d

                [최근 기록]
                %s
                """.formatted(
                r.goalType(), r.goalDistanceKm(), r.goalDate(), r.goalTimeSeconds(),
                r.preferredRunsPerWeek(), r.availableDays(), c.confidence(),
                c.validRunCount14(), c.validRunCount28(), c.totalDistance28() / 1_000.0,
                c.weeklyDistanceMeters() / 1_000.0, c.averagePaceSecondsPerKm(),
                c.averageDistanceMeters() / 1_000.0, c.longestRunMeters() / 1_000.0,
                daysUntilGoal, c.maxRuns(), c.maxWeeklyDistanceMeters() / 1_000.0,
                c.maxLongRunMeters() / 1_000.0, c.qualityAllowed(),
                c.qualityAllowed() ? 1 : 0, objectMapper.writeValueAsString(recentRuns)
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

    private double starterWeeklyDistance(TrainingRecommendationRequest request) {
        double goalBased = request.goalDistanceKm() == null
                ? 9_000.0 : Math.min(request.goalDistanceKm() * 1_000.0, 15_000.0);
        return Math.max(6_000.0, roundHundred(goalBased));
    }

    private double roundHundred(double value) {
        return Math.floor(value / 100.0) * 100.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record TrainingContext(
            List<RunningRecord> records,
            int validRunCount14,
            int validRunCount28,
            double totalDistance14,
            double totalDistance28,
            Integer averagePaceSecondsPerKm,
            double averageDistanceMeters,
            double weeklyDistanceMeters,
            double longestRunMeters,
            int maxRuns,
            double maxWeeklyDistanceMeters,
            double maxLongRunMeters,
            boolean qualityAllowed,
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
