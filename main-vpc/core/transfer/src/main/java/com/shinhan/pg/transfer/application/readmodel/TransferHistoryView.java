package com.shinhan.pg.transfer.application.readmodel;

import java.time.Instant;

/** 조회/응답용 내부 투영 초안. 도메인 Entity나 DB 테이블이 아니며 페이지/필수 필드는 후속 확정한다. */
public record TransferHistoryView(
        String previousStatus, String nextStatus, String reasonCode, Instant occurredAt) {}
