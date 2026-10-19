package com.shinhan.pg.transfer.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.command.GetTransferCommand;
import com.shinhan.pg.transfer.application.command.ListTransfersCommand;
import com.shinhan.pg.transfer.application.command.RecoverTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.LedgerOutcomeUnknownException;
import com.shinhan.pg.transfer.application.port.out.LedgerPostingPort;
import com.shinhan.pg.transfer.application.port.out.OtpVerificationPort;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferDetailsRepository;
import com.shinhan.pg.transfer.application.port.out.TransferAtomicWritePort;
import com.shinhan.pg.transfer.application.port.out.TransferHistoryRepository;
import com.shinhan.pg.transfer.application.port.out.TransferQueryPort;
import com.shinhan.pg.transfer.application.port.out.TransferRepository;
import com.shinhan.pg.transfer.application.readmodel.LedgerPostingReceiptDraft;
import com.shinhan.pg.transfer.application.readmodel.TransferReservationDraft;
import com.shinhan.pg.transfer.application.readmodel.TransferSummary;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * [WO-A6] requestTransfer 동기 완료 경로를 mock ledger로 검증한다.
 * 원장 호출은 로컬 DB 트랜잭션 밖이라는 전제 하에, 예약→post→완료/UNKNOWN/FAILED 분기만 본다.
 */
class TransferApplicationServiceTest {
    private static final UUID ACTOR = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID FROM = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID TO = UUID.fromString("20000000-0000-0000-0000-000000000003");

    private LedgerPostingPort ledger;
    private TransferAtomicWritePort atomicWrite;
    private TransferRepository transferRepository;
    private TransferQueryPort transferQueryPort;
    private TransferApplicationService service;

    @BeforeEach
    void setUp() {
        ledger = mock(LedgerPostingPort.class);
        atomicWrite = mock(TransferAtomicWritePort.class);
        transferRepository = mock(TransferRepository.class);
        transferQueryPort = mock(TransferQueryPort.class);
        service =
                new TransferApplicationService(
                        ledger,
                        mock(OtpVerificationPort.class),
                        atomicWrite,
                        transferRepository,
                        mock(TransferHistoryRepository.class),
                        mock(OverseasTransferDetailsRepository.class),
                        transferQueryPort, id -> new com.shinhan.pg.transfer.application.readmodel.AccountAccessSnapshot(id, ACTOR, "KRW", "ACTIVE"));
    }

    private RequestTransferCommand command() {
        return new RequestTransferCommand(
                UUID.randomUUID(), ACTOR, FROM, TO, new BigDecimal("1000"), "KRW", "test-proof", "key-1");
    }

    @Test
    void synchronousCompletionPath() {
        var command = command();
        var draft = new TransferReservationDraft(UUID.randomUUID(), UUID.randomUUID());
        var receipt = new LedgerPostingReceiptDraft(
                draft.transactionId(), UUID.randomUUID(), UUID.randomUUID());
        when(atomicWrite.reserveRequest(command)).thenReturn(draft);
        when(ledger.post(draft.transactionId(), command)).thenReturn(receipt);

        var result = service.requestTransfer(command);

        assertEquals(draft.transferId(), result.transferId());
        assertEquals(draft.transactionId(), result.transactionId());
        assertEquals("COMPLETED", result.status());
        verify(atomicWrite)
                .completeOnceWithOutbox(
                        new CompleteTransferCommand(
                                receipt.eventId(),
                                draft.transferId(),
                                draft.transactionId(),
                                receipt.journalId()));
        verify(atomicWrite, never()).markUnknown(any());
        verify(atomicWrite, never()).markFailed(any(), any());
    }

    @Test
    void timeoutLeavesUnknownAndNeverReposts() {
        var command = command();
        var draft = new TransferReservationDraft(UUID.randomUUID(), UUID.randomUUID());
        when(atomicWrite.reserveRequest(command)).thenReturn(draft);
        when(ledger.post(draft.transactionId(), command))
                .thenThrow(new LedgerOutcomeUnknownException("read timeout", null));

        var result = service.requestTransfer(command);

        // 결과 불명 — 즉시 실패 확정 금지. UNKNOWN으로 두고 재조회로 수렴한다.
        assertEquals("UNKNOWN", result.status());
        verify(atomicWrite).markUnknown(draft.transferId());
        verify(atomicWrite, never()).completeOnceWithOutbox(any());
        verify(atomicWrite, never()).markFailed(any(), any());
        verify(ledger, never()).findJournal(any());
    }

    @Test
    void ledgerRejectionMarksFailedAndPropagates422Semantics() {
        var command = command();
        var draft = new TransferReservationDraft(UUID.randomUUID(), UUID.randomUUID());
        when(atomicWrite.reserveRequest(command)).thenReturn(draft);
        when(ledger.post(draft.transactionId(), command))
                .thenThrow(
                        new TransferException(
                                TransferErrorCode.INSUFFICIENT_FUNDS, "Ledger rejected"));

        var error = assertThrows(TransferException.class, () -> service.requestTransfer(command));
        assertEquals(TransferErrorCode.INSUFFICIENT_FUNDS, error.code());
        verify(atomicWrite).markFailed(draft.transferId(), "LEDGER_REJECTED");
        verify(atomicWrite, never()).completeOnceWithOutbox(any());
        verify(atomicWrite, never()).markUnknown(any());
    }

    @Test
    void duplicateReservationPropagates409WithoutLedgerCall() {
        var command = command();
        var existingId = UUID.randomUUID();
        when(atomicWrite.reserveRequest(command))
                .thenThrow(
                        new TransferException(
                                TransferErrorCode.DUPLICATE_REQUEST, "duplicate", existingId));

        var error = assertThrows(TransferException.class, () -> service.requestTransfer(command));
        assertEquals(TransferErrorCode.DUPLICATE_REQUEST, error.code());
        assertEquals(existingId, error.existingTransferId());
        verify(ledger, never()).post(any(), any());
    }

    @Test
    void requestValidation() {
        var base = command();
        // 인증 문맥 부재 → 401 의미
        var noActor =
                assertThrows(
                        TransferException.class,
                        () ->
                                service.requestTransfer(
                                        new RequestTransferCommand(
                                                base.requestId(), null, FROM, TO,
                                                base.amount(), "KRW", null, "k")));
        assertEquals(TransferErrorCode.UNAUTHENTICATED, noActor.code());
        // 금액 0 → 400
        assertThrows(
                TransferException.class,
                () ->
                        service.requestTransfer(
                                new RequestTransferCommand(
                                        UUID.randomUUID(), ACTOR, FROM, TO,
                                        BigDecimal.ZERO, "KRW", null, "k")));
        // KRW 소수(scale 위반) → 400 (common-money UNNECESSARY 계약)
        assertThrows(
                TransferException.class,
                () ->
                        service.requestTransfer(
                                new RequestTransferCommand(
                                        UUID.randomUUID(), ACTOR, FROM, TO,
                                        new BigDecimal("10.5"), "KRW", null, "k")));
        // 동일 출·입금 계좌 → 400
        assertThrows(
                TransferException.class,
                () ->
                        service.requestTransfer(
                                new RequestTransferCommand(
                                        UUID.randomUUID(), ACTOR, FROM, FROM,
                                        base.amount(), "KRW", null, "k")));
        // 멱등키 부재 → 400
        assertThrows(
                TransferException.class,
                () ->
                        service.requestTransfer(
                                new RequestTransferCommand(
                                        UUID.randomUUID(), ACTOR, FROM, TO,
                                        base.amount(), "KRW", null, " ")));
        verify(atomicWrite, never()).reserveRequest(any());
    }

    @Test
    void getTransferEnforcesOwnership() {
        var transferId = UUID.randomUUID();
        var transfer =
                new com.shinhan.pg.transfer.domain.model.Transfer(
                        transferId, ACTOR, FROM, TO,
                        com.shinhan.pg.common.commonmoney.Money.of(1000),
                        "k", "h", UUID.randomUUID(), "COMPLETED");
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(transferQueryPort.findHistories(transferId)).thenReturn(List.of());

        var result = service.getTransfer(new GetTransferCommand(UUID.randomUUID(), ACTOR, transferId));
        assertEquals("COMPLETED", result.status());

        var stranger =
                assertThrows(
                        TransferException.class,
                        () ->
                                service.getTransfer(
                                        new GetTransferCommand(
                                                UUID.randomUUID(), UUID.randomUUID(), transferId)));
        assertEquals(TransferErrorCode.ACCOUNT_FORBIDDEN, stranger.code());

        var missingId = UUID.randomUUID();
        when(transferRepository.findById(missingId)).thenReturn(Optional.empty());
        var missing =
                assertThrows(
                        TransferException.class,
                        () ->
                                service.getTransfer(
                                        new GetTransferCommand(
                                                UUID.randomUUID(), ACTOR, missingId)));
        assertEquals(TransferErrorCode.NOT_FOUND, missing.code());
    }

    @Test
    void listTransfersPaginatesAndValidates() {
        var summaries =
                List.of(
                        new TransferSummary(UUID.randomUUID(), BigDecimal.TEN, "KRW", "COMPLETED",
                                java.time.Instant.now()),
                        new TransferSummary(UUID.randomUUID(), BigDecimal.ONE, "KRW", "PROCESSING",
                                java.time.Instant.now()),
                        new TransferSummary(UUID.randomUUID(), BigDecimal.TWO, "KRW", "UNKNOWN",
                                java.time.Instant.now()));
        when(transferQueryPort.findSummaries(ACTOR, null, null, null, 0, 3))
                .thenReturn(summaries);

        var result =
                service.listTransfers(
                        new ListTransfersCommand(UUID.randomUUID(), ACTOR, null, null, null, 0, 2));
        assertEquals(2, result.transfers().size());
        assertEquals(1, result.nextPage());

        assertThrows(
                TransferException.class,
                () ->
                        service.listTransfers(
                                new ListTransfersCommand(
                                        UUID.randomUUID(), ACTOR, null, null, "BOGUS", 0, 2)));
        assertThrows(
                TransferException.class,
                () ->
                        service.listTransfers(
                                new ListTransfersCommand(
                                        UUID.randomUUID(), null, null, null, null, 0, 2)));
    }

    @Test
    void recoverCompletesOnlyWhenJournalExists() {
        var transferId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var command = new RecoverTransferCommand(UUID.randomUUID(), transferId, transactionId);
        when(transferQueryPort.findStatusById(transferId)).thenReturn(Optional.of("UNKNOWN"));

        // journal 미반영(404) — 실패로 단정하지 않고 UNKNOWN 유지.
        when(ledger.findJournal(transactionId)).thenReturn(Optional.empty());
        service.recoverTransfer(command);
        verify(atomicWrite, never()).completeOnceWithOutbox(any());

        // journal 확인됨 — 동일 transactionId로 완료 수렴.
        var receipt = new LedgerPostingReceiptDraft(transactionId, UUID.randomUUID(), UUID.randomUUID());
        when(ledger.findJournal(transactionId)).thenReturn(Optional.of(receipt));
        service.recoverTransfer(command);
        verify(atomicWrite)
                .completeOnceWithOutbox(
                        new CompleteTransferCommand(
                                receipt.eventId(), transferId, transactionId, receipt.journalId()));

        // 일시 장애 — UNKNOWN 유지, 완료 시도 없음.
        org.mockito.Mockito.clearInvocations(atomicWrite);
        when(ledger.findJournal(transactionId))
                .thenThrow(new LedgerOutcomeUnknownException("timeout", null));
        service.recoverTransfer(command);
        verify(atomicWrite, never()).completeOnceWithOutbox(any());
    }

    @Test
    void recoverSkipsConvergedTransfers() {
        var command =
                new RecoverTransferCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(transferQueryPort.findStatusById(command.transferId()))
                .thenReturn(Optional.of("COMPLETED"));

        service.recoverTransfer(command);
        verify(ledger, never()).findJournal(any());
        verify(atomicWrite, never()).completeOnceWithOutbox(any());
    }
}
