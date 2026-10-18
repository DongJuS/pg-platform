package com.shinhan.pg.wealthreport.application.result;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/** [MVP4-F01] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record GenerateReportResult(
        UUID reportId, String status, URI downloadUrl, Instant completedAt) {}
