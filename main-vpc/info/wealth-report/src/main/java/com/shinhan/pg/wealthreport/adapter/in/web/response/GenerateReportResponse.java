package com.shinhan.pg.wealthreport.adapter.in.web.response;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/** [MVP4-F01] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GenerateReportResponse(
        UUID reportId, String status, URI downloadUrl, Instant completedAt) {}
