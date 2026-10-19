package com.shinhan.pg.auth.config;

import com.shinhan.pg.auth.adapter.out.cache.RevokedTokenRedisAdapter;
import com.shinhan.pg.auth.adapter.out.persistence.OtpProofPersistenceAdapter;
import com.shinhan.pg.auth.adapter.out.persistence.RefreshSessionPersistenceAdapter;
import com.shinhan.pg.auth.adapter.out.persistence.UserPersistenceAdapter;
import com.shinhan.pg.auth.adapter.out.security.JwtTokenAdapter;
import com.shinhan.pg.auth.adapter.out.security.PasswordHashAdapter;
import com.shinhan.pg.auth.adapter.out.security.Rs256JwtSupport;
import com.shinhan.pg.auth.application.port.out.OtpProofRepository;
import com.shinhan.pg.auth.application.port.out.PasswordHashPort;
import com.shinhan.pg.auth.application.port.out.RefreshSessionRepository;
import com.shinhan.pg.auth.application.port.out.RevokedTokenStorePort;
import com.shinhan.pg.auth.application.port.out.TokenIssuancePort;
import com.shinhan.pg.auth.application.port.out.UserRepository;
import com.shinhan.pg.auth.application.service.AuthApplicationService;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class AuthSkeletonConfiguration {
    @Bean
    public AuthTokenProperties authTokenProperties(Environment environment) {
        return AuthTokenProperties.from(environment);
    }

    @Bean
    public Rs256JwtSupport rs256JwtSupport(AuthTokenProperties properties) {
        if (properties.privateKeyPem() != null && properties.publicKeyPem() != null) {
            return Rs256JwtSupport.fromPem(
                    properties.privateKeyPem(), properties.publicKeyPem(), properties.keyId());
        }
        LoggerFactory.getLogger(getClass())
                .warn(
                        "pg.auth.jwt 키 미설정 — 개발용 임시 RS256 키를 사용합니다."
                                + " 서버 재시작 시 기존 access 토큰이 전부 무효화됩니다.");
        return Rs256JwtSupport.generateEphemeral(properties.keyId());
    }

    @Bean
    public PasswordHashPort passwordHashPort() {
        return new PasswordHashAdapter();
    }

    @Bean
    public TokenIssuancePort tokenIssuancePort(
            Rs256JwtSupport jwt, AuthTokenProperties properties) {
        return new JwtTokenAdapter(
                jwt,
                properties.issuer(),
                properties.audience(),
                properties.accessTokenTtl());
    }

    @Bean
    public RevokedTokenStorePort revokedTokenStorePort(StringRedisTemplate redisTemplate, Environment environment) {
        return new RevokedTokenRedisAdapter(redisTemplate, environment.getProperty("pg.auth.revocation-key-prefix", "auth:revoked:jti:"));
    }

    @Bean
    public UserRepository userRepository(DataSource dataSource) {
        return new UserPersistenceAdapter(new JdbcTemplate(dataSource));
    }

    @Bean
    public RefreshSessionRepository refreshSessionRepository(DataSource dataSource) {
        return new RefreshSessionPersistenceAdapter(new JdbcTemplate(dataSource));
    }

    @Bean
    public OtpProofRepository otpProofRepository(DataSource dataSource) {
        return new OtpProofPersistenceAdapter(new JdbcTemplate(dataSource));
    }

    @Bean
    public AuthApplicationService authApplicationService(
            PasswordHashPort passwordHashPort,
            TokenIssuancePort tokenIssuancePort,
            RevokedTokenStorePort revokedTokenStorePort,
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            OtpProofRepository otpProofRepository,
            AuthTokenProperties properties) {
        return new AuthApplicationService(
                passwordHashPort,
                tokenIssuancePort,
                revokedTokenStorePort,
                userRepository,
                refreshSessionRepository,
                otpProofRepository,
                properties.otpServerSecret(),
                properties.refreshTokenTtl(),
                properties.otpTokenTtl());
    }
}
