package com.shinhan.pg.payment.application.readmodel;

import java.time.Instant;
import java.util.UUID;

/** 서명 검증과 역직렬화를 마친 이벤트만 표현할 내부 투영. 중복/역순 적용은 별도 단계다. */
public record VerifiedPgEventDraft(
        UUID eventId,
        UUID paymentId,
        String providerCode,
        String eventType,
        Instant occurredAt,
        String externalReference) {}
