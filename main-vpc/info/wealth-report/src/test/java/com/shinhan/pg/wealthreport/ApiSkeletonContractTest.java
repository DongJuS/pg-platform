package com.shinhan.pg.wealthreport;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.wealthreport.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.wealthreport.adapter.in.web.ReportOperationsController;
import com.shinhan.pg.wealthreport.adapter.in.web.WealthReportController;
import com.shinhan.pg.wealthreport.config.WealthReportSkeletonConfiguration;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                WealthReportSkeletonConfiguration.class,
                new Class<?>[] {WealthReportController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP4-F01",
                                "POST",
                                "/api/v1/wealth/reports",
                                "{\"from\": \"2026-10-18T00:00:00Z\", \"to\":"
                                        + " \"2026-10-18T00:00:00Z\", \"accountIds\":"
                                        + " [\"00000000-0000-0000-0000-000000000002\"], \"asOf\":"
                                        + " \"2026-10-18T00:00:00Z\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP4-F02",
                                "GET",
                                "/api/v1/wealth/reports/00000000-0000-0000-0000-000000000002",
                                null,
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP4-F03",
                                "GET",
                                "/api/v1/wealth/reports",
                                null,
                                "from=2026-10-18T00:00:00Z&to=2026-10-18T00:00:00Z&status=mock-value&page=1&size=1")),
                "/api/v1/wealth/reports",
                "/api/v1/wealth/reports/invalid-id");
    }

    @Test
    void heldApisDoNotPublishMappings() {
        ApiSkeletonAssertions.heldApisHaveNoMappings(ReportOperationsController.class);
    }
}
