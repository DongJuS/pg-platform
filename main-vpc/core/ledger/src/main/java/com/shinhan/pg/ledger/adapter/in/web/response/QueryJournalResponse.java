package com.shinhan.pg.ledger.adapter.in.web.response;

import java.util.UUID;

/** [MVP1-F14] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record QueryJournalResponse(
        UUID transactionId, UUID journalId, UUID eventId, String status) {}
