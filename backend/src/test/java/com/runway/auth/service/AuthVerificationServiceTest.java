package com.runway.auth.service;

import com.runway.auth.domain.AuthVerificationChallenge;
import com.runway.auth.domain.VerificationPurpose;
import com.runway.auth.repository.AuthVerificationChallengeRepository;
import com.runway.user.domain.User;
import com.runway.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthVerificationServiceTest {

    @Mock
    private AuthVerificationChallengeRepository challengeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificationMailService mailService;

    private AuthVerificationService service;

    @BeforeEach
    void setUp() {
        service = new AuthVerificationService(
                challengeRepository,
                userRepository,
                passwordEncoder,
                mailService
        );
    }

    @Test
    void passwordResetRequestDoesNotRevealUnknownEmail() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("none@example.com"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.requestPasswordResetCode("NONE@example.com"));

        verifyNoInteractions(challengeRepository, passwordEncoder, mailService);
    }

    @Test
    void passwordResetRequestCreatesHashedSingleUseChallenge() {
        User user = User.builder()
                .email("runner@example.com")
                .passwordHash("stored-password-hash")
                .nickname("runner")
                .build();
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("runner@example.com"))
                .thenReturn(Optional.of(user));
        when(challengeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                "runner@example.com",
                VerificationPurpose.PASSWORD_RESET
        )).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
        when(challengeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.requestPasswordResetCode("Runner@example.com");

        ArgumentCaptor<AuthVerificationChallenge> captor =
                ArgumentCaptor.forClass(AuthVerificationChallenge.class);
        verify(challengeRepository).save(captor.capture());
        AuthVerificationChallenge challenge = captor.getValue();
        assertEquals("runner@example.com", challenge.getEmail());
        assertEquals(VerificationPurpose.PASSWORD_RESET, challenge.getPurpose());
        assertEquals("hashed-code", challenge.getCodeHash());
        verify(mailService).sendVerificationCode(
                eq("runner@example.com"),
                matches("\\d{6}"),
                eq(true)
        );
    }

    @Test
    void verifiedTokenCanOnlyBeConsumedOnce() {
        AuthVerificationChallenge challenge = new AuthVerificationChallenge(
                "runner@example.com",
                VerificationPurpose.SIGNUP,
                "hashed-code",
                java.time.Instant.now().plusSeconds(600)
        );
        when(challengeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                "runner@example.com",
                VerificationPurpose.SIGNUP
        )).thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

        String token = service.verifyCode(
                "runner@example.com",
                "123456",
                VerificationPurpose.SIGNUP
        );
        when(challengeRepository.findByVerificationTokenHash(anyString()))
                .thenReturn(Optional.of(challenge));

        assertEquals(
                "runner@example.com",
                service.consumeVerifiedEmail(
                        "runner@example.com",
                        token,
                        VerificationPurpose.SIGNUP
                )
        );
        assertTrue(challenge.isConsumed());
        assertThrows(
                RuntimeException.class,
                () -> service.consumeVerifiedEmail(
                        "runner@example.com",
                        token,
                        VerificationPurpose.SIGNUP
                )
        );
    }

    @Test
    void invalidCodeIncrementsFailureCounter() {
        AuthVerificationChallenge challenge = new AuthVerificationChallenge(
                "runner@example.com",
                VerificationPurpose.PASSWORD_RESET,
                "hashed-code",
                java.time.Instant.now().plusSeconds(600)
        );
        when(challengeRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                "runner@example.com",
                VerificationPurpose.PASSWORD_RESET
        )).thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("000000", "hashed-code")).thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> service.verifyCode(
                        "runner@example.com",
                        "000000",
                        VerificationPurpose.PASSWORD_RESET
                )
        );
        verify(challengeRepository).incrementFailedAttempts(challenge.getId());
    }
}
