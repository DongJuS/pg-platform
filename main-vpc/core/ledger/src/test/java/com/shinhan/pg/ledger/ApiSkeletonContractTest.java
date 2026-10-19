package com.shinhan.pg.ledger;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.ledger.adapter.in.web.InternalLedgerController;
import com.shinhan.pg.ledger.adapter.in.web.LedgerAccountController;
import com.shinhan.pg.ledger.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.ledger.application.port.in.GetBalanceUseCase;
import com.shinhan.pg.ledger.application.port.in.ListJournalEntriesUseCase;
import com.shinhan.pg.ledger.application.port.in.PostJournalUseCase;
import com.shinhan.pg.ledger.application.port.in.QueryJournalUseCase;
import com.shinhan.pg.ledger.config.LedgerSkeletonConfiguration;

import org.junit.jupiter.api.Test;

/**
 * MVP1-F11~F14 실구현 이후의 API 계약 테스트. 기본 프로필 비활성 + 형식 오류 400만 고정한다.
 * 유효 요청의 업무 동작(200/404/422, 멱등, 동시 출금 잔액 불변식)은
 * LedgerPostingConcurrencyIT 등 MariaDB 통합 테스트가 담당한다.
 * UseCase는 이 테스트에서 도달하지 않는 페이크다.
 */
class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndRejectMalformedRequests() throws Exception {
        var ledgerAccountController =
                new LedgerAccountController(
                        (GetBalanceUseCase)
                                command -> {
                                    throw new UnsupportedOperationException();
                                },
                        (ListJournalEntriesUseCase)
                                command -> {
                                    throw new UnsupportedOperationException();
                                });
        var internalLedgerController =
                new InternalLedgerController(
                        (PostJournalUseCase)
                                command -> {
                                    throw new UnsupportedOperationException();
                                },
                        (QueryJournalUseCase)
                                command -> {
                                    throw new UnsupportedOperationException();
                                });
        ApiSkeletonAssertions.implementedEndpoints(
                LedgerSkeletonConfiguration.class,
                new Object[] {ledgerAccountController, internalLedgerController},
                new PlatformExceptionHandler(),
                "/internal/v1/ledger/postings",
                "/api/v1/accounts/invalid-id/balances");
    }
}
