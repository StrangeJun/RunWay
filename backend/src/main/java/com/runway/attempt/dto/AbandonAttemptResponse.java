package com.runway.attempt.dto;

import com.runway.attempt.domain.CourseAttempt;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AbandonAttemptResponse {

    private UUID courseAttemptId;
    private UUID runningRecordId;
    private UUID courseId;
    private String status;
    private String verificationStatus;
    private Instant endedAt;

    public static AbandonAttemptResponse from(CourseAttempt attempt, Instant endedAt) {
        return AbandonAttemptResponse.builder()
                .courseAttemptId(attempt.getId())
                .runningRecordId(attempt.getRunningRecordId())
                .courseId(attempt.getCourseId())
                .status(attempt.getStatus().getDbValue())
                .verificationStatus(attempt.getVerificationStatus().getDbValue())
                .endedAt(endedAt)
                .build();
    }
}
