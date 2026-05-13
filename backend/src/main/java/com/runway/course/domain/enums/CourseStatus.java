package com.runway.course.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseStatus {

    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private final String dbValue;

    CourseStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static CourseStatus from(String value) {
        for (CourseStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CourseStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseStatus, String> {

        @Override
        public String convertToDatabaseColumn(CourseStatus attribute) {
            return attribute == null ? null : attribute.getDbValue();
        }

        @Override
        public CourseStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : CourseStatus.from(dbData);
        }
    }
}
