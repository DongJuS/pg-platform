package com.shinhan.pg.wealthreport.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP4-F01,MVP4-F02,MVP4-F03,MVP4-F10] 순수 업무 모델 초안. PDF 저장 완료 후 동기 응답. 진행 단계와 asOf는 중단 복구를 위해 별도 보존한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record AssetReport(
        UUID id,
        UUID userId,
        Instant asOf,
        String status,
        String calculationVersion,
        String pdfObjectKey,
        Instant completedAt) {}
