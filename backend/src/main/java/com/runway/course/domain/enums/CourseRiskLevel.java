package com.runway.course.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseRiskLevel {
    LOW("low"), MEDIUM("medium"), HIGH("high");

    private final String dbValue;

    CourseRiskLevel(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static CourseRiskLevel from(String value) {
        for (CourseRiskLevel v : values()) if (v.dbValue.equals(value)) return v;
        throw new IllegalArgumentException("Unknown CourseRiskLevel: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseRiskLevel, String> {
        @Override public String convertToDatabaseColumn(CourseRiskLevel a) { return a == null ? null : a.dbValue; }
        @Override public CourseRiskLevel convertToEntityAttribute(String d) { return d == null ? null : from(d); }
    }
}
