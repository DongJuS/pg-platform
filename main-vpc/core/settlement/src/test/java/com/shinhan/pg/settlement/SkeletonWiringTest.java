package com.shinhan.pg.settlement;

import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;
import com.shinhan.pg.settlement.application.port.in.CloseSettlementUseCase;
import com.shinhan.pg.settlement.application.port.in.GetSettlementUseCase;
import com.shinhan.pg.settlement.application.port.in.RerunSettlementUseCase;
import com.shinhan.pg.settlement.config.SettlementSkeletonConfiguration;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                SettlementSkeletonConfiguration.class,
                CloseSettlementUseCase.class,
                GetSettlementUseCase.class,
                RerunSettlementUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assembles(
                SettlementSkeletonConfiguration.class,
                CloseSettlementUseCase.class,
                GetSettlementUseCase.class,
                RerunSettlementUseCase.class);
    }
}
