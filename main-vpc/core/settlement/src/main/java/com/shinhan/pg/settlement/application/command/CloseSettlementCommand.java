package com.shinhan.pg.settlement.application.command;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 내부 입력 초안. 추적 requestId와 금융 멱등성·eventId·업무 ID를 구분한다. 외부 wire 계약은 아니다. */
public record CloseSettlementCommand(
        UUID requestId,
        UUID cycleId,
        LocalDate businessDate,
        Instant cutoffAt,
        int cycleNumber,
        int revision) {}
