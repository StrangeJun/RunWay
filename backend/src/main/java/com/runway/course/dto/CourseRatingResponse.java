package com.runway.course.dto;

import com.runway.course.domain.CourseRating;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CourseRatingResponse {

    private UUID ratingId;
    private UUID courseId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    public static CourseRatingResponse from(CourseRating r) {
        return CourseRatingResponse.builder()
                .ratingId(r.getId())
                .courseId(r.getCourseId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
