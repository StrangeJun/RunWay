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

    /** 개인 최고 기록(PR) 여부 */
    private boolean isPR;
    /** 이전 최고 기록(초). 첫 완주이면 null */
    private Integer previousBestSeconds;
    /** 이전 기록 대비 개선 시간(초). 양수 = 빠름, 음수 = 느림. 첫 완주이면 null */
    private Integer improvementSeconds;

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
                .isPR(false)
                .build();
    }

    public static FinishAttemptResponse from(CourseAttempt attempt,
                                              boolean isPR,
                                              Integer previousBestSeconds,
                                              Integer improvementSeconds) {
        return FinishAttemptResponse.builder()
                .courseAttemptId(attempt.getId())
                .runningRecordId(attempt.getRunningRecordId())
                .courseId(attempt.getCourseId())
                .status(attempt.getStatus().getDbValue())
                .verificationStatus(attempt.getVerificationStatus().getDbValue())
                .durationSeconds(attempt.getDurationSeconds())
                .distanceMeters(attempt.getDistanceMeters())
                .completedAt(attempt.getCompletedAt())
                .isPR(isPR)
                .previousBestSeconds(previousBestSeconds)
                .improvementSeconds(improvementSeconds)
                .build();
    }
}
