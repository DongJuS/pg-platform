package com.shinhan.pg.aml.domain.model;

import java.util.UUID;

/**
 * [MVP6-F01,MVP6-F02,MVP6-F04] 업무 조회 모델. 규칙 점수로 보고 대상을 자동 판정한다. 조회는 판정/보고 상태를 바꾸지 않는다. 판정/보고 전이는
 * 원자적 workflow가 관리한다. ORM Entity가 아니다.
 */
public record AmlCase(
        UUID id,
        String caseKey,
        UUID transactionId,
        String ruleVersion,
        Integer riskScore,
        String status,
        java.time.Instant createdAt) {
    public AmlCase(
            UUID id,
            String caseKey,
            UUID transactionId,
            String ruleVersion,
            Integer riskScore,
            String status) {
        this(id, caseKey, transactionId, ruleVersion, riskScore, status, null);
    }
}
