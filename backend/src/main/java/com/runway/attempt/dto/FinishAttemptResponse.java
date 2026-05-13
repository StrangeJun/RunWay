package com.runway.attempt.dto;

import com.runway.attempt.domain.CourseAttempt;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class FinishAttemptResponse {

    private UUID courseAttemptId;
    private UUID runningRecordId;
    private UUID courseId;
    private String status;
    private String verificationStatus;
    private Integer durationSeconds;
    private Double distanceMeters;
    private Instant completedAt;

    public static FinishAttemptResponse from(CourseAttempt attempt) {
        return FinishAttemptResponse.builder()
                .courseAttemptId(attempt.getId())
                .runningRecordId(attempt.getRunningRecordId())
                .courseId(attempt.getCourseId())
                .status(attempt.getStatus().getDbValue())
                .verificationStatus(attempt.getVerificationStatus().getDbValue())
                .durationSeconds(attempt.getDurationSeconds())
                .distanceMeters(attempt.getDistanceMeters())
                .completedAt(attempt.getCompletedAt())
                .build();
    }
}
