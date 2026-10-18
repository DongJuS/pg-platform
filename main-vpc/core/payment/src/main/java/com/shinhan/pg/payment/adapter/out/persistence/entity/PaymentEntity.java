package com.shinhan.pg.payment.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F01,MVP2-F02,MVP2-F15] 물리 저장 필드 초안. DB 테이블·컬럼·ORM은 ERD와 후속 확정한다.
 * TODO: @Entity/@Table/@Id 또는 SQL row mapping을 선택한다. 현재 ORM annotation·업무 migration 없음.
 * TODO: 금액/통화·시간대·null·unique·관계 저장을 대조하고 원본 Entity를 API로 반환하지 않는다.
 * </pre>
 */
public final class PaymentEntity {
    // TODO: id의 컬럼명·null·인덱스·보호 정책 대조
    private UUID id;
    // TODO: actorUserId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID actorUserId;
    // TODO: merchantId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID merchantId;
    // TODO: orderReference의 컬럼명·null·인덱스·보호 정책 대조
    private String orderReference;
    // TODO: approvedAmount의 컬럼명·null·인덱스·보호 정책 대조
    private BigDecimal approvedAmount;
    // TODO: approvedAmountCurrency의 컬럼명·null·인덱스·보호 정책 대조
    private String approvedAmountCurrency;
    // TODO: refundedAmount의 컬럼명·null·인덱스·보호 정책 대조
    private BigDecimal refundedAmount;
    // TODO: refundedAmountCurrency의 컬럼명·null·인덱스·보호 정책 대조
    private String refundedAmountCurrency;
    // TODO: reservedRefundAmount의 컬럼명·null·인덱스·보호 정책 대조
    private BigDecimal reservedRefundAmount;
    // TODO: reservedRefundAmountCurrency의 컬럼명·null·인덱스·보호 정책 대조
    private String reservedRefundAmountCurrency;
    // TODO: status의 컬럼명·null·인덱스·보호 정책 대조
    private String status;
}
