package com.shinhan.pg.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * CMN-F02 — JWT 검증 실구현(Spring Security 리소스서버).
 * 공개키는 auth(WO-A1)의 `/.well-known/jwks.json`에서 조회하고(`spring.security.oauth2.resourceserver.jwt.jwk-set-uri`),
 * 서명(RS256)·만료 외에 issuer/audience를 명시적으로 강제한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewayJwtConfiguration {

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            GatewayJwtProperties properties,
            org.springframework.data.redis.core.ReactiveStringRedisTemplate redis,
            @Value("${pg.auth.revocation-key-prefix:auth:revoked:jti:}") String prefix) {
        var decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(jwtValidator(properties));
        return new RevocationCheckingJwtDecoder(decoder, redis::hasKey, prefix);
    }

    /** 만료 + issuer + audience 검증 조합. 단위 테스트(GatewayJwtConfigurationTest)에서 직접 검증한다. */
    static OAuth2TokenValidator<Jwt> jwtValidator(GatewayJwtProperties properties) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                audienceValidator(properties.audience()));
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        var error = new OAuth2Error("invalid_token", "Required audience is missing: " + audience, null);
        return token -> token.getAudience() != null && token.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(error);
    }
}
