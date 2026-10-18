package com.shinhan.pg.auth.domain.policy;

import com.shinhan.pg.auth.domain.error.AuthErrorCode;
import com.shinhan.pg.common.commonerror.ApplicationException;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthPolicyTest {
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();
    private final RefreshRotationPolicy rotationPolicy = new RefreshRotationPolicy();
    private final OtpVerificationPolicy otpPolicy = new OtpVerificationPolicy(3);

    @Test
    void passwordPolicyAcceptsStrongPassword() {
        assertDoesNotThrow(() -> passwordPolicy.validatePassword("stronger1pass"));
    }

    @Test
    void passwordPolicyRejectsShortOrSimplePassword() {
        assertThrows(ApplicationException.class, () -> passwordPolicy.validatePassword("short1"));
        assertThrows(ApplicationException.class,
                () -> passwordPolicy.validatePassword("onlyletters!!"));
        assertThrows(ApplicationException.class,
                () -> passwordPolicy.validatePassword("12345678901"));
    }

    @Test
    void passwordPolicyRejectsPasswordDerivedFromEmail() {
        var error = assertThrows(ApplicationException.class,
                () -> passwordPolicy.validateNotDerivedFromEmail("alice2024x1", "alice@example.com"));
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, error.errorCode());
    }

    @Test
    void rotationRejectsRevokedSessionAsReuse() {
        var session = new com.shinhan.pg.auth.domain.model.RefreshSession(
                UUID.randomUUID(), UUID.randomUUID(), "hash", UUID.randomUUID(),
                Instant.now().plusSeconds(60), Instant.now().minusSeconds(1));
        var error = assertThrows(ApplicationException.class,
                () -> rotationPolicy.validateRotation(session, Instant.now()));
        assertEquals(AuthErrorCode.TOKEN_REUSED, error.errorCode());
    }

    @Test
    void rotationRejectsExpiredSession() {
        var session = new com.shinhan.pg.auth.domain.model.RefreshSession(
                UUID.randomUUID(), UUID.randomUUID(), "hash", UUID.randomUUID(),
                Instant.now().minusSeconds(1), null);
        assertThrows(ApplicationException.class,
                () -> rotationPolicy.validateRotation(session, Instant.now()));
    }

    @Test
    void otpPolicyLocksAfterMaxFailures() {
        var proof = new com.shinhan.pg.auth.domain.model.OtpProof(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hash",
                Instant.now().plusSeconds(60), null, 3);
        var error = assertThrows(ApplicationException.class,
                () -> otpPolicy.validateProof(proof, proof.transferDraftId(), Instant.now()));
        assertEquals(AuthErrorCode.OTP_LOCKED, error.errorCode());
    }

    @Test
    void otpPolicyRejectsConsumedProof() {
        var proof = new com.shinhan.pg.auth.domain.model.OtpProof(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hash",
                Instant.now().plusSeconds(60), Instant.now(), 0);
        var error = assertThrows(ApplicationException.class,
                () -> otpPolicy.validateProof(proof, proof.transferDraftId(), Instant.now()));
        assertEquals(AuthErrorCode.OTP_INVALID, error.errorCode());
    }
}
