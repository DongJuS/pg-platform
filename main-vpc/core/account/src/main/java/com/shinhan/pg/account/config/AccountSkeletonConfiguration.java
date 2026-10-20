package com.shinhan.pg.account.config;

import com.shinhan.pg.account.adapter.out.ledger.LedgerAccountRestSkeleton;
import com.shinhan.pg.account.adapter.out.persistence.AccountPersistenceAdapter;
import com.shinhan.pg.account.adapter.out.persistence.AccountWriteAdapter;
import com.shinhan.pg.account.application.port.out.AccountRepository;
import com.shinhan.pg.account.application.port.out.AccountWritePort;
import com.shinhan.pg.account.application.port.out.LedgerAccountPort;
import com.shinhan.pg.account.application.service.AccountApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다.
 *
 * <p>구현 확정(MVP1-F07~F10): 영속성은 JdbcTemplate 실구현 bean으로 조립한다. LedgerAccountPort는 provision 경로 미결(지시서
 * §5-②)로 실제 호출하지 않는다 — 스텁 bean만 유지.
 */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class AccountSkeletonConfiguration {
    @Bean
    public LedgerAccountPort ledgerAccountPort() {
        // TODO[LEDGER-PROVISION]: 원장 계정 provision 경로(지시서 §5-②) 확정 전까지 호출 금지 스텁.
        return new LedgerAccountRestSkeleton();
    }

    @Bean
    public AccountWritePort accountWritePort(DataSource dataSource) {
        return new AccountWriteAdapter(new JdbcTemplate(dataSource));
    }

    @Bean
    public AccountRepository accountRepository(DataSource dataSource) {
        return new AccountPersistenceAdapter(new JdbcTemplate(dataSource));
    }

    @Bean
    public AccountApplicationService accountApplicationService(
            LedgerAccountPort ledgerAccountPort,
            AccountWritePort accountWritePort,
            AccountRepository accountRepository,
            org.springframework.core.env.Environment env) {
        boolean swift = env.getProperty("pg.swift.enabled", Boolean.class, false);
        var profiles = java.util.Set.of(env.getActiveProfiles());
        if (swift
                && (!profiles.contains("swift-test")
                        || profiles.contains("aws")
                        || profiles.contains("prod")
                        || profiles.contains("production")))
            throw new IllegalStateException(
                    "USD opening requires explicit swift-test development activation");
        return new AccountApplicationService(
                ledgerAccountPort, accountWritePort, accountRepository, swift);
    }
}
