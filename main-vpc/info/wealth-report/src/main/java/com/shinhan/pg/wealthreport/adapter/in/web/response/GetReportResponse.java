package com.shinhan.pg.wealthreport.adapter.in.web.response;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/** [MVP4-F02] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetReportResponse(
        UUID reportId, String status, Instant asOf, URI downloadUrl, Instant urlExpiresAt) {}
