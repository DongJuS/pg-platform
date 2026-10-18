package com.shinhan.pg.fepadapter;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.fepadapter.adapter.in.web.FepSettlementController;
import com.shinhan.pg.fepadapter.adapter.in.web.FepSwiftController;
import com.shinhan.pg.fepadapter.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.fepadapter.config.FepAdapterSkeletonConfiguration;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                FepAdapterSkeletonConfiguration.class,
                new Class<?>[] {FepSwiftController.class, FepSettlementController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP5-F04",
                                "POST",
                                "/internal/v1/fep/swift/messages",
                                "{\"transferId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"uetr\": \"mock-value\", \"messageKey\": \"mock-value\","
                                    + " \"pacs008\": \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP7-F03",
                                "POST",
                                "/internal/v1/fep/settlement/files",
                                "{\"cycleId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"fileKey\": \"mock-value\", \"checksum\": \"mock-value\","
                                    + " \"fileContent\": \"mock-value\"}",
                                "")),
                "/internal/v1/fep/swift/messages",
                null);
    }
}
