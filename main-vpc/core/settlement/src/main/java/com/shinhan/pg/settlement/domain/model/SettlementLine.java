package com.shinhan.pg.settlement.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.util.UUID;

/** [MVP7-F02,MVP7-F08] 순수 업무 모델 초안. 거래 효과를 회차에 한 번만 포함한다. 지연 거래 이월·보정 정책은 보류다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record SettlementLine(
        UUID id, UUID cycleId, String effectKey, String institutionCode, Money amount) {}
