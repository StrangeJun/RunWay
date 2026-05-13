package com.runway.attempt.dto;

import com.runway.attempt.domain.CourseAttempt;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CourseAttemptResponse {

    private UUID courseAttemptId;
    private UUID runningRecordId;
    private UUID courseId;
    private String status;
    private String verificationStatus;
    private Integer durationSeconds;
    private Double distanceMeters;
    private Instant startedAt;
    private Instant completedAt;

    public static CourseAttemptResponse from(CourseAttempt attempt) {
        return CourseAttemptResponse.builder()
                .courseAttemptId(attempt.getId())
                .runningRecordId(attempt.getRunningRecordId())
                .courseId(attempt.getCourseId())
                .status(attempt.getStatus().getDbValue())
                .verificationStatus(attempt.getVerificationStatus().getDbValue())
                .durationSeconds(attempt.getDurationSeconds())
                .distanceMeters(attempt.getDistanceMeters())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .build();
    }
}
