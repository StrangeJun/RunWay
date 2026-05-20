package com.runway.course.service;

import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.course.domain.CourseRating;
import com.runway.course.dto.CourseRatingRequest;
import com.runway.course.dto.CourseRatingResponse;
import com.runway.course.repository.CourseRatingRepository;
import com.runway.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRatingService {

    private final CourseRepository courseRepository;
    private final CourseRatingRepository courseRatingRepository;

    @Transactional
    public CourseRatingResponse rateCourse(UUID userId, UUID courseId, CourseRatingRequest request) {
        courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));

        Optional<CourseRating> existing = courseRatingRepository.findByCourseIdAndUserId(courseId, userId);
        CourseRating rating;
        if (existing.isPresent()) {
            rating = existing.get();
            rating.update(request.getRating(), request.getComment());
            log.info("Course rating updated: courseId={} userId={} rating={}", courseId, userId, request.getRating());
        } else {
            rating = CourseRating.builder()
                    .courseId(courseId)
                    .userId(userId)
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .build();
            courseRatingRepository.save(rating);
            log.info("Course rating created: courseId={} userId={} rating={}", courseId, userId, request.getRating());
        }

        return CourseRatingResponse.from(rating);
    }
}
