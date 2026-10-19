package com.shinhan.pg.transfer.application.readmodel;

import java.time.Instant;
import java.util.UUID;

/** [MVP1-F20] UNKNOWN 복구 스캔의 최소 읽기 투영. 도메인 엔티티가 아니다. */
public record UnknownTransferView(UUID transferId, UUID transactionId, Instant createdAt) {}
