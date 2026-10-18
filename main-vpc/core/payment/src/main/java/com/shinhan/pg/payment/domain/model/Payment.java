package com.shinhan.pg.payment.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.util.UUID;

/** [MVP2-F01,MVP2-F02,MVP2-F15] 순수 업무 모델 초안. 동기 응답·웹훅을 같은 완료 경로로 수렴시킨다. 승인 불명 중 재승인하지 않는다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record Payment(
        UUID id,
        UUID actorUserId,
        UUID merchantId,
        String orderReference,
        Money approvedAmount,
        Money refundedAmount,
        Money reservedRefundAmount,
        String status) {}
