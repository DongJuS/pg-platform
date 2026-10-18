package com.shinhan.pg.wealthreport.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP4-F06] 순수 업무 모델 초안. 원본↔토큰 매핑은 내부에서만 저장·복원. 저장 보호 방식·보존 기간은 미정이다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record TokenMapping(
        UUID reportId, String tokenLabel, String protectedOriginalValue, Instant expiresAt) {
    @Override
    public String toString() {
        return "TokenMapping[REDACTED]";
    }
}
