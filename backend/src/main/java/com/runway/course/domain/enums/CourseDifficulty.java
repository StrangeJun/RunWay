package com.runway.course.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum CourseDifficulty {
    EASY("easy"), NORMAL("normal"), HARD("hard");

    private final String dbValue;

    CourseDifficulty(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static CourseDifficulty from(String value) {
        for (CourseDifficulty v : values()) if (v.dbValue.equals(value)) return v;
        throw new IllegalArgumentException("Unknown CourseDifficulty: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CourseDifficulty, String> {
        @Override public String convertToDatabaseColumn(CourseDifficulty a) { return a == null ? null : a.dbValue; }
        @Override public CourseDifficulty convertToEntityAttribute(String d) { return d == null ? null : from(d); }
    }
}
