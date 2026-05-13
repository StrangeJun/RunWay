package com.runway.course.dto;

import com.runway.course.domain.Course;
import com.runway.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CourseDetailResponse {

    private UUID courseId;
    private String name;
    private String description;
    private String status;
    private Double distanceMeters;
    private Boolean isLoop;
    private Integer attemptCount;
    private Integer completionCount;
    private CreatorDto creator;
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Builder
    public static class CreatorDto {
        private UUID userId;
        private String nickname;
        private String profileImageUrl;

        public static CreatorDto from(User user) {
            return CreatorDto.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
        }
    }

    public static CourseDetailResponse from(Course course, User creator) {
        return CourseDetailResponse.builder()
                .courseId(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .status(course.getStatus().getDbValue())
                .distanceMeters(course.getDistanceMeters())
                .isLoop(course.getIsLoop())
                .attemptCount(course.getAttemptCount())
                .completionCount(course.getCompletionCount())
                .creator(CreatorDto.from(creator))
                .startPoint(new GeoPoint(
                        course.getStartLocation().getY(),
                        course.getStartLocation().getX()))
                .endPoint(new GeoPoint(
                        course.getEndLocation().getY(),
                        course.getEndLocation().getX()))
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
