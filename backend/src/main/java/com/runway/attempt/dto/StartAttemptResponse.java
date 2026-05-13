package com.runway.attempt.dto;

import com.runway.attempt.domain.CourseAttempt;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class StartAttemptResponse {

    private UUID courseAttemptId;
    private UUID runningRecordId;
    private UUID courseId;
    private String status;
    private String verificationStatus;
    private Instant startedAt;

    public static StartAttemptResponse from(CourseAttempt attempt) {
        return StartAttemptResponse.builder()
                .courseAttemptId(attempt.getId())
                .runningRecordId(attempt.getRunningRecordId())
                .courseId(attempt.getCourseId())
                .status(attempt.getStatus().getDbValue())
                .verificationStatus(attempt.getVerificationStatus().getDbValue())
                .startedAt(attempt.getStartedAt())
                .build();
    }
}
