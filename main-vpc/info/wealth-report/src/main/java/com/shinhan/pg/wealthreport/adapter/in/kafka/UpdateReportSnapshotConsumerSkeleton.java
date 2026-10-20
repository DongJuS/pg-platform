package com.shinhan.pg.wealthreport.adapter.in.kafka;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.LedgerPostedEvent;
import com.shinhan.pg.common.eventcontracts.TransferCompletedEvent;
import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;
import com.shinhan.pg.wealthreport.application.port.in.UpdateReportSnapshotUseCase;

import org.springframework.kafka.annotation.KafkaListener;

/**
 * Notification-only projection. Listener returns only after durable local commit, failures are not
 * acked. No report generation, fake payment event contract or cross-topic amount accumulation.
 */
public final class UpdateReportSnapshotConsumerSkeleton {
    private final UpdateReportSnapshotUseCase useCase;

    public UpdateReportSnapshotConsumerSkeleton(UpdateReportSnapshotUseCase u) {
        useCase = u;
    }

    public void handle(UpdateReportSnapshotCommand c) {
        useCase.updateReportSnapshot(c);
    }

    @KafkaListener(
            containerFactory = "reportKafkaFactory",
            topics = "ledger.posted",
            groupId = "wealth-report-snapshots",
            autoStartup = "${platform.report.events.enabled:false}")
    public void ledger(byte[] payload, org.springframework.kafka.support.Acknowledgment ack) {
        var e = EventCodec.fromBytes(payload, LedgerPostedEvent.class);
        handle(
                new UpdateReportSnapshotCommand(
                        "ledger.posted",
                        e.eventId(),
                        e.transactionId(),
                        e.journalId(),
                        e.occurredAt()));
        ack.acknowledge();
    }

    @KafkaListener(
            containerFactory = "reportKafkaFactory",
            topics = "transfer.completed",
            groupId = "wealth-report-snapshots",
            autoStartup = "${platform.report.events.enabled:false}")
    public void transfer(byte[] payload, org.springframework.kafka.support.Acknowledgment ack) {
        var e = EventCodec.fromBytes(payload, TransferCompletedEvent.class);
        handle(
                new UpdateReportSnapshotCommand(
                        "transfer.completed",
                        e.eventId(),
                        e.transactionId(),
                        e.journalId(),
                        e.occurredAt()));
        ack.acknowledge();
    }
}
