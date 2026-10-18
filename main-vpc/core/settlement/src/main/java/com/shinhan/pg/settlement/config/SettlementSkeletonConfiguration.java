package com.shinhan.pg.settlement.config;

import com.shinhan.pg.settlement.adapter.in.batch.CloseSettlementBatchSkeleton;
import com.shinhan.pg.settlement.adapter.out.fep.FepSettlementRestSkeleton;
import com.shinhan.pg.settlement.adapter.out.ledger.SettlementLedgerRestSkeleton;
import com.shinhan.pg.settlement.adapter.out.persistence.SettlementAtomicWriteSkeleton;
import com.shinhan.pg.settlement.adapter.out.persistence.SettlementCyclePersistenceAdapter;
import com.shinhan.pg.settlement.adapter.out.persistence.SettlementFilePersistenceAdapter;
import com.shinhan.pg.settlement.adapter.out.persistence.SettlementLinePersistenceAdapter;
import com.shinhan.pg.settlement.application.port.in.CloseSettlementUseCase;
import com.shinhan.pg.settlement.application.port.out.FepSettlementPort;
import com.shinhan.pg.settlement.application.port.out.LedgerPostingPort;
import com.shinhan.pg.settlement.application.port.out.SettlementAtomicWritePort;
import com.shinhan.pg.settlement.application.port.out.SettlementCycleRepository;
import com.shinhan.pg.settlement.application.port.out.SettlementFileRepository;
import com.shinhan.pg.settlement.application.port.out.SettlementLineRepository;
import com.shinhan.pg.settlement.application.service.SettlementApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class SettlementSkeletonConfiguration {
    @Bean
    public FepSettlementPort fepSettlementPort() {
        return new FepSettlementRestSkeleton();
    }

    @Bean
    public LedgerPostingPort ledgerPostingPort() {
        return new SettlementLedgerRestSkeleton();
    }

    @Bean
    public SettlementAtomicWritePort settlementAtomicWritePort() {
        return new SettlementAtomicWriteSkeleton();
    }

    @Bean
    public SettlementCycleRepository settlementCycleRepository() {
        return new SettlementCyclePersistenceAdapter();
    }

    @Bean
    public SettlementLineRepository settlementLineRepository() {
        return new SettlementLinePersistenceAdapter();
    }

    @Bean
    public SettlementFileRepository settlementFileRepository() {
        return new SettlementFilePersistenceAdapter();
    }

    @Bean
    public SettlementApplicationService settlementApplicationService(
            FepSettlementPort fepSettlementPort,
            LedgerPostingPort ledgerPostingPort,
            SettlementAtomicWritePort settlementAtomicWritePort,
            SettlementCycleRepository settlementCycleRepository,
            SettlementLineRepository settlementLineRepository,
            SettlementFileRepository settlementFileRepository) {
        return new SettlementApplicationService(
                fepSettlementPort,
                ledgerPostingPort,
                settlementAtomicWritePort,
                settlementCycleRepository,
                settlementLineRepository,
                settlementFileRepository);
    }

    @Bean
    public CloseSettlementBatchSkeleton closeSettlementBatch(CloseSettlementUseCase useCase) {
        return new CloseSettlementBatchSkeleton(useCase);
    }
}
