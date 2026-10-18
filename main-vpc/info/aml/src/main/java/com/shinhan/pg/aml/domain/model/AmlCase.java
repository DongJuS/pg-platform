package com.shinhan.pg.aml.domain.model;

import java.util.UUID;

/** [MVP6-F01,MVP6-F02,MVP6-F04] 순수 업무 모델 초안. 규칙 점수로 보고 대상을 자동 판정한다. 조회는 판정/보고 상태를 바꾸지 않는다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record AmlCase(
        UUID id,
        String caseKey,
        UUID transactionId,
        String ruleVersion,
        Integer riskScore,
        String status) {}
