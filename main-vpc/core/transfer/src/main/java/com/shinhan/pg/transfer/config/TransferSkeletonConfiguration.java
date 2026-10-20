package com.shinhan.pg.transfer.config;

import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.transfer.adapter.in.batch.RecoverTransferBatchSkeleton;
import com.shinhan.pg.transfer.adapter.in.kafka.CompleteTransferConsumerSkeleton;
import com.shinhan.pg.transfer.adapter.out.ledger.LedgerClientProperties;
import com.shinhan.pg.transfer.adapter.out.ledger.LedgerPostingRestAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.OverseasTransferDetailsPersistenceAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.TransferAtomicWriteAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.TransferPersistenceJdbcAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.outbox.TransferOutboxStore;
import com.shinhan.pg.transfer.application.port.in.CompleteTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RecoverTransferUseCase;
import com.shinhan.pg.transfer.application.port.out.LedgerPostingPort;
import com.shinhan.pg.transfer.application.port.out.OtpVerificationPort;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferDetailsRepository;
import com.shinhan.pg.transfer.application.port.out.TransferAtomicWritePort;
import com.shinhan.pg.transfer.application.port.out.TransferQueryPort;
import com.shinhan.pg.transfer.application.service.TransferApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. WO-A6: F15~F17/F20 실구현 bean으로 연결한다. 기존 스켈레톤
 * 클래스 파일은 유지하되 bean 등록에서는 제외한다.
 */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class TransferSkeletonConfiguration {

    /** transfer→ledger 내부 REST 설정. pg.clients.ledger.base-url 바인딩(configuration contract 준수). */
    @Bean
    public LedgerClientProperties ledgerClientProperties(Environment environment) {
        return LedgerClientProperties.from(environment);
    }

    /** WO-A6 실구현: RestClient 기반 호출자 소유 ledger 클라이언트(ArchUnit R7: adapter.out.ledger). */
    @Bean
    public LedgerPostingPort ledgerPostingPort(LedgerClientProperties properties) {
        return new LedgerPostingRestAdapter(properties);
    }

    /** Auth의 내부 OTP 소비 API를 호출한다. 인증 실패/만료/재사용은 이체 예약 전에 거절한다. */
    @Bean
    public OtpVerificationPort otpVerificationPort(Environment environment) {
        return new com.shinhan.pg.transfer.adapter.out.auth.OtpVerificationRestAdapter(environment);
    }

    @Bean
    public com.shinhan.pg.transfer.application.port.out.AccountQueryPort accountQueryPort(
            Environment env) {
        return new com.shinhan.pg.transfer.adapter.out.account.AccountQueryRestAdapter(env);
    }

    /** WO-A6 실구현: 멱등 예약·조건부 완료+Outbox·UNKNOWN/FAILED 전이의 원자성 책임자. */
    @Bean
    public TransferAtomicWritePort transferAtomicWritePort(
            DataSource dataSource, OutboxStore outboxStore) {
        return new TransferAtomicWriteAdapter(dataSource, outboxStore);
    }

    /** WO-A6 실구현: TransferRepository/TransferQueryPort의 JDBC 구현. */
    @Bean
    public TransferPersistenceJdbcAdapter transferPersistenceJdbcAdapter(DataSource dataSource) {
        return new TransferPersistenceJdbcAdapter(new JdbcTemplate(dataSource));
    }

    /** WO-A6 실구현: TransferHistoryRepository의 JDBC 구현. */
    @Bean
    public com.shinhan.pg.transfer.adapter.out.persistence.TransferHistoryJdbcAdapter
            transferHistoryJdbcAdapter(DataSource dataSource) {
        return new com.shinhan.pg.transfer.adapter.out.persistence.TransferHistoryJdbcAdapter(
                new JdbcTemplate(dataSource));
    }

    @Bean
    public com.shinhan.pg.transfer.application.port.out.OverseasTransferStore
            overseasTransferDetailsRepository(DataSource ds, OutboxStore outbox, Environment env) {
        return new OverseasTransferDetailsPersistenceAdapter(
                ds, outbox, env.getProperty("pg.swift.payload-key", ""));
    }

    @Bean
    public com.shinhan.pg.transfer.application.service.OverseasTransferService
            overseasTransferService(
                    com.shinhan.pg.transfer.application.port.out.OverseasTransferStore store,
                    com.shinhan.pg.transfer.application.port.out.AccountQueryPort accounts,
                    OtpVerificationPort otp,
                    Environment env) {
        return new com.shinhan.pg.transfer.application.service.OverseasTransferService(
                env.getProperty("pg.swift.enabled", Boolean.class, false),
                store,
                new com.shinhan.pg.transfer.adapter.out.ledger.OverseasLedgerRestAdapter(env),
                accounts,
                otp);
    }

    @Bean
    public com.shinhan.pg.transfer.application.service.OverseasOperationsService
            overseasOperationsService(
                    com.shinhan.pg.transfer.application.port.out.OverseasTransferStore store,
                    Environment env) {
        var admins =
                java.util.Arrays.stream(env.getProperty("pg.swift.admin-user-ids", "").split(","))
                        .filter(s -> !s.isBlank())
                        .map(java.util.UUID::fromString)
                        .collect(java.util.stream.Collectors.toSet());
        return new com.shinhan.pg.transfer.application.service.OverseasOperationsService(
                admins,
                store,
                new com.shinhan.pg.transfer.adapter.out.swift.SwiftOperationsRestAdapter(env));
    }

    @Bean
    public TransferApplicationService transferApplicationService(
            LedgerPostingPort ledgerPostingPort,
            OtpVerificationPort otpVerificationPort,
            TransferAtomicWritePort transferAtomicWritePort,
            TransferPersistenceJdbcAdapter persistence,
            com.shinhan.pg.transfer.adapter.out.persistence.TransferHistoryJdbcAdapter histories,
            OverseasTransferDetailsRepository overseasTransferDetailsRepository,
            com.shinhan.pg.transfer.application.port.out.AccountQueryPort accountQueryPort,
            com.shinhan.pg.transfer.application.service.OverseasTransferService overseas) {
        return new TransferApplicationService(
                ledgerPostingPort,
                otpVerificationPort,
                transferAtomicWritePort,
                persistence,
                histories,
                overseasTransferDetailsRepository,
                persistence,
                accountQueryPort,
                overseas);
    }

    /** ledger.posted 소비 → completeTransfer. REST 완료와 동일한 멱등 경로로 수렴한다. */
    @Bean
    public CompleteTransferConsumerSkeleton completeTransferKafka(
            CompleteTransferUseCase useCase, TransferQueryPort transferQueryPort) {
        return new CompleteTransferConsumerSkeleton(useCase, transferQueryPort);
    }

    /** UNKNOWN 주기적 재조회. 간격은 pg.transfer.recover.interval(보수적 기본 60s, 정책 미정). */
    @Bean
    public RecoverTransferBatchSkeleton recoverTransferBatch(
            RecoverTransferUseCase useCase,
            TransferQueryPort transferQueryPort,
            Environment environment) {
        return new RecoverTransferBatchSkeleton(
                useCase,
                transferQueryPort,
                Integer.parseInt(environment.getProperty("pg.transfer.recover.batch-size", "50")));
    }

    @Bean
    public OutboxStore outboxStore(DataSource dataSource) {
        return new TransferOutboxStore(new JdbcTemplate(dataSource));
    }
}
