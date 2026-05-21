package com.runway.course.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseSlopeLevel {
    FLAT("flat"), MODERATE("moderate"), STEEP("steep");

    private final String dbValue;

    CourseSlopeLevel(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static CourseSlopeLevel from(String value) {
        for (CourseSlopeLevel v : values()) if (v.dbValue.equals(value)) return v;
        throw new IllegalArgumentException("Unknown CourseSlopeLevel: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseSlopeLevel, String> {
        @Override public String convertToDatabaseColumn(CourseSlopeLevel a) { return a == null ? null : a.dbValue; }
        @Override public CourseSlopeLevel convertToEntityAttribute(String d) { return d == null ? null : from(d); }
    }
}
