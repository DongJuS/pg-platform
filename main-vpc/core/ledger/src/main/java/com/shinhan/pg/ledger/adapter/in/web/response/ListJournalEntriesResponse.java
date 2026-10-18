package com.shinhan.pg.ledger.adapter.in.web.response;

import com.shinhan.pg.ledger.application.readmodel.JournalEntryView;

import java.util.List;

/** [MVP1-F12] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record ListJournalEntriesResponse(List<JournalEntryView> entries, String nextCursor) {}
