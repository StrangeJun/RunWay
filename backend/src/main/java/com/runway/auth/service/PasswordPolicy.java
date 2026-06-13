package com.runway.auth.service;

import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 72;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "1234567890",
            "qwertyuiop",
            "password10",
            "password123",
            "admin12345",
            "welcome123",
            "letmein123",
            "runway1234"
    );

    public void validate(String password, String email, String nickname) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw weakPassword();
        }

        String normalizedPassword = password.toLowerCase(Locale.ROOT);
        if (COMMON_PASSWORDS.contains(normalizedPassword)
                || isSingleCharacterRepeated(normalizedPassword)
                || containsPersonalValue(normalizedPassword, emailLocalPart(email))
                || containsPersonalValue(normalizedPassword, normalize(nickname))) {
            throw weakPassword();
        }
    }

    private boolean containsPersonalValue(String password, String value) {
        return value != null && value.length() >= 3 && password.contains(value);
    }

    private String emailLocalPart(String email) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail == null) {
            return null;
        }
        int at = normalizedEmail.indexOf('@');
        return at > 0 ? normalizedEmail.substring(0, at) : normalizedEmail;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isSingleCharacterRepeated(String password) {
        return password.chars().distinct().count() == 1;
    }

    private RunwayException weakPassword() {
        return new RunwayException(ErrorCode.WEAK_PASSWORD);
    }
}
