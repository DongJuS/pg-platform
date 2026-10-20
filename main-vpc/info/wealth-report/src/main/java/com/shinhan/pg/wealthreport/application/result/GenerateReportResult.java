package com.shinhan.pg.wealthreport.application.result;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record GenerateReportResult(
        UUID reportId, String status, URI downloadUrl, Instant completedAt, String draftSource) {}
