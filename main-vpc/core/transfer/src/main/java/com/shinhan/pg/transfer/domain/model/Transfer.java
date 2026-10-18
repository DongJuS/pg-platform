package com.shinhan.pg.transfer.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.util.UUID;

/** [MVP1-F15,MVP1-F16,MVP1-F17,MVP1-F20] 순수 업무 모델 초안. 중복 요청은 409. UNKNOWN을 실패로 덮지 않고 같은 transactionId로 확인한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record Transfer(
        UUID id,
        UUID actorUserId,
        UUID fromAccountId,
        UUID toAccountId,
        Money amount,
        String idempotencyKey,
        String requestHash,
        UUID transactionId,
        String status) {}
