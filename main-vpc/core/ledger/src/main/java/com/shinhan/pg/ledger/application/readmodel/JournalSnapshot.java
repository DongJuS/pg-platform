package com.shinhan.pg.ledger.application.readmodel;

import java.util.UUID;

/** 저장소 seam을 위한 최소 읽기 투영. 도메인 엔티티나 확정 DB 스키마가 아니다. */
public record JournalSnapshot(UUID id) {}
