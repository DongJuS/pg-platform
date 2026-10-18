package com.shinhan.pg.aml.adapter.out.persistence.entity;

import java.util.UUID;

/**
 * <pre>
 * [MVP6-F03,MVP6-F05] 물리 저장 필드 초안. DB 테이블·컬럼·ORM은 ERD와 후속 확정한다.
 * TODO: @Entity/@Table/@Id 또는 SQL row mapping을 선택한다. 현재 ORM annotation·업무 migration 없음.
 * TODO: 금액/통화·시간대·null·unique·관계 저장을 대조하고 원본 Entity를 API로 반환하지 않는다.
 * </pre>
 */
public final class AmlReportEntity {
    // TODO: id의 컬럼명·null·인덱스·보호 정책 대조
    private UUID id;
    // TODO: caseId의 컬럼명·null·인덱스·보호 정책 대조
    private UUID caseId;
    // TODO: reportKey의 컬럼명·null·인덱스·보호 정책 대조
    private String reportKey;
    // TODO: reportType의 컬럼명·null·인덱스·보호 정책 대조
    private String reportType;
    // TODO: status의 컬럼명·null·인덱스·보호 정책 대조
    private String status;
    // TODO: receiptReference의 컬럼명·null·인덱스·보호 정책 대조
    private String receiptReference;
}
