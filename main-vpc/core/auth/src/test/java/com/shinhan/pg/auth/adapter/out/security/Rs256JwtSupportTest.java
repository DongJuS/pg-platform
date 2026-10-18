package com.shinhan.pg.auth.adapter.out.security;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Rs256JwtSupportTest {
    private final Rs256JwtSupport jwt = Rs256JwtSupport.generateEphemeral("test-1");
    private final UUID userId = UUID.randomUUID();

    @Test
    void issuedTokenVerifiesWithClaims() {
        var token = jwt.issue(userId, "issuer-a", "audience-b",
                new Rs256JwtSupport.DurationSpec(Duration.ofMinutes(15)));
        var verified = jwt.verify(token, "issuer-a", "audience-b");
        assertEquals(userId, verified.userId());
    }

    @Test
    void wrongIssuerOrAudienceIsRejected() {
        var token = jwt.issue(userId, "issuer-a", "audience-b",
                new Rs256JwtSupport.DurationSpec(Duration.ofMinutes(15)));
        assertThrows(SecurityException.class, () -> jwt.verify(token, "issuer-x", "audience-b"));
        assertThrows(SecurityException.class, () -> jwt.verify(token, "issuer-a", "audience-x"));
    }

    @Test
    void tamperedPayloadIsRejected() {
        var token = jwt.issue(userId, "issuer-a", "audience-b",
                new Rs256JwtSupport.DurationSpec(Duration.ofMinutes(15)));
        var parts = token.split("\\.");
        var tampered = parts[0] + "." + parts[1] + "x." + parts[2];
        assertThrows(SecurityException.class,
                () -> jwt.verify(tampered, "issuer-a", "audience-b"));
    }

    @Test
    void expiredTokenIsRejected() {
        var token = jwt.issue(userId, "issuer-a", "audience-b",
                new Rs256JwtSupport.DurationSpec(Duration.ZERO));
        assertThrows(SecurityException.class, () -> jwt.verify(token, "issuer-a", "audience-b"));
    }

    @Test
    void pemRoundTripParsesAndVerifies() throws NoSuchAlgorithmException {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        var encoder = Base64.getMimeEncoder(64, "\n".getBytes());
        var privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + encoder.encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        var publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + encoder.encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
        var fromPem = Rs256JwtSupport.fromPem(privatePem, publicPem, "k-1");
        var token = fromPem.issue(userId, "i", "a",
                new Rs256JwtSupport.DurationSpec(Duration.ofMinutes(1)));
        assertEquals(userId, fromPem.verify(token, "i", "a").userId());
    }
}
