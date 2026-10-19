package com.shinhan.pg.account.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] 물리 저장 필드 초안. DB 테이블·컬럼·ORM은 ERD와 후속 확정한다.
 * TODO: @Entity/@Table/@Id 또는 SQL row mapping을 선택한다. 현재 ORM annotation·업무 migration 없음.
 * TODO: 금액/통화·시간대·null·unique·관계 저장을 대조하고 원본 Entity를 API로 반환하지 않는다.
 * </pre>
 *
 * <p>구현 확정: JPA 없이 SQL row mapping(JdbcTemplate)을 사용한다(R7/의존성 제약 — build에 JPA 없음).
 * 물리 매핑은 V3__accounts.sql 기준: id→id, ownerId→owner_id, accountNumber→account_number(unique),
 * currency→currency, alias→alias(null 허용), status→status, createdAt→created_at.
 * 잔액 컬럼은 없다 — 잔액은 ledger가 소유한다.
 */
public final class AccountEntity {
    // TODO: id의 컬럼명·null·인덱스·보호 정책 대조 — V3: id varchar(36) PK
    private final UUID id;
    // TODO: ownerId의 컬럼명·null·인덱스·보호 정책 대조 — V3: owner_id varchar(36) not null, idx_accounts_owner
    private final UUID ownerId;
    // TODO: accountNumber의 컬럼명·null·인덱스·보호 정책 대조 — V3: account_number varchar(32) unique
    private final String accountNumber;
    // TODO: currency의 컬럼명·null·인덱스·보호 정책 대조 — V3: currency char(3) not null
    private final String currency;
    // TODO: alias의 컬럼명·null·인덱스·보호 정책 대조 — V3: alias varchar(100) null 허용
    private final String alias;
    // TODO: status의 컬럼명·null·인덱스·보호 정책 대조 — V3: status varchar(16) not null default 'ACTIVE'
    private final String status;
    // 생성 시각은 DB 기본값(now(6))이 채운다. 도메인 모델에는 노출하지 않는다.
    private final Instant createdAt;

    public AccountEntity(
            UUID id,
            UUID ownerId,
            String accountNumber,
            String currency,
            String alias,
            String status,
            Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.alias = alias;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public String currency() {
        return currency;
    }

    public String alias() {
        return alias;
    }

    public String status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
