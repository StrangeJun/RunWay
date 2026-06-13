package com.runway.auth.service;

import com.runway.common.exception.RunwayException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void acceptsLongPassphraseWithoutForcedSpecialCharacters() {
        assertDoesNotThrow(() ->
                passwordPolicy.validate("blue river morning 27", "runner@example.com", "speedy"));
    }

    @Test
    void rejectsShortOrCommonPasswords() {
        assertThrows(RunwayException.class, () ->
                passwordPolicy.validate("short123", "runner@example.com", "speedy"));
        assertThrows(RunwayException.class, () ->
                passwordPolicy.validate("password123", "runner@example.com", "speedy"));
    }

    @Test
    void rejectsPasswordsContainingEmailOrNickname() {
        assertThrows(RunwayException.class, () ->
                passwordPolicy.validate("runner-secure-2026", "runner@example.com", "speedy"));
        assertThrows(RunwayException.class, () ->
                passwordPolicy.validate("my-speedy-password", "runner@example.com", "speedy"));
    }
}
