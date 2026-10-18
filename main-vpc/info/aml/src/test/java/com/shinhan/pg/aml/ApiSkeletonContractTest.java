package com.shinhan.pg.aml;

import com.shinhan.pg.aml.adapter.in.web.AmlCaseController;
import com.shinhan.pg.aml.adapter.in.web.AmlReportController;
import com.shinhan.pg.aml.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.aml.config.AmlSkeletonConfiguration;
import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                AmlSkeletonConfiguration.class,
                new Class<?>[] {AmlCaseController.class, AmlReportController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP6-F02",
                                "GET",
                                "/internal/v1/aml/cases",
                                null,
                                "status=mock-value&from=2026-10-18T00:00:00Z&to=2026-10-18T00:00:00Z&page=1&size=1"),
                        new ApiSkeletonAssertions.Call(
                                "MVP6-F03",
                                "POST",
                                "/internal/v1/aml/cases/00000000-0000-0000-0000-000000000002/reports",
                                null,
                                "")),
                null,
                null);
    }
}
