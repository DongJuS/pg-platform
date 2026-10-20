package com.shinhan.pg.swift.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.swift.application.command.RecordSwiftAcknowledgementCommand;
import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.port.in.RecordSwiftAcknowledgementUseCase;
import com.shinhan.pg.swift.application.port.in.RetrySwiftUseCase;
import com.shinhan.pg.swift.application.port.in.SubmitSwiftUseCase;
import com.shinhan.pg.swift.application.port.out.FepSwiftPort;
import com.shinhan.pg.swift.application.result.SubmitSwiftResult;

/**
 * 기능별 계약과 TODO를 모은 application 구현 자리. Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB
 * 트랜잭션 안에서 수행하지 않는다. Active development flow uses durable submission and source instruction ports;
 * untyped acknowledgements are rejected.
 */
public final class SwiftApplicationService
        implements SubmitSwiftUseCase, RetrySwiftUseCase, RecordSwiftAcknowledgementUseCase {
    private final FepSwiftPort fepSwiftPort;
    private final com.shinhan.pg.swift.application.port.out.SwiftSubmissionStore submissions;
    private final com.shinhan.pg.swift.application.port.out.TransferInstructionPort instructions;
    private final boolean enabled;
    private final boolean retransmitEnabled;
    private final int maximum;
    private final long minimumSeconds;

    public SwiftApplicationService(
            FepSwiftPort fepSwiftPort,
            com.shinhan.pg.swift.application.port.out.SwiftSubmissionStore submissions,
            com.shinhan.pg.swift.application.port.out.TransferInstructionPort instructions,
            boolean enabled,
            boolean retransmitEnabled,
            int maximum,
            long minimumSeconds) {
        this.submissions = submissions;
        this.instructions = instructions;
        this.enabled = enabled;
        this.retransmitEnabled = retransmitEnabled;
        com.shinhan.pg.common.swiftmessages.SwiftProblem.require(
                maximum >= 1 && maximum <= 10 && minimumSeconds >= 1 && minimumSeconds <= 86400);
        this.maximum = maximum;
        this.minimumSeconds = minimumSeconds;
        this.fepSwiftPort = java.util.Objects.requireNonNull(fepSwiftPort);
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F03] Kafka transfer.outbound 수신 → FEP REST 전송. 주 진입점은 HTTP가 아니다.
     * Stored Transfer UETR → MT103 supported subset → frozen pacs.008 → durable submission → FEP. Parse errors never authorize an automatic financial reversal.
     * Implemented: local durable claims/state/Outbox; HTTP is outside the transaction.
     * </pre>
     */
    @Override
    public SubmitSwiftResult submitSwift(SubmitSwiftCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        requireEnabled();
        com.shinhan.pg.common.swiftmessages.SwiftIds.uetr(command.uetr());
        var existing = submissions.find(command.uetr());
        if (existing.isEmpty()) {
            var r = instructions.get(command.transferId());
            if (!command.uetr().equals(r.uetr()))
                throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
            var mt = com.shinhan.pg.common.swiftmessages.Mt103Subset.write(r);
            var parsed =
                    new com.shinhan.pg.swift.domain.policy.Mt103Parser()
                            .parse(r.transferId(), r.uetr(), mt);
            // A frozen source date makes byte identity deterministic even for concurrent event
            // deliveries.
            var xml =
                    com.shinhan.pg.common.swiftmessages.Iso20022.payment(
                            parsed,
                            r.valueDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
            submissions.register(parsed, mt, xml);
        } else if (!command.transferId().equals(existing.get().transferId()))
            throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
        dispatch(command, false);
        return new SubmitSwiftResult(command.transferId());
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F05] Explicit operator retransmission only: same stored bytes/UETR after query, UNKNOWN, interval and attempt checks.
     * Development defaults: initially disabled, maximum 3 sends including first, minimum interval 30 seconds.
     * Implemented: local durable claims/state/Outbox; HTTP is outside the transaction.
     * </pre>
     */
    @Override
    public void retrySwift(RetrySwiftCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        requireEnabled();
        if (!retransmitEnabled)
            throw new com.shinhan.pg.common.swiftmessages.SwiftProblem(
                    "RETRANSMIT_DISABLED", com.shinhan.pg.common.commonerror.ErrorType.CONFLICT);
        var row =
                submissions
                        .find(command.uetr())
                        .orElseThrow(com.shinhan.pg.common.swiftmessages.SwiftProblem::invalid);
        if (!command.transferId().equals(row.transferId()))
            throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
        // A status query never creates a new payment. A known intermediate/final response blocks
        // retransmission.
        if (query(command.uetr()))
            throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
        if (!dispatch(
                new SubmitSwiftCommand(command.requestId(), command.transferId(), command.uetr()),
                true)) throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F06,MVP5-F08] FEP REST/pacs.002 결과 판정 후 fep.ack 의도를 기록할 내부 유스케이스.
     * Validated pacs.002 → atomic Swift state/fep.ack; Ledger separately applies settlement/reversal. Bare uncorrelated status codes are rejected.
     * TODO: fep.ack 발행자·소비자와 최종 상태 갱신 책임을 정함. 수신 확인·진행 중·최종 결과를 구분하고 확정·역분개는 멱등 처리. / 예외: 미확정 응답·계속 무응답은 확인 대기. 지원 상태 코드와 최종성 정의는 보류.
     * Implemented: local durable claims/state/Outbox; HTTP is outside the transaction.
     * </pre>
     */
    @Override
    public void recordSwiftAcknowledgement(RecordSwiftAcknowledgementCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        // Bare status codes are not a trusted result contract: require validated, correlated
        // pacs.002.
        throw com.shinhan.pg.common.swiftmessages.SwiftProblem.invalid();
    }

    private boolean dispatch(SubmitSwiftCommand c, boolean resend) {
        var worker = java.util.UUID.randomUUID();
        if (!submissions.claim(c.uetr(), worker, resend, maximum, minimumSeconds)) return false;
        var row = submissions.find(c.uetr()).orElseThrow();
        try {
            if (resend)
                fepSwiftPort.retry(new RetrySwiftCommand(c.requestId(), c.transferId(), c.uetr()));
            else fepSwiftPort.send(c, row.xml(), row.reference());
            submissions.sent(c.uetr(), worker, false);
        } catch (RuntimeException unavailable) {
            submissions.sent(c.uetr(), worker, true);
        }
        query(c.uetr());
        return true;
    }

    public boolean query(String uetr) {
        requireEnabled();
        try {
            var observation = fepSwiftPort.queryResult(uetr);
            if (observation.transmissionUnknown()) submissions.transmissionUnknown(uetr);
            var reports = observation.reports();
            if (reports.isEmpty()) return false;
            for (var xml : reports) {
                var result = com.shinhan.pg.common.swiftmessages.Iso20022.result(xml);
                submissions.result(uetr, xml, result);
            }
            return true;
        } catch (com.shinhan.pg.common.swiftmessages.SwiftProblem unavailable) {
            return false;
        } finally {
            submissions.deferQuery(uetr);
        }
    }

    public void recover() {
        if (!enabled) return;
        submissions.purge();
        for (var row : submissions.due()) {
            if ("READY".equals(row.status()) && row.xml() != null)
                dispatch(
                        new SubmitSwiftCommand(
                                java.util.UUID.randomUUID(), row.transferId(), row.uetr()),
                        false);
            else query(row.uetr());
        }
    }

    public java.util.List<String> history(String uetr, boolean raw) {
        requireEnabled();
        return submissions.history(uetr, raw);
    }

    private void requireEnabled() {
        if (!enabled) throw new FeatureNotImplementedException("MVP5");
    }
}
