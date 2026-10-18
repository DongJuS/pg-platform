package com.shinhan.pg.ledger;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.ledger.adapter.in.web.InternalLedgerController;
import com.shinhan.pg.ledger.adapter.in.web.LedgerAccountController;
import com.shinhan.pg.ledger.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.ledger.config.LedgerSkeletonConfiguration;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                LedgerSkeletonConfiguration.class,
                new Class<?>[] {LedgerAccountController.class, InternalLedgerController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F11",
                                "GET",
                                "/api/v1/accounts/00000000-0000-0000-0000-000000000002/balances",
                                null,
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F12",
                                "GET",
                                "/api/v1/accounts/00000000-0000-0000-0000-000000000002/entries",
                                null,
                                "from=2026-10-18T00:00:00Z&to=2026-10-18T00:00:00Z&cursor=mock-value&size=1"),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F13",
                                "POST",
                                "/internal/v1/ledger/postings",
                                "{\"transactionId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"postings\": [{\"accountId\":"
                                    + " \"00000000-0000-0000-0000-000000000002\", \"signedAmount\":"
                                    + " 1000, \"currency\": \"KRW\"}, {\"accountId\":"
                                    + " \"00000000-0000-0000-0000-000000000003\", \"signedAmount\":"
                                    + " -1000, \"currency\": \"KRW\"}]}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F14",
                                "GET",
                                "/internal/v1/ledger/transactions/00000000-0000-0000-0000-000000000002",
                                null,
                                "")),
                "/internal/v1/ledger/postings",
                "/api/v1/accounts/invalid-id/balances");
    }
}
