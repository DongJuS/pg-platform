package com.shinhan.pg.aml;

import com.shinhan.pg.aml.application.port.in.EvaluateTransactionUseCase;
import com.shinhan.pg.aml.application.port.in.ListAmlCasesUseCase;
import com.shinhan.pg.aml.application.port.in.RetryAmlReportUseCase;
import com.shinhan.pg.aml.application.port.in.SubmitAmlReportUseCase;
import com.shinhan.pg.aml.config.AmlSkeletonConfiguration;
import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                AmlSkeletonConfiguration.class,
                EvaluateTransactionUseCase.class,
                RetryAmlReportUseCase.class,
                ListAmlCasesUseCase.class,
                SubmitAmlReportUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assembles(
                AmlSkeletonConfiguration.class,
                EvaluateTransactionUseCase.class,
                RetryAmlReportUseCase.class,
                ListAmlCasesUseCase.class,
                SubmitAmlReportUseCase.class);
    }
}
