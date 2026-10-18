package com.shinhan.pg.ledger.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.util.UUID;

/** [MVP1-F12,MVP1-F13] 순수 업무 모델 초안. 원본 분개 삭제로 취소하지 않고 연계 역분개를 검토한다. 통화·반올림 상세 계산은 임의 확정하지 않는다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record Posting(UUID id, UUID journalId, UUID ledgerAccountId, Money signedAmount) {}
