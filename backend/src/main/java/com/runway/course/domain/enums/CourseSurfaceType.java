package com.runway.course.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseSurfaceType {
    ROAD("road"), PARK("park"), TRAIL("trail"), MIXED("mixed");

    private final String dbValue;

    CourseSurfaceType(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static CourseSurfaceType from(String value) {
        for (CourseSurfaceType v : values()) if (v.dbValue.equals(value)) return v;
        throw new IllegalArgumentException("Unknown CourseSurfaceType: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseSurfaceType, String> {
        @Override public String convertToDatabaseColumn(CourseSurfaceType a) { return a == null ? null : a.dbValue; }
        @Override public CourseSurfaceType convertToEntityAttribute(String d) { return d == null ? null : from(d); }
    }
}
