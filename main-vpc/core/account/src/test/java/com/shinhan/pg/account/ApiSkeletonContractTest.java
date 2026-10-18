package com.shinhan.pg.account;

import com.shinhan.pg.account.adapter.in.web.AccountController;
import com.shinhan.pg.account.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.account.config.AccountSkeletonConfiguration;
import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                AccountSkeletonConfiguration.class,
                new Class<?>[] {AccountController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F07",
                                "POST",
                                "/api/v1/accounts",
                                "{\"currency\": \"KRW\", \"alias\": \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F08", "GET", "/api/v1/accounts", null, "page=1&size=1"),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F09",
                                "GET",
                                "/api/v1/accounts/00000000-0000-0000-0000-000000000002",
                                null,
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F10",
                                "PATCH",
                                "/api/v1/accounts/00000000-0000-0000-0000-000000000002/status",
                                "{\"status\": \"mock-value\", \"reason\": \"mock-value\"}",
                                "")),
                "/api/v1/accounts",
                "/api/v1/accounts/invalid-id");
    }
}
