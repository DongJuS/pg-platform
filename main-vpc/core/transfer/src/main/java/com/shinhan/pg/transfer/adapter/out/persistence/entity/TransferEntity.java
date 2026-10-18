package com.shinhan.pg.transfer.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F15,MVP1-F16,MVP1-F17,MVP1-F20] 물리 저장 필드 초안. DB 테이블·컬럼·ORM은 ERD와 후속 확정한다.
 * TODO: @Entity/@Table/@Id 또는 SQL row mapping을 선택한다. 현재 ORM annotation·업무 migration 없음.
 * TODO: 금액/통화·시간대·null·unique·관계 저장을 대조하고 원본 Entity를 API로 반환하지 않는다.
 * </pre>
 */
public final class TransferEntity {
    // TODO: id의 컬럼명·null·인덱스·보호 정책 대조
    private UUID id;
    // TODO: actorUserId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID actorUserId;
    // TODO: fromAccountId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID fromAccountId;
    // TODO: toAccountId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID toAccountId;
    // TODO: amount의 컬럼명·null·인덱스·보호 정책 대조
    private BigDecimal amount;
    // TODO: amountCurrency의 컬럼명·null·인덱스·보호 정책 대조
    private String amountCurrency;
    // TODO: idempotencyKey의 컬럼명·null·인덱스·보호 정책 대조
    private String idempotencyKey;
    // TODO: requestHash의 컬럼명·null·인덱스·보호 정책 대조
    private String requestHash;
    // TODO: transactionId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID transactionId;
    // TODO: status의 컬럼명·null·인덱스·보호 정책 대조
    private String status;
}
