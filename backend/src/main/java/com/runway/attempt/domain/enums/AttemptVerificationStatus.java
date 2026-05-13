package com.runway.attempt.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum AttemptVerificationStatus {

    PENDING("pending"),
    VERIFIED("verified"),
    REJECTED("rejected");

    private final String dbValue;

    AttemptVerificationStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static AttemptVerificationStatus from(String value) {
        for (AttemptVerificationStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown AttemptVerificationStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<AttemptVerificationStatus, String> {

        @Override
        public String convertToDatabaseColumn(AttemptVerificationStatus attribute) {
            return attribute == null ? null : attribute.getDbValue();
        }

        @Override
        public AttemptVerificationStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : AttemptVerificationStatus.from(dbData);
        }
    }
}
