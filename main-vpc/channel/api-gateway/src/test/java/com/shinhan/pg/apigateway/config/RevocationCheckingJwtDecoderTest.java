package com.shinhan.pg.apigateway.config;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RevocationCheckingJwtDecoderTest {
    private static Jwt validJwt() {
        return Jwt.withTokenValue("test").header("alg", "RS256")
            .subject("00000000-0000-0000-0000-000000000001").jti("session-1")
            .issuedAt(Instant.now().minusSeconds(5)).expiresAt(Instant.now().plusSeconds(300)).build();
    }
    @Test void loggedOutTokenCannotAuthenticate() {
        var decoder = new RevocationCheckingJwtDecoder(token -> Mono.just(validJwt()), key -> Mono.just(true));
        assertThrows(JwtException.class, () -> decoder.decode("test").block());
    }
    @Test void activeTokenCanAuthenticate() {
        var decoder = new RevocationCheckingJwtDecoder(token -> Mono.just(validJwt()), key -> Mono.just(false));
        assertEquals("session-1", decoder.decode("test").block().getId());
    }
    @Test void unavailableRevocationStoreNeverGrantsAccess() {
        var decoder = new RevocationCheckingJwtDecoder(token -> Mono.just(validJwt()), key -> Mono.error(new IllegalStateException("offline")));
        assertThrows(JwtException.class, () -> decoder.decode("test").block());
    }
}
