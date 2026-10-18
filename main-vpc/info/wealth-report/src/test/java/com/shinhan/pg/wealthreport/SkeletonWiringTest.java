package com.shinhan.pg.wealthreport;

import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;
import com.shinhan.pg.wealthreport.application.port.in.GenerateReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.GetReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.ListReportsUseCase;
import com.shinhan.pg.wealthreport.application.port.in.RecoverReportGenerationUseCase;
import com.shinhan.pg.wealthreport.application.port.in.UpdateReportSnapshotUseCase;
import com.shinhan.pg.wealthreport.config.WealthReportSkeletonConfiguration;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                WealthReportSkeletonConfiguration.class,
                GenerateReportUseCase.class,
                UpdateReportSnapshotUseCase.class,
                GetReportUseCase.class,
                ListReportsUseCase.class,
                RecoverReportGenerationUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assembles(
                WealthReportSkeletonConfiguration.class,
                GenerateReportUseCase.class,
                UpdateReportSnapshotUseCase.class,
                GetReportUseCase.class,
                ListReportsUseCase.class,
                RecoverReportGenerationUseCase.class);
    }
}
