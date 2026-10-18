package com.shinhan.pg.payment.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP2-F06,MVP2-F15] 순수 업무 모델 초안. 서명 검증 뒤 기록한다. 동일 ID의 다른 내용·역순 전이 정책은 미정이다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record PgEvent(
        UUID id,
        String providerCode,
        String externalEventId,
        UUID paymentId,
        String payloadHash,
        Instant occurredAt) {}
