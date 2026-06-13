package com.runway.auth.domain;

public enum VerificationPurpose {
    SIGNUP("signup"),
    PASSWORD_RESET("password_reset");

    private final String dbValue;

    VerificationPurpose(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
