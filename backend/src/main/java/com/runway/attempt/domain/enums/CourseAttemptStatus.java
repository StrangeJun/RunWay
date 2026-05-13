package com.runway.attempt.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseAttemptStatus {

    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    ABANDONED("abandoned");

    private final String dbValue;

    CourseAttemptStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static CourseAttemptStatus from(String value) {
        for (CourseAttemptStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CourseAttemptStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseAttemptStatus, String> {

        @Override
        public String convertToDatabaseColumn(CourseAttemptStatus attribute) {
            return attribute == null ? null : attribute.getDbValue();
        }

        @Override
        public CourseAttemptStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : CourseAttemptStatus.from(dbData);
        }
    }
}
