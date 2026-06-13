package com.runway.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_verification_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthVerificationChallenge {

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuthVerificationChallenge(
            String email,
            VerificationPurpose purpose,
            String codeHash,
            Instant expiresAt
    ) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public void recordFailure() {
        this.failedAttempts++;
    }

    public void verify(String tokenHash) {
        this.verificationTokenHash = tokenHash;
        this.verifiedAt = Instant.now();
    }

    public void consume() {
        this.consumedAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }
}
