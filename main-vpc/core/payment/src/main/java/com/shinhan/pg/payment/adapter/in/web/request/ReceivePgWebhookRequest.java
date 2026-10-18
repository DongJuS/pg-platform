package com.shinhan.pg.payment.adapter.in.web.request;

import java.time.Instant;
import java.util.UUID;

/** [MVP2-F06] 요청 DTO 초안. 서명 검증 후 역직렬화하는 payload 자료형이며 Controller는 raw byte[]를 받는다. */
public record ReceivePgWebhookRequest(
        UUID eventId,
        UUID paymentId,
        String providerCode,
        String eventType,
        Instant occurredAt,
        String externalTransactionId) {}
