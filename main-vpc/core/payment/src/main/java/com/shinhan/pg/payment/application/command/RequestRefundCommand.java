package com.shinhan.pg.payment.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/** [MVP2-F04] 유스케이스 입력 초안. requestId는 추적용이며 멱등키가 아니다. 사용자/서비스 신원은 신뢰한 인증 문맥에서 전달한다. */
public record RequestRefundCommand(
        UUID requestId,
        UUID actorUserId,
        BigDecimal amount,
        String reason,
        UUID paymentId,
        String idempotencyKey) {}
