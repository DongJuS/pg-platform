package com.shinhan.pg.aigateway.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * <pre>
 * [MVP3-F01,MVP3-F02] 물리 저장 필드 초안. DB 테이블·컬럼·ORM은 ERD와 후속 확정한다.
 * TODO: @Entity/@Table/@Id 또는 SQL row mapping을 선택한다. 현재 ORM annotation·업무 migration 없음.
 * TODO: 금액/통화·시간대·null·unique·관계 저장을 대조하고 원본 Entity를 API로 반환하지 않는다.
 * </pre>
 */
public final class AiRequestAuditEntity {
    // TODO: requestId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID requestId;
    // TODO: callerServiceId의 컬럼명·null·인덱스·보호 정책 대조
    private String callerServiceId;
    // TODO: requestHash의 컬럼명·null·인덱스·보호 정책 대조
    private String requestHash;
    // TODO: decision의 컬럼명·null·인덱스·보호 정책 대조
    private String decision;
    // TODO: reasonCode의 컬럼명·null·인덱스·보호 정책 대조
    private String reasonCode;
    // TODO: inputTokens의 컬럼명·null·인덱스·보호 정책 대조
    private Integer inputTokens;
    // TODO: outputTokens의 컬럼명·null·인덱스·보호 정책 대조
    private Integer outputTokens;
    // TODO: createdAt의 컬럼명·null·인덱스·보호 정책 대조
    private Instant createdAt;
}
