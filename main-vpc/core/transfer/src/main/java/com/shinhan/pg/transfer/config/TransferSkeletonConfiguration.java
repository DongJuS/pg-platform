package com.shinhan.pg.transfer.config;

import com.shinhan.pg.transfer.adapter.in.batch.RecoverTransferBatchSkeleton;
import com.shinhan.pg.transfer.adapter.in.kafka.CompleteTransferConsumerSkeleton;
import com.shinhan.pg.transfer.adapter.out.auth.OtpVerificationRestSkeleton;
import com.shinhan.pg.transfer.adapter.out.ledger.LedgerPostingRestSkeleton;
import com.shinhan.pg.transfer.adapter.out.persistence.OverseasTransferDetailsPersistenceAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.TransferAtomicWriteSkeleton;
import com.shinhan.pg.transfer.adapter.out.persistence.TransferHistoryPersistenceAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.TransferPersistenceAdapter;
import com.shinhan.pg.transfer.application.port.in.CompleteTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RecoverTransferUseCase;
import com.shinhan.pg.transfer.application.port.out.LedgerPostingPort;
import com.shinhan.pg.transfer.application.port.out.OtpVerificationPort;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferDetailsRepository;
import com.shinhan.pg.transfer.application.port.out.TransferAtomicWritePort;
import com.shinhan.pg.transfer.application.port.out.TransferHistoryRepository;
import com.shinhan.pg.transfer.application.port.out.TransferRepository;
import com.shinhan.pg.transfer.application.service.TransferApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class TransferSkeletonConfiguration {
    @Bean
    public LedgerPostingPort ledgerPostingPort() {
        return new LedgerPostingRestSkeleton();
    }

    @Bean
    public OtpVerificationPort otpVerificationPort() {
        return new OtpVerificationRestSkeleton();
    }

    @Bean
    public TransferAtomicWritePort transferAtomicWritePort() {
        return new TransferAtomicWriteSkeleton();
    }

    @Bean
    public TransferRepository transferRepository() {
        return new TransferPersistenceAdapter();
    }

    @Bean
    public TransferHistoryRepository transferHistoryRepository() {
        return new TransferHistoryPersistenceAdapter();
    }

    @Bean
    public OverseasTransferDetailsRepository overseasTransferDetailsRepository() {
        return new OverseasTransferDetailsPersistenceAdapter();
    }

    @Bean
    public TransferApplicationService transferApplicationService(
            LedgerPostingPort ledgerPostingPort,
            OtpVerificationPort otpVerificationPort,
            TransferAtomicWritePort transferAtomicWritePort,
            TransferRepository transferRepository,
            TransferHistoryRepository transferHistoryRepository,
            OverseasTransferDetailsRepository overseasTransferDetailsRepository) {
        return new TransferApplicationService(
                ledgerPostingPort,
                otpVerificationPort,
                transferAtomicWritePort,
                transferRepository,
                transferHistoryRepository,
                overseasTransferDetailsRepository);
    }

    @Bean
    public CompleteTransferConsumerSkeleton completeTransferKafka(CompleteTransferUseCase useCase) {
        return new CompleteTransferConsumerSkeleton(useCase);
    }

    @Bean
    public RecoverTransferBatchSkeleton recoverTransferBatch(RecoverTransferUseCase useCase) {
        return new RecoverTransferBatchSkeleton(useCase);
    }
}
