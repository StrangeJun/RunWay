package com.runway.auth.service;

import com.runway.auth.domain.AuthVerificationChallenge;
import com.runway.auth.domain.VerificationPurpose;
import com.runway.auth.repository.AuthVerificationChallengeRepository;
import com.runway.common.exception.ErrorCode;
import com.runway.common.exception.RunwayException;
import com.runway.user.domain.User;
import com.runway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AuthVerificationChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationMailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestSignupCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new RunwayException(ErrorCode.DUPLICATED_EMAIL);
        }
        createAndSend(email, VerificationPurpose.SIGNUP, true);
    }

    @Transactional
    public void requestPasswordResetCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElse(null);
        if (user == null || user.getPasswordHash() == null) {
            return;
        }
        createAndSend(email, VerificationPurpose.PASSWORD_RESET, false);
    }

    @Transactional
    public String verifyCode(String rawEmail, String code, VerificationPurpose purpose) {
        String email = normalizeEmail(rawEmail);
        AuthVerificationChallenge challenge = latest(email, purpose);
        Instant now = Instant.now();

        if (challenge.isConsumed() || challenge.isVerified() || challenge.isExpired(now)) {
            throw invalidCode();
        }
        if (challenge.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new RunwayException(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challengeRepository.incrementFailedAttempts(challenge.getId());
            throw invalidCode();
        }

        String token = randomToken();
        challenge.verify(hash(token));
        return token;
    }

    @Transactional
    public String consumeVerifiedEmail(String rawEmail, String token, VerificationPurpose purpose) {
        String email = normalizeEmail(rawEmail);
        AuthVerificationChallenge challenge = challengeRepository.findByVerificationTokenHash(hash(token))
                .orElseThrow(this::invalidToken);
        if (!challenge.getEmail().equals(email)
                || challenge.getPurpose() != purpose
                || !challenge.isVerified()
                || challenge.isConsumed()
                || challenge.isExpired(Instant.now())) {
            throw invalidToken();
        }
        challenge.consume();
        return email;
    }

    @Transactional
    public String consumePasswordResetToken(String token) {
        AuthVerificationChallenge challenge = challengeRepository.findByVerificationTokenHash(hash(token))
                .orElseThrow(this::invalidToken);
        if (challenge.getPurpose() != VerificationPurpose.PASSWORD_RESET
                || !challenge.isVerified()
                || challenge.isConsumed()
                || challenge.isExpired(Instant.now())) {
            throw invalidToken();
        }
        challenge.consume();
        return challenge.getEmail();
    }

    private void createAndSend(
            String email,
            VerificationPurpose purpose,
            boolean rejectDuringCooldown
    ) {
        if (isWithinResendCooldown(email, purpose)) {
            if (rejectDuringCooldown) {
                throw new RunwayException(ErrorCode.VERIFICATION_RESEND_TOO_SOON);
            }
            return;
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        challengeRepository.save(new AuthVerificationChallenge(
                email,
                purpose,
                passwordEncoder.encode(code),
                Instant.now().plus(CODE_TTL)
        ));
        mailService.sendVerificationCode(email, code, purpose == VerificationPurpose.PASSWORD_RESET);
    }

    private boolean isWithinResendCooldown(String email, VerificationPurpose purpose) {
        return challengeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .filter(previous -> previous.getCreatedAt().isAfter(
                        Instant.now().minus(RESEND_COOLDOWN)
                ))
                .isPresent();
    }

    private AuthVerificationChallenge latest(String email, VerificationPurpose purpose) {
        return challengeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(this::invalidCode);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private RunwayException invalidCode() {
        return new RunwayException(ErrorCode.INVALID_VERIFICATION_CODE);
    }

    private RunwayException invalidToken() {
        return new RunwayException(ErrorCode.INVALID_VERIFICATION_TOKEN);
    }
}
