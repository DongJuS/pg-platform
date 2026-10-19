package com.shinhan.pg.transfer.adapter.in.kafka;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.common.eventcontracts.LedgerPostedEvent;
import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.port.in.CompleteTransferUseCase;
import com.shinhan.pg.transfer.application.port.out.TransferQueryPort;
import com.shinhan.pg.transfer.domain.error.TransferException;

import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

/**
 * <pre>
 * [MVP1-F15,MVP1-F20] Kafka ledger.posted 반영 또는 REST 결과 확정. transfer.completed 발행 책임은 Transfer에 있다.
 * 같은 키 재요청은 중복 오류로 알리고 새 이체를 만들지 않음. 복구 작업은 기존 transactionId로 원장 결과 확인.
 * </pre>
 *
 * <p>WO-A6 실구현: ledger.posted(byte[] payload, WO-A3 EventCodec 계약)를 소비해 completeTransfer로
 * 연결한다. REST 동기 완료와 동일한 완료 경로이며, 중복 소비·REST와의 경쟁은
 * completeOnceWithOutbox의 조건부 update가 멱등 흡수한다.
 * ack는 완료 처리가 커밋된 뒤에만 보낸다(manual_immediate) — 실패 시 재전달되지만 멱등 경로라 안전하다.
 * 이 서비스가 만들지 않은 transactionId(다른 서비스 거래)는 처리하지 않고 ack 후 건너뛴다.
 * ack/DLQ 최종 계약(재시도 횟수·DLQ 토픽)은 미정 — 현재는 컨테이너 기본 에러 처리에 위임한다.</p>
 */
public final class CompleteTransferConsumerSkeleton {
    private final CompleteTransferUseCase useCase;
    private final TransferQueryPort transferQueryPort;

    public CompleteTransferConsumerSkeleton(
            CompleteTransferUseCase useCase, TransferQueryPort transferQueryPort) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
        this.transferQueryPort = java.util.Objects.requireNonNull(transferQueryPort);
    }

    /** 기존 진입점(직접 command 주입). Kafka 경로와 동일한 완료 처리를 호출한다. */
    public void handle(CompleteTransferCommand command) {
        useCase.completeTransfer(command);
    }

    @KafkaListener(topics = EventTopics.LEDGER_POSTED)
    public void onLedgerPosted(byte[] payload, Acknowledgment acknowledgment) {
        var event = EventCodec.fromBytes(payload, LedgerPostedEvent.class);
        var transferId = transferQueryPort.findTransferIdByTransactionId(event.transactionId());
        if (transferId.isEmpty()) {
            // 다른 서비스 소유 거래의 ledger.posted — 이 컨슈머의 처리 대상이 아니다.
            LoggerFactory.getLogger(getClass())
                    .debug("ledger.posted for unknown transactionId {} — skipped", event.transactionId());
            acknowledgment.acknowledge();
            return;
        }
        try {
            useCase.completeTransfer(
                    new CompleteTransferCommand(
                            event.eventId(),
                            transferId.get(),
                            event.transactionId(),
                            event.journalId()));
        } catch (TransferException business) {
            // 최종 상태 역전 등 불허 전이 — 재전달로 해소되지 않는다. ack 후 관측에 맡긴다.
            LoggerFactory.getLogger(getClass())
                    .warn("ledger.posted rejected: {} — acked without retry", business.getMessage());
            acknowledgment.acknowledge();
            return;
        }
        acknowledgment.acknowledge();
    }
}
