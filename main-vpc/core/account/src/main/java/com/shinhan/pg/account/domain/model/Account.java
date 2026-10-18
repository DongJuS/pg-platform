package com.shinhan.pg.account.domain.model;

import java.util.UUID;

/** [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] 순수 업무 모델 초안. 계좌 메타데이터만 소유한다. 잔액은 Ledger에서 조회한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record Account(
        UUID id,
        UUID ownerId,
        String accountNumber,
        String currency,
        String alias,
        String status) {}
