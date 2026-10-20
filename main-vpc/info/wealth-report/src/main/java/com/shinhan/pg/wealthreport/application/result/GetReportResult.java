package com.shinhan.pg.wealthreport.application.result;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record GetReportResult(
        UUID reportId,
        String status,
        Instant asOf,
        URI downloadUrl,
        Instant urlExpiresAt,
        String stage,
        String failureCode,
        String draftSource) {}
