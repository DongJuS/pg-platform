package com.shinhan.pg.transfer;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.transfer.adapter.in.web.OverseasTransferController;
import com.shinhan.pg.transfer.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.transfer.adapter.in.web.TransferController;
import com.shinhan.pg.transfer.config.TransferSkeletonConfiguration;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                TransferSkeletonConfiguration.class,
                new Class<?>[] {TransferController.class, OverseasTransferController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F15",
                                "POST",
                                "/api/v1/transfers",
                                "{\"fromAccountId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"toAccountId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"amount\": 1000, \"currency\": \"KRW\", \"otpToken\":"
                                    + " \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F16",
                                "GET",
                                "/api/v1/transfers/00000000-0000-0000-0000-000000000002",
                                null,
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F17",
                                "GET",
                                "/api/v1/transfers",
                                null,
                                "from=2026-10-18T00:00:00Z&to=2026-10-18T00:00:00Z&status=mock-value&page=1&size=1"),
                        new ApiSkeletonAssertions.Call(
                                "MVP5-F01",
                                "POST",
                                "/api/v1/transfers/overseas",
                                "{\"fromAccountId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"recipientName\": \"mock-value\", \"recipientAccount\":"
                                    + " \"mock-value\", \"bic\": \"mock-value\", \"amount\": 1000,"
                                    + " \"currency\": \"KRW\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP5-F02",
                                "GET",
                                "/api/v1/transfers/overseas/00000000-0000-0000-0000-000000000002",
                                null,
                                "")),
                "/api/v1/transfers",
                "/api/v1/transfers/invalid-id");
    }
}
