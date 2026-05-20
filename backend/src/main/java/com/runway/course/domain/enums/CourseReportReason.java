package com.runway.course.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseReportReason {
    INAPPROPRIATE_CONTENT,
    WRONG_LOCATION,
    SPAM,
    OTHER;

    public String toDbValue() {
        return name().toLowerCase();
    }

    public static CourseReportReason fromDbValue(String value) {
        return valueOf(value.toUpperCase());
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseReportReason, String> {
        @Override
        public String convertToDatabaseColumn(CourseReportReason reason) {
            return reason == null ? null : reason.toDbValue();
        }

        @Override
        public CourseReportReason convertToEntityAttribute(String dbData) {
            return dbData == null ? null : CourseReportReason.fromDbValue(dbData);
        }
    }
}
