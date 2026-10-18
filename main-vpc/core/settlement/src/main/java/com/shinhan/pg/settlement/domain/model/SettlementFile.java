package com.shinhan.pg.settlement.domain.model;

import java.util.UUID;

/** [MVP7-F02,MVP7-F03,MVP7-F07] 순수 업무 모델 초안. 파일 건수·합계·해시와 결과 파일 대사를 보존한다. 고정길이 필드·순액 산식 상세는 후속이다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record SettlementFile(
        UUID id,
        UUID cycleId,
        String fileKey,
        Integer recordCount,
        String checksum,
        String status) {}
