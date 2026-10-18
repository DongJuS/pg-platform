package com.shinhan.pg.payment.adapter.in.web.response;

import java.util.UUID;

/** [MVP2-F04] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record RequestRefundResponse(UUID refundId, UUID paymentId, String status) {}
