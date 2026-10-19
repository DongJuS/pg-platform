package com.shinhan.pg.common.eventcontracts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * ADR 0001 2-B: Transfer가 이체 상태를 확정한 후 {@link EventTopics#TRANSFER_COMPLETED}로 발행하는 이벤트.
 *
 * <p>payload는 최소 식별자만 담는다. 금액·계좌 등의 상세는 소비자가 이체/원장 조회로 해소한다(payload 비대화 금지).
 * Versioned contracts are additive: V1 필드를 제거·이름 변경하지 않는다. 파괴적 변경은 V2로.</p>
 *
 * @param eventId       이벤트 자체의 고유 식별자(중복 소비 방지용)
 * @param transferId    완료된 이체 식별자
 * @param transactionId 원장 거래 식별자(분개 멱등 키)
 * @param journalId     확정된 분개(journal) 식별자
 * @param occurredAt    이벤트 발생 시각(UTC)
 */
public record TransferCompletedEvent(
        UUID eventId, UUID transferId, UUID transactionId, UUID journalId, Instant occurredAt) {

    public TransferCompletedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(transferId, "transferId");
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(journalId, "journalId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
