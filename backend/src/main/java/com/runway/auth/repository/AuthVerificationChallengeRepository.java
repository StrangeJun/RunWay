package com.runway.auth.repository;

import com.runway.auth.domain.AuthVerificationChallenge;
import com.runway.auth.domain.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface AuthVerificationChallengeRepository
        extends JpaRepository<AuthVerificationChallenge, UUID> {

    Optional<AuthVerificationChallenge> findFirstByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            VerificationPurpose purpose
    );

    Optional<AuthVerificationChallenge> findByVerificationTokenHash(String verificationTokenHash);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("""
            update AuthVerificationChallenge challenge
               set challenge.failedAttempts = challenge.failedAttempts + 1
             where challenge.id = :id
            """)
    void incrementFailedAttempts(UUID id);
}
