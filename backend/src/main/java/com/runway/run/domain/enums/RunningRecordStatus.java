package com.runway.run.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum RunningRecordStatus {

    IN_PROGRESS("in_progress"),
    PAUSED("paused"),
    COMPLETED("completed"),
    ABANDONED("abandoned");

    private final String dbValue;

    RunningRecordStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static RunningRecordStatus from(String value) {
        for (RunningRecordStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RunningRecordStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<RunningRecordStatus, String> {

        @Override
        public String convertToDatabaseColumn(RunningRecordStatus attribute) {
            return attribute == null ? null : attribute.getDbValue();
        }

        @Override
        public RunningRecordStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : RunningRecordStatus.from(dbData);
        }
    }
}
