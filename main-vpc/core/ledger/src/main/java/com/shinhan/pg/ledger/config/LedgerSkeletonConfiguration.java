package com.shinhan.pg.ledger.config;

import com.shinhan.pg.ledger.adapter.in.kafka.ApplyInstitutionResultConsumerSkeleton;
import com.shinhan.pg.ledger.adapter.out.persistence.JournalPersistenceAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.LedgerAccountPersistenceAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.LedgerAtomicWriteAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.PostingPersistenceAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.JournalEntityMapper;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.LedgerAccountEntityMapper;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.PostingEntityMapper;
import com.shinhan.pg.ledger.adapter.out.persistence.outbox.LedgerOutboxStore;
import com.shinhan.pg.ledger.application.port.in.ApplyInstitutionResultUseCase;
import com.shinhan.pg.ledger.application.port.out.JournalRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAccountRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAtomicWritePort;
import com.shinhan.pg.ledger.application.port.out.PostingRepository;
import com.shinhan.pg.ledger.application.service.LedgerApplicationService;
import com.shinhan.pg.ledger.domain.policy.BalanceProtectionPolicy;
import com.shinhan.pg.ledger.domain.policy.JournalBalancePolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. MVP1-F11~F14 실구현: LockedLedgerWriteSkeleton 대신
 * LedgerAtomicWriteAdapter를 연결한다 (계좌 정렬 잠금·잔액 검사·분개·Outbox의 단일 MariaDB 트랜잭션).
 */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class LedgerSkeletonConfiguration {
    @Bean
    public com.shinhan.pg.ledger.application.port.out.AccountQueryPort accountQueryPort(
            org.springframework.core.env.Environment env) {
        return new com.shinhan.pg.ledger.adapter.out.account.AccountQueryRestAdapter(env);
    }

    @Bean
    public com.shinhan.pg.ledger.application.port.in.CollectReportSnapshotUseCase
            collectReportSnapshotUseCase(
                    DataSource ds,
                    com.shinhan.pg.ledger.application.port.out.AccountQueryPort accounts) {
        return new com.shinhan.pg.ledger.application.service.CollectReportSnapshotService(
                accounts,
                new com.shinhan.pg.ledger.adapter.out.persistence.ReportLedgerSnapshotAdapter(ds));
    }

    @Bean
    public LedgerOutboxStore ledgerOutboxStore(DataSource dataSource) {
        return new LedgerOutboxStore(new JdbcTemplate(dataSource));
    }

    @Bean
    public LedgerAtomicWritePort ledgerAtomicWritePort(
            DataSource dataSource, LedgerOutboxStore ledgerOutboxStore) {
        var jdbc = new JdbcTemplate(dataSource);
        return new LedgerAtomicWriteAdapter(
                jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
                ledgerOutboxStore,
                new JournalBalancePolicy(),
                new BalanceProtectionPolicy());
    }

    @Bean
    public LedgerAccountRepository ledgerAccountRepository(DataSource dataSource) {
        return new LedgerAccountPersistenceAdapter(
                new JdbcTemplate(dataSource), new LedgerAccountEntityMapper());
    }

    @Bean
    public JournalRepository journalRepository(DataSource dataSource) {
        return new JournalPersistenceAdapter(
                new JdbcTemplate(dataSource), new JournalEntityMapper());
    }

    @Bean
    public PostingRepository postingRepository(DataSource dataSource) {
        return new PostingPersistenceAdapter(
                new JdbcTemplate(dataSource), new PostingEntityMapper());
    }

    @Bean
    public LedgerApplicationService ledgerApplicationService(
            LedgerAtomicWritePort ledgerAtomicWritePort,
            LedgerAccountRepository ledgerAccountRepository,
            JournalRepository journalRepository,
            PostingRepository postingRepository,
            com.shinhan.pg.ledger.application.port.out.AccountQueryPort accountQueryPort) {
        return new LedgerApplicationService(
                ledgerAtomicWritePort,
                ledgerAccountRepository,
                journalRepository,
                postingRepository,
                accountQueryPort);
    }

    @Bean
    public ApplyInstitutionResultConsumerSkeleton applyInstitutionResultKafka(
            ApplyInstitutionResultUseCase useCase) {
        return new ApplyInstitutionResultConsumerSkeleton(useCase);
    }
}
