package com.shinhan.pg.transfer.application.readmodel;

import java.util.UUID;

/** 원장 REST 결과와 ledger.posted가 공유할 안정적 식별자 초안. 실제 wire 계약은 후속이다. */
public record LedgerPostingReceiptDraft(UUID transactionId, UUID journalId, UUID eventId) {}
