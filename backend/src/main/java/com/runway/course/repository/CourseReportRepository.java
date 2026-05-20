package com.runway.course.repository;

import com.runway.course.domain.CourseReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseReportRepository extends JpaRepository<CourseReport, UUID> {

    boolean existsByCourseIdAndReporterId(UUID courseId, UUID reporterId);
}
