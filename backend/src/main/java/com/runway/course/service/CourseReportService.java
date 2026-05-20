package com.runway.course.service;

import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.course.domain.CourseReport;
import com.runway.course.domain.enums.CourseReportReason;
import com.runway.course.dto.CourseReportRequest;
import com.runway.course.dto.CourseReportResponse;
import com.runway.course.repository.CourseReportRepository;
import com.runway.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseReportService {

    private final CourseRepository courseRepository;
    private final CourseReportRepository courseReportRepository;

    @Transactional
    public CourseReportResponse reportCourse(UUID reporterId, UUID courseId, CourseReportRequest request) {
        courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new RunwayException(ErrorCode.COURSE_NOT_FOUND));

        if (courseReportRepository.existsByCourseIdAndReporterId(courseId, reporterId)) {
            throw new RunwayException(ErrorCode.ALREADY_REPORTED);
        }

        CourseReportReason reason;
        try {
            reason = CourseReportReason.fromDbValue(request.getReason());
        } catch (IllegalArgumentException e) {
            throw new RunwayException(ErrorCode.INVALID_REPORT_REASON);
        }

        CourseReport report = CourseReport.builder()
                .courseId(courseId)
                .reporterId(reporterId)
                .reason(reason)
                .description(request.getDescription())
                .build();

        courseReportRepository.save(report);
        return CourseReportResponse.from(report);
    }
}
