package com.shinhan.pg.transfer.application.service;

import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.swiftmessages.Remittance;
import com.shinhan.pg.common.swiftmessages.SwiftIds;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.transfer.application.command.GetOverseasTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestOverseasTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.AccountQueryPort;
import com.shinhan.pg.transfer.application.port.out.OtpVerificationPort;
import com.shinhan.pg.transfer.application.port.out.OverseasLedgerReservationPort;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferStore;
import com.shinhan.pg.transfer.application.readmodel.OverseasSnapshot;
import com.shinhan.pg.transfer.application.result.GetOverseasTransferResult;
import com.shinhan.pg.transfer.application.result.RequestOverseasTransferResult;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Auth → source-owned Account checks → OTP consumption → durable intent → Ledger outside DB TX.
 * USD, no FX, zero fee and holding accounts are confirmed development rules, not production
 * activation.
 */
public final class OverseasTransferService {
    private final boolean enabled;
    private final OverseasTransferStore store;
    private final OverseasLedgerReservationPort ledger;
    private final AccountQueryPort accounts;
    private final OtpVerificationPort otp;

    public OverseasTransferService(
            boolean enabled,
            OverseasTransferStore store,
            OverseasLedgerReservationPort ledger,
            AccountQueryPort accounts,
            OtpVerificationPort otp) {
        this.enabled = enabled;
        this.store = store;
        this.ledger = ledger;
        this.accounts = accounts;
        this.otp = otp;
    }

    public RequestOverseasTransferResult request(RequestOverseasTransferCommand c) {
        if (!enabled) throw new FeatureNotImplementedException("MVP5-F01");
        if (c.actorUserId() == null) throw new SwiftProblem("UNAUTHORIZED", ErrorType.UNAUTHORIZED);
        SwiftProblem.require(
                c.fromAccountId() != null
                        && c.idempotencyKey() != null
                        && c.idempotencyKey().matches("[A-Za-z0-9_-]{1,128}"));
        Remittance.text(c.recipientName(), 35);
        Remittance.text(c.recipientAccount(), 34);
        SwiftProblem.require(
                c.bic() != null
                        && c.bic().matches("[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?")
                        && "USD".equals(c.currency())
                        && c.amount() != null
                        && c.amount().signum() > 0);
        try {
            SwiftProblem.require(
                    c.amount().setScale(2, RoundingMode.UNNECESSARY).precision() <= 15);
        } catch (ArithmeticException invalid) {
            throw SwiftProblem.invalid();
        }
        String hash =
                SwiftIds.hash(
                        new String(
                                EventCodec.toBytes(
                                        List.of(
                                                c.fromAccountId(),
                                                c.recipientName(),
                                                c.recipientAccount(),
                                                c.bic(),
                                                c.amount().stripTrailingZeros().toPlainString(),
                                                c.currency())),
                                StandardCharsets.UTF_8));
        var existing = store.find(c.actorUserId(), c.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(hash)) throw SwiftProblem.conflict();
            return receipt(existing.get());
        }
        var account = accounts.getAccount(c.fromAccountId());
        if (!c.actorUserId().equals(account.ownerId()))
            throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
        SwiftProblem.require(
                "ACTIVE".equals(account.status()) && c.currency().equals(account.currency()));
        // The existing Auth contract binds an opaque destination UUID. Bind it to the whole
        // overseas intent.
        otp.verify(
                new RequestTransferCommand(
                        c.requestId(),
                        c.actorUserId(),
                        c.fromAccountId(),
                        UUID.nameUUIDFromBytes(hash.getBytes(StandardCharsets.UTF_8)),
                        c.amount(),
                        c.currency(),
                        c.otpToken(),
                        c.idempotencyKey()));
        var transfer = store.create(c, hash);
        recover(transfer.transferId());
        return receipt(store.find(transfer.transferId()).orElseThrow());
    }

    public GetOverseasTransferResult get(GetOverseasTransferCommand c) {
        if (!enabled) throw new FeatureNotImplementedException("MVP5-F02");
        var row = owned(c.actorUserId(), c.transferId());
        return new GetOverseasTransferResult(
                row.transferId(),
                row.uetr(),
                row.status(),
                "REJECTED".equals(row.status()) ? "RJCT" : null);
    }

    public OverseasSnapshot owned(UUID actor, UUID id) {
        if (actor == null) throw new SwiftProblem("UNAUTHORIZED", ErrorType.UNAUTHORIZED);
        var row =
                store.find(id)
                        .orElseThrow(() -> new SwiftProblem("NOT_FOUND", ErrorType.NOT_FOUND));
        if (!actor.equals(row.actorUserId()))
            throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
        return row;
    }

    public void recover(UUID id) {
        if (!enabled) return;
        UUID worker = UUID.randomUUID();
        if (!store.claim(id, worker)) return;
        var row = store.find(id).orElseThrow();
        try {
            ledger.reserve(row);
            store.ready(id, worker);
        } catch (TransferException rejection) {
            store.uncertain(id, worker, rejection.code() == TransferErrorCode.INSUFFICIENT_FUNDS);
        } catch (RuntimeException unavailable) {
            store.uncertain(id, worker, false);
        }
    }

    public void recoverPending() {
        if (enabled) {
            store.purgePrivateData();
            for (UUID id : store.recoverable()) recover(id);
        }
    }

    private static RequestOverseasTransferResult receipt(OverseasSnapshot r) {
        return new RequestOverseasTransferResult(r.transferId(), r.uetr(), r.status());
    }
}
