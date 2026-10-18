package com.shinhan.pg.account.config;

import com.shinhan.pg.account.adapter.out.ledger.LedgerAccountRestSkeleton;
import com.shinhan.pg.account.adapter.out.persistence.AccountPersistenceAdapter;
import com.shinhan.pg.account.adapter.out.persistence.AccountWriteSkeleton;
import com.shinhan.pg.account.application.port.out.AccountRepository;
import com.shinhan.pg.account.application.port.out.AccountWritePort;
import com.shinhan.pg.account.application.port.out.LedgerAccountPort;
import com.shinhan.pg.account.application.service.AccountApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class AccountSkeletonConfiguration {
    @Bean
    public LedgerAccountPort ledgerAccountPort() {
        return new LedgerAccountRestSkeleton();
    }

    @Bean
    public AccountWritePort accountWritePort() {
        return new AccountWriteSkeleton();
    }

    @Bean
    public AccountRepository accountRepository() {
        return new AccountPersistenceAdapter();
    }

    @Bean
    public AccountApplicationService accountApplicationService(
            LedgerAccountPort ledgerAccountPort,
            AccountWritePort accountWritePort,
            AccountRepository accountRepository) {
        return new AccountApplicationService(
                ledgerAccountPort, accountWritePort, accountRepository);
    }
}
