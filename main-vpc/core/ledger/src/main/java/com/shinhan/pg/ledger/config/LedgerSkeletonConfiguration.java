package com.shinhan.pg.ledger.config;

import com.shinhan.pg.ledger.adapter.in.kafka.ApplyInstitutionResultConsumerSkeleton;
import com.shinhan.pg.ledger.adapter.out.persistence.JournalPersistenceAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.LedgerAccountPersistenceAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.LockedLedgerWriteSkeleton;
import com.shinhan.pg.ledger.adapter.out.persistence.PostingPersistenceAdapter;
import com.shinhan.pg.ledger.application.port.in.ApplyInstitutionResultUseCase;
import com.shinhan.pg.ledger.application.port.out.JournalRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAccountRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAtomicWritePort;
import com.shinhan.pg.ledger.application.port.out.PostingRepository;
import com.shinhan.pg.ledger.application.service.LedgerApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class LedgerSkeletonConfiguration {
    @Bean
    public LedgerAtomicWritePort ledgerAtomicWritePort() {
        return new LockedLedgerWriteSkeleton();
    }

    @Bean
    public LedgerAccountRepository ledgerAccountRepository() {
        return new LedgerAccountPersistenceAdapter();
    }

    @Bean
    public JournalRepository journalRepository() {
        return new JournalPersistenceAdapter();
    }

    @Bean
    public PostingRepository postingRepository() {
        return new PostingPersistenceAdapter();
    }

    @Bean
    public LedgerApplicationService ledgerApplicationService(
            LedgerAtomicWritePort ledgerAtomicWritePort,
            LedgerAccountRepository ledgerAccountRepository,
            JournalRepository journalRepository,
            PostingRepository postingRepository) {
        return new LedgerApplicationService(
                ledgerAtomicWritePort,
                ledgerAccountRepository,
                journalRepository,
                postingRepository);
    }

    @Bean
    public ApplyInstitutionResultConsumerSkeleton applyInstitutionResultKafka(
            ApplyInstitutionResultUseCase useCase) {
        return new ApplyInstitutionResultConsumerSkeleton(useCase);
    }
}
