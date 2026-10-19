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
 *
 * <p>구현: ORM 없이 SQL row mapping(JdbcTemplate). V3__ledger.sql의 journals 행에 대응한다.
 * postings는 1:N 관계를 postings 테이블 조회 결과로 채운 읽기 투영이다(저장 시에는 무시한다).
 */
public record JournalEntity(
        UUID id, UUID transactionId, List<PostingEntity> postings, Instant postedAt) {}
