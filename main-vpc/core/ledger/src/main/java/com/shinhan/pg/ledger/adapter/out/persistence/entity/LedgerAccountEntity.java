package com.shinhan.pg.ledger.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F11,MVP1-F19] 물리 저장 필드 초안. DB 테이블·컬럼·ORM은 ERD와 후속 확정한다.
 * TODO: @Entity/@Table/@Id 또는 SQL row mapping을 선택한다. 현재 ORM annotation·업무 migration 없음.
 * TODO: 금액/통화·시간대·null·unique·관계 저장을 대조하고 원본 Entity를 API로 반환하지 않는다.
 * </pre>
 *
 * <p>구현: ORM 없이 SQL row mapping(JdbcTemplate). V3__ledger.sql의 ledger_accounts 행에 대응한다.
 * id는 원장 내부 식별자, externalAccountId는 account 서비스의 계좌 ID(unique).
 */
public record LedgerAccountEntity(
        UUID id, UUID externalAccountId, BigDecimal balance, String balanceCurrency, Long version) {}
