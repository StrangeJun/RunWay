package com.runway.course.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseRecommendedTime {
    MORNING("morning"), DAY("day"), NIGHT("night"), ANY("any");

    private final String dbValue;

    CourseRecommendedTime(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static CourseRecommendedTime from(String value) {
        for (CourseRecommendedTime v : values()) if (v.dbValue.equals(value)) return v;
        throw new IllegalArgumentException("Unknown CourseRecommendedTime: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseRecommendedTime, String> {
        @Override public String convertToDatabaseColumn(CourseRecommendedTime a) { return a == null ? null : a.dbValue; }
        @Override public CourseRecommendedTime convertToEntityAttribute(String d) { return d == null ? null : from(d); }
    }
}
