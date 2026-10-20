package com.shinhan.pg.wealthreport.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Physical asset_reports row. Actor scoping belongs to query/use case, never JSON serialization.
 */
public record AssetReportEntity(
        UUID id,
        UUID userId,
        Instant asOf,
        String status,
        String stage,
        String calculationVersion,
        String failureCode,
        String draftSource,
        Instant completedAt) {}
