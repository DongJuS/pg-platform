package com.shinhan.pg.settlement;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.settlement.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.settlement.adapter.in.web.SettlementController;
import com.shinhan.pg.settlement.config.SettlementSkeletonConfiguration;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                SettlementSkeletonConfiguration.class,
                new Class<?>[] {SettlementController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP7-F04", "GET", "/internal/v1/settlements/2026-10-18", null, ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP7-F04",
                                "POST",
                                "/internal/v1/settlements/2026-10-18/rerun",
                                "{\"cycleNumber\": 1, \"revision\": 1}",
                                "")),
                "/internal/v1/settlements/2026-10-18/rerun",
                null);
    }
}
