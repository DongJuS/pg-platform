package com.shinhan.pg.transfer;

import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;
import com.shinhan.pg.transfer.application.port.in.CompleteTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.GetOverseasTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.GetTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.ListTransfersUseCase;
import com.shinhan.pg.transfer.application.port.in.RecoverTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestOverseasTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestTransferUseCase;
import com.shinhan.pg.transfer.config.TransferSkeletonConfiguration;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                TransferSkeletonConfiguration.class,
                RequestTransferUseCase.class,
                CompleteTransferUseCase.class,
                RecoverTransferUseCase.class,
                GetTransferUseCase.class,
                ListTransfersUseCase.class,
                RequestOverseasTransferUseCase.class,
                GetOverseasTransferUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assemblesWithInfrastructure(
                TransferSkeletonConfiguration.class,
                RequestTransferUseCase.class,
                CompleteTransferUseCase.class,
                RecoverTransferUseCase.class,
                GetTransferUseCase.class,
                ListTransfersUseCase.class,
                RequestOverseasTransferUseCase.class,
                GetOverseasTransferUseCase.class);
    }
}
