package com.shinhan.pg.account.adapter.out.persistence.entity;

import java.util.UUID;

/**
 * <pre>
 * [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] 물리 저장 필드 초안. DB 테이블·컬럼·ORM은 ERD와 후속 확정한다.
 * TODO: @Entity/@Table/@Id 또는 SQL row mapping을 선택한다. 현재 ORM annotation·업무 migration 없음.
 * TODO: 금액/통화·시간대·null·unique·관계 저장을 대조하고 원본 Entity를 API로 반환하지 않는다.
 * </pre>
 */
public final class AccountEntity {
    // TODO: id의 컬럼명·null·인덱스·보호 정책 대조
    private UUID id;
    // TODO: ownerId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID ownerId;
    // TODO: accountNumber의 컬럼명·null·인덱스·보호 정책 대조
    private String accountNumber;
    // TODO: currency의 컬럼명·null·인덱스·보호 정책 대조
    private String currency;
    // TODO: alias의 컬럼명·null·인덱스·보호 정책 대조
    private String alias;
    // TODO: status의 컬럼명·null·인덱스·보호 정책 대조
    private String status;
}
