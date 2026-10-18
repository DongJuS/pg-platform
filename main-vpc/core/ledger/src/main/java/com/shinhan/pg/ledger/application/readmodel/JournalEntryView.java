package com.shinhan.pg.ledger.application.readmodel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 조회/응답용 내부 투영 초안. 도메인 Entity나 DB 테이블이 아니며 페이지/필수 필드는 후속 확정한다. */
public record JournalEntryView(
        UUID journalId,
        UUID transactionId,
        BigDecimal signedAmount,
        String currency,
        Instant postedAt) {}
