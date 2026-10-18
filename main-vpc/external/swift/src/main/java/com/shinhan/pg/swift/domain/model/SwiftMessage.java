package com.shinhan.pg.swift.domain.model;

import java.util.UUID;

/** [MVP5-F03,MVP5-F05,MVP5-F08] 순수 업무 모델 초안. MT103 입력을 pacs.008 필드로 해석하고 동일 UETR로 재송신한다. 상세 전문은 후속 XSD 계약이다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record SwiftMessage(
        UUID id,
        UUID transferId,
        String uetr,
        String sourceFormat,
        String messageHash,
        String status,
        Integer attemptCount) {}
