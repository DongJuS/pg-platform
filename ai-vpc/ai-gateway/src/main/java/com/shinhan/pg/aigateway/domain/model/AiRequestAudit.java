package com.shinhan.pg.aigateway.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP3-F01,MVP3-F02] 순수 업무 모델 초안. 원문 프롬프트·응답·인증정보는 감사 기록에 기본 저장하지 않는다. 상세 저장/조회 정책은 보류다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record AiRequestAudit(
        UUID requestId,
        String callerServiceId,
        String requestHash,
        String decision,
        String reasonCode,
        Integer inputTokens,
        Integer outputTokens,
        Instant createdAt) {
    @Override
    public String toString() {
        return "AiRequestAudit[REDACTED]";
    }
}
