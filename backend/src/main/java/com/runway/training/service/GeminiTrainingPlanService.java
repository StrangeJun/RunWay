package com.runway.training.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.runway.training.dto.TrainingPlanRequest;
import com.runway.training.dto.TrainingPlanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiTrainingPlanService {

    private final ObjectMapper objectMapper;
    private final FallbackTrainingPlanFactory fallbackFactory;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public TrainingPlanResponse recommend(TrainingPlanRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Gemini API key is not configured; using fallback training plan");
            return fallbackFactory.create(request);
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent"))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .timeout(Duration.ofSeconds(25))
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(request)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini request failed: status={}", response.statusCode());
                return fallbackFactory.create(request);
            }
            return parseResponse(response.body());
        } catch (Exception exception) {
            log.warn("Gemini training plan generation failed; using fallback", exception);
            return fallbackFactory.create(request);
        }
    }

    private String buildPayload(TrainingPlanRequest request) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", prompt(request));

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", responseSchema());
        generationConfig.put("temperature", 0.4);
        generationConfig.put("maxOutputTokens", 4096);
        return objectMapper.writeValueAsString(root);
    }

    private String prompt(TrainingPlanRequest request) {
        return """
                You are a conservative running coach. Create exactly one week (Monday through Sunday)
                of training in Korean. Avoid aggressive mileage increases and include rest days.
                This is general fitness guidance, not medical advice.

                Goal distance: %s km
                Goal time: %s minutes
                Target pace: %s per km
                Current weekly distance: %s km
                Available training days: %s
                Experience: %s

                dayOfWeek must use java.util.Calendar values: Sunday=1, Monday=2, ... Saturday=7.
                workoutType must be one of FREE, EASY, RECOVERY, TEMPO, INTERVAL, LONG, REST.
                Return all seven days. Set restDay=true and numeric targets null on rest days.
                """.formatted(
                request.getGoalDistanceKm(),
                request.getGoalTimeMinutes(),
                request.getTargetPace(),
                request.getCurrentWeeklyKm(),
                request.getTrainingDays(),
                request.getExperienceLevel()
        );
    }

    private ObjectNode responseSchema() {
        ObjectNode day = objectSchema();
        day.putArray("required")
                .add("dayOfWeek").add("workoutType").add("title")
                .add("description").add("restDay");
        ObjectNode dayProperties = (ObjectNode) day.get("properties");
        dayProperties.set("dayOfWeek", integerSchema());
        dayProperties.set("workoutType", stringSchema());
        dayProperties.set("title", stringSchema());
        dayProperties.set("distanceKm", nullableNumberSchema());
        dayProperties.set("targetPace", stringSchema());
        dayProperties.set("durationMinutes", nullableIntegerSchema());
        dayProperties.set("description", stringSchema());
        dayProperties.set("restDay", booleanSchema());

        ObjectNode schema = objectSchema();
        schema.putArray("required").add("title").add("summary").add("days");
        ObjectNode properties = (ObjectNode) schema.get("properties");
        properties.set("title", stringSchema());
        properties.set("summary", stringSchema());
        properties.set("days", objectMapper.createObjectNode()
                .put("type", "array")
                .set("items", day));
        return schema;
    }

    private TrainingPlanResponse parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String text = root.path("candidates").path(0).path("content")
                .path("parts").path(0).path("text").asText();
        JsonNode plan = objectMapper.readTree(text);
        List<TrainingPlanResponse.TrainingDay> days = objectMapper.readerForListOf(
                TrainingPlanResponse.TrainingDay.class
        ).readValue(plan.path("days"));
        if (days.size() != 7) throw new IllegalArgumentException("Gemini returned an invalid week");
        return new TrainingPlanResponse(
                plan.path("title").asText("1주 러닝 훈련 계획"),
                plan.path("summary").asText(),
                days,
                "GEMINI"
        );
    }

    private ObjectNode objectSchema() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "object");
        node.set("properties", objectMapper.createObjectNode());
        return node;
    }

    private ObjectNode stringSchema() {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private ObjectNode integerSchema() {
        return objectMapper.createObjectNode().put("type", "integer");
    }

    private ObjectNode nullableIntegerSchema() {
        return integerSchema().put("nullable", true);
    }

    private ObjectNode nullableNumberSchema() {
        return objectMapper.createObjectNode().put("type", "number").put("nullable", true);
    }

    private ObjectNode booleanSchema() {
        return objectMapper.createObjectNode().put("type", "boolean");
    }
}
