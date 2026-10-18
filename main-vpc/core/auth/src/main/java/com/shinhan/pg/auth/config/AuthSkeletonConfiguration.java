package com.shinhan.pg.auth.config;

import com.shinhan.pg.auth.adapter.out.cache.RevokedTokenRedisAdapter;
import com.shinhan.pg.auth.adapter.out.persistence.OtpProofPersistenceAdapter;
import com.shinhan.pg.auth.adapter.out.persistence.RefreshSessionPersistenceAdapter;
import com.shinhan.pg.auth.adapter.out.persistence.UserPersistenceAdapter;
import com.shinhan.pg.auth.adapter.out.security.JwtTokenAdapter;
import com.shinhan.pg.auth.adapter.out.security.PasswordHashAdapter;
import com.shinhan.pg.auth.application.port.out.OtpProofRepository;
import com.shinhan.pg.auth.application.port.out.PasswordHashPort;
import com.shinhan.pg.auth.application.port.out.RefreshSessionRepository;
import com.shinhan.pg.auth.application.port.out.RevokedTokenStorePort;
import com.shinhan.pg.auth.application.port.out.TokenIssuancePort;
import com.shinhan.pg.auth.application.port.out.UserRepository;
import com.shinhan.pg.auth.application.service.AuthApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class AuthSkeletonConfiguration {
    @Bean
    public PasswordHashPort passwordHashPort() {
        return new PasswordHashAdapter();
    }

    @Bean
    public TokenIssuancePort tokenIssuancePort() {
        return new JwtTokenAdapter();
    }

    @Bean
    public RevokedTokenStorePort revokedTokenStorePort() {
        return new RevokedTokenRedisAdapter();
    }

    @Bean
    public UserRepository userRepository() {
        return new UserPersistenceAdapter();
    }

    @Bean
    public RefreshSessionRepository refreshSessionRepository() {
        return new RefreshSessionPersistenceAdapter();
    }

    @Bean
    public OtpProofRepository otpProofRepository() {
        return new OtpProofPersistenceAdapter();
    }

    @Bean
    public AuthApplicationService authApplicationService(
            PasswordHashPort passwordHashPort,
            TokenIssuancePort tokenIssuancePort,
            RevokedTokenStorePort revokedTokenStorePort,
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            OtpProofRepository otpProofRepository) {
        return new AuthApplicationService(
                passwordHashPort,
                tokenIssuancePort,
                revokedTokenStorePort,
                userRepository,
                refreshSessionRepository,
                otpProofRepository);
    }
}
