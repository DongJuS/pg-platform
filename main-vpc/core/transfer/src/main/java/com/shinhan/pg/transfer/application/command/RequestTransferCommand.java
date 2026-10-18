package com.shinhan.pg.transfer.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/** [MVP1-F15] 유스케이스 입력 초안. requestId는 추적용이며 멱등키가 아니다. 사용자/서비스 신원은 신뢰한 인증 문맥에서 전달한다. */
public record RequestTransferCommand(
        UUID requestId,
        UUID actorUserId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String otpToken,
        String idempotencyKey) {
    @Override
    public String toString() {
        return "RequestTransferCommand[REDACTED]";
    }
}
