package com.shinhan.pg.ledger.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.util.UUID;

/** [MVP1-F11,MVP1-F19] 순수 업무 모델 초안. 계좌 ID 순서 잠금과 잔액 갱신의 권위자. Redis 보조 락으로 DB 원자성을 대체하지 않는다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record LedgerAccount(UUID id, UUID externalAccountId, Money balance, Long version) {}
