package com.shinhan.pg.payment.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.util.UUID;

/** [MVP2-F04,MVP2-F05,MVP2-F16] 순수 업무 모델 초안. 확정 환불+진행 중 예약액이 승인액을 넘지 않도록 원자적으로 보호한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record Refund(UUID id, UUID paymentId, Money amount, String idempotencyKey, String status) {}
