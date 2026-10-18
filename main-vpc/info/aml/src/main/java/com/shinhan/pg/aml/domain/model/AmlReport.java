package com.shinhan.pg.aml.domain.model;

import java.util.UUID;

/** [MVP6-F03,MVP6-F05] 순수 업무 모델 초안. KoFIU Mock 보고 전송 실패는 케이스 취소가 아니다. reportKey로 재전송 중복을 식별한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record AmlReport(
        UUID id,
        UUID caseId,
        String reportKey,
        String reportType,
        String status,
        String receiptReference) {}
