package com.shinhan.pg.apigateway.config;

import java.util.function.Function;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

/** Signature validation and revocation lookup use the same decoder entry point. */
public final class RevocationCheckingJwtDecoder implements ReactiveJwtDecoder {
    private final ReactiveJwtDecoder delegate;
    private final Function<String, Mono<Boolean>> revoked;
    private final String prefix;
    public RevocationCheckingJwtDecoder(ReactiveJwtDecoder delegate, Function<String, Mono<Boolean>> revoked) {
        this(delegate, revoked, "auth:revoked:jti:");
    }
    public RevocationCheckingJwtDecoder(ReactiveJwtDecoder delegate, Function<String, Mono<Boolean>> revoked, String prefix) {
        this.delegate = delegate;
        this.revoked = revoked;
        this.prefix = prefix;
    }
    @Override public Mono<Jwt> decode(String token) {
        return delegate.decode(token).flatMap(jwt -> {
            if (jwt.getId() == null || jwt.getId().isBlank()) {
                return Mono.error(new BadJwtException("Token identifier is required"));
            }
            return revoked.apply(prefix + jwt.getId())
                .switchIfEmpty(Mono.error(new JwtException("Revocation check unavailable")))
                .flatMap(found -> found
                    ? Mono.<Jwt>error(new BadJwtException("Token has been revoked"))
                    : Mono.just(jwt));
        }).onErrorMap(error -> error instanceof JwtException ? error
            : new JwtException("Revocation check unavailable", error));
    }
}
