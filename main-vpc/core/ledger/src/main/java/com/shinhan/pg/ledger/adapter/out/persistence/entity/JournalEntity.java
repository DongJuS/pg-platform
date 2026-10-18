package com.shinhan.pg.ledger.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F13,MVP1-F14] 물리 저장 필드 초안. DB 테이블·컬럼·ORM은 ERD와 후속 확정한다.
 * TODO: @Entity/@Table/@Id 또는 SQL row mapping을 선택한다. 현재 ORM annotation·업무 migration 없음.
 * TODO: 금액/통화·시간대·null·unique·관계 저장을 대조하고 원본 Entity를 API로 반환하지 않는다.
 * </pre>
 */
public final class JournalEntity {
    // TODO: id의 컬럼명·null·인덱스·보호 정책 대조
    private UUID id;
    // TODO: transactionId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID transactionId;
    // TODO: postingsIds의 컬럼명·null·인덱스·보호 정책 대조
    private List<UUID> postingsIds;
    // TODO: postedAt의 컬럼명·null·인덱스·보호 정책 대조
    private Instant postedAt;
}
