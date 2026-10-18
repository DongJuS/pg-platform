package com.shinhan.pg.fepadapter;

import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;
import com.shinhan.pg.fepadapter.application.port.in.DispatchInstitutionMessageUseCase;
import com.shinhan.pg.fepadapter.application.port.in.ExchangeSettlementFileUseCase;
import com.shinhan.pg.fepadapter.application.port.in.QueryInstitutionResultUseCase;
import com.shinhan.pg.fepadapter.application.port.in.ReceiveInstitutionResultUseCase;
import com.shinhan.pg.fepadapter.application.port.in.SendSwiftMessageUseCase;
import com.shinhan.pg.fepadapter.config.FepAdapterSkeletonConfiguration;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                FepAdapterSkeletonConfiguration.class,
                DispatchInstitutionMessageUseCase.class,
                ReceiveInstitutionResultUseCase.class,
                QueryInstitutionResultUseCase.class,
                SendSwiftMessageUseCase.class,
                ExchangeSettlementFileUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assembles(
                FepAdapterSkeletonConfiguration.class,
                DispatchInstitutionMessageUseCase.class,
                ReceiveInstitutionResultUseCase.class,
                QueryInstitutionResultUseCase.class,
                SendSwiftMessageUseCase.class,
                ExchangeSettlementFileUseCase.class);
    }
}
