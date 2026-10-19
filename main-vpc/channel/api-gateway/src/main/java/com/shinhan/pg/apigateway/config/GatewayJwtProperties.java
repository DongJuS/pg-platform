package com.shinhan.pg.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * CMN-F02 JWT 검증 정책 값. 기본값은 auth(AuthTokenProperties)의 기본 issuer/audience와 동일하게 맞춘다.
 * auth의 발급 값이 바뀌면 gateway도 같은 값으로 함께 바꿔야 한다(계약 고정).
 */
@ConfigurationProperties(prefix = "pg.gateway.jwt")
public record GatewayJwtProperties(
        @DefaultValue("pg-platform-auth") String issuer,
        @DefaultValue("pg-platform") String audience) {}
