package com.runway.course.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseReportStatus {
    PENDING,
    REVIEWED,
    DISMISSED;

    public String toDbValue() {
        return name().toLowerCase();
    }

    public static CourseReportStatus fromDbValue(String value) {
        return valueOf(value.toUpperCase());
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseReportStatus, String> {
        @Override
        public String convertToDatabaseColumn(CourseReportStatus status) {
            return status == null ? null : status.toDbValue();
        }

        @Override
        public CourseReportStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : CourseReportStatus.fromDbValue(dbData);
        }
    }
}
