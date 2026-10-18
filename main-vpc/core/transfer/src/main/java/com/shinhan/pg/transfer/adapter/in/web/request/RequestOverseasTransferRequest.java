package com.shinhan.pg.transfer.adapter.in.web.request;

import java.math.BigDecimal;
import java.util.UUID;

/** [MVP5-F01] 요청 DTO 초안. 신원/멱등키/경로 변수는 본문과 구분한다. 검증 제약은 주석의 계약을 따라 추가한다. */
public record RequestOverseasTransferRequest(
        UUID fromAccountId,
        String recipientName,
        String recipientAccount,
        String bic,
        BigDecimal amount,
        String currency) {
    @Override
    public String toString() {
        return "RequestOverseasTransferRequest[REDACTED]";
    }
}
