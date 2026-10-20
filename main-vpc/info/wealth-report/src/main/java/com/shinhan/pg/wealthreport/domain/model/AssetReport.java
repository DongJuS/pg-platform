package com.shinhan.pg.wealthreport.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Durable generation receipt. No raw prompt, narrative, credentials or token mappings. */
public record AssetReport(
        UUID id,
        UUID userId,
        Instant asOf,
        String status,
        String stage,
        String calculationVersion,
        String failureCode,
        String draftSource,
        Instant completedAt) {}
