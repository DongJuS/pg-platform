package com.shinhan.pg.auth.domain.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RFC 6238 SHA-1 기준 테스트 벡터(6자리 변환)로 Mock TOTP를 검증한다. */
class TotpMockTest {
    private static final byte[] SEED = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @Test
    void matchesRfc6238Vectors() {
        assertEquals("287082", TotpMock.codeAt(SEED, Instant.ofEpochSecond(59)));
        assertEquals("081804", TotpMock.codeAt(SEED, Instant.ofEpochSecond(1111111109L)));
        assertEquals("050471", TotpMock.codeAt(SEED, Instant.ofEpochSecond(1111111111L)));
        assertEquals("005924", TotpMock.codeAt(SEED, Instant.ofEpochSecond(1234567890L)));
        assertEquals("279037", TotpMock.codeAt(SEED, Instant.ofEpochSecond(2000000000L)));
    }

    @Test
    void verifyAcceptsCurrentStepCode() {
        var now = Instant.now();
        var code = TotpMock.codeAt(SEED, now);
        assertTrue(TotpMock.verify(SEED, code, now, 1));
    }

    @Test
    void verifyAcceptsAdjacentStepWithinWindow() {
        var now = Instant.now();
        var previous = Instant.ofEpochSecond(now.getEpochSecond() - TotpMock.TIME_STEP.getSeconds());
        var code = TotpMock.codeAt(SEED, previous);
        assertTrue(TotpMock.verify(SEED, code, now, 1));
    }

    @Test
    void verifyRejectsWrongOrMalformedCode() {
        var now = Instant.now();
        assertFalse(TotpMock.verify(SEED, "000000", now, 1));
        assertFalse(TotpMock.verify(SEED, "12", now, 1));
        assertFalse(TotpMock.verify(SEED, null, now, 1));
        assertFalse(TotpMock.verify(SEED, "abcdef", now, 1));
    }

    @Test
    void seedForIsDeterministicPerUser() {
        var secret = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        var user = java.util.UUID.randomUUID();
        var first = TotpMock.seedFor(secret, user);
        var second = TotpMock.seedFor(secret, user);
        assertEquals(java.util.Base64.getEncoder().encodeToString(first),
                java.util.Base64.getEncoder().encodeToString(second));
    }
}
