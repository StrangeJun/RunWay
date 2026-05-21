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
    private Double avgRating;
    private Long ratingCount;
    private CreatorDto creator;
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private Boolean isFavorited;
    private String difficulty;
    private String slopeLevel;
    private String riskLevel;
    private String surfaceType;
    private String recommendedTime;
    private String warnings;
    private Boolean isOwner;
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

    public static CourseDetailResponse from(Course course, User creator, boolean isOwner,
                                             Double avgRating, Long ratingCount, boolean isFavorited) {
        GeoPoint startPoint = isOwner
                ? new GeoPoint(course.getStartLocation().getY(), course.getStartLocation().getX())
                : null;
        GeoPoint endPoint = isOwner
                ? new GeoPoint(course.getEndLocation().getY(), course.getEndLocation().getX())
                : null;

        return CourseDetailResponse.builder()
                .courseId(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .status(course.getStatus().getDbValue())
                .distanceMeters(course.getDistanceMeters())
                .isLoop(course.getIsLoop())
                .attemptCount(course.getAttemptCount())
                .completionCount(course.getCompletionCount())
                .avgRating(avgRating)
                .ratingCount(ratingCount)
                .isFavorited(isFavorited)
                .difficulty(course.getDifficulty() != null ? course.getDifficulty().getDbValue() : null)
                .slopeLevel(course.getSlopeLevel() != null ? course.getSlopeLevel().getDbValue() : null)
                .riskLevel(course.getRiskLevel() != null ? course.getRiskLevel().getDbValue() : null)
                .surfaceType(course.getSurfaceType() != null ? course.getSurfaceType().getDbValue() : null)
                .recommendedTime(course.getRecommendedTime() != null ? course.getRecommendedTime().getDbValue() : null)
                .warnings(course.getWarnings())
                .isOwner(isOwner)
                .creator(CreatorDto.from(creator))
                .startPoint(startPoint)
                .endPoint(endPoint)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
