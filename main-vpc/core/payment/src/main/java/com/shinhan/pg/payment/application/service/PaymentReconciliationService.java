package com.shinhan.pg.payment.application.service;

import com.shinhan.pg.payment.application.command.GetReconciliationCommand;
import com.shinhan.pg.payment.application.command.StartReconciliationCommand;
import com.shinhan.pg.payment.application.port.in.GetReconciliationUseCase;
import com.shinhan.pg.payment.application.port.in.StartReconciliationUseCase;
import com.shinhan.pg.payment.application.port.out.FepPaymentPort;
import com.shinhan.pg.payment.application.port.out.LedgerPostingPort;
import com.shinhan.pg.payment.application.port.out.PaymentStore;
import com.shinhan.pg.payment.application.port.out.ReconciliationStore;
import com.shinhan.pg.payment.application.readmodel.PgResult;
import com.shinhan.pg.payment.application.readmodel.ReconciliationDifferenceView;
import com.shinhan.pg.payment.application.result.GetReconciliationResult;
import com.shinhan.pg.payment.application.result.StartReconciliationResult;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/** F08/F09: read-only comparison of Payment, PG and Ledger evidence; never changes money. */
public final class PaymentReconciliationService
        implements StartReconciliationUseCase, GetReconciliationUseCase {
    private final PaymentStore payments;
    private final ReconciliationStore runs;
    private final FepPaymentPort fep;
    private final LedgerPostingPort ledger;
    private final java.util.function.IntConsumer mismatch;

    public PaymentReconciliationService(
            PaymentStore payments,
            ReconciliationStore runs,
            FepPaymentPort fep,
            LedgerPostingPort ledger) {
        this(payments, runs, fep, ledger, count -> {});
    }

    public PaymentReconciliationService(
            PaymentStore payments,
            ReconciliationStore runs,
            FepPaymentPort fep,
            LedgerPostingPort ledger,
            java.util.function.IntConsumer mismatch) {
        this.payments = payments;
        this.runs = runs;
        this.fep = fep;
        this.ledger = ledger;
        this.mismatch = mismatch;
    }

    public StartReconciliationResult startReconciliation(StartReconciliationCommand c) {
        authorized(c.callerServiceId());
        if (c.businessDate() == null) throw new PaymentException(PaymentErrorCode.INVALID_REQUEST);
        var id = runs.begin(c.businessDate());
        try {
            var differences = new ArrayList<ReconciliationDifferenceView>();
            var external = new HashMap<UUID, PgResult>();
            for (var pg : fep.results(c.businessDate())) {
                if (external.putIfAbsent(pg.operationId(), pg) != null)
                    differences.add(
                            new ReconciliationDifferenceView(
                                    "PG_DUPLICATE",
                                    pg.paymentId(),
                                    pg.externalReference(),
                                    null,
                                    pg.amount()));
            }
            for (var op : payments.operationsOn(c.businessDate())) {
                var pg = external.remove(op.id());
                if (pg == null)
                    differences.add(
                            new ReconciliationDifferenceView(
                                    "PG_MISSING",
                                    op.paymentId(),
                                    op.id().toString(),
                                    op.amount().amount(),
                                    null));
                else if (!pg.paymentId().equals(op.paymentId())
                        || !pg.kind().equals(op.kind())
                        || !pg.currency().equals(op.amount().currency().getCurrencyCode())
                        || pg.amount().compareTo(op.amount().amount()) != 0)
                    differences.add(
                            new ReconciliationDifferenceView(
                                    "PG_VALUE",
                                    op.paymentId(),
                                    pg.externalReference(),
                                    op.amount().amount(),
                                    pg.amount()));
                else if (!pg.outcome().equals(op.pgOutcome())
                        || "APPROVED".equals(pg.outcome()) && !op.completed())
                    differences.add(
                            new ReconciliationDifferenceView(
                                    "STATE",
                                    op.paymentId(),
                                    pg.externalReference(),
                                    op.amount().amount(),
                                    pg.amount()));
                var audit = ledger.audit(op.id());
                var p = payments.payment(op.paymentId());
                var from =
                        "APPROVAL".equals(op.kind())
                                ? p.sourceAccountId()
                                : p.destinationAccountId();
                var to =
                        "APPROVAL".equals(op.kind())
                                ? p.destinationAccountId()
                                : p.sourceAccountId();
                if (op.completed()) {
                    BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
                    boolean valid = audit.postings().size() == 2;
                    for (var line : audit.postings()) {
                        if (!line.currency().equals(op.amount().currency().getCurrencyCode()))
                            valid = false;
                        if (line.accountId().equals(from)) debit = debit.add(line.signedAmount());
                        else if (line.accountId().equals(to))
                            credit = credit.add(line.signedAmount());
                        else valid = false;
                    }
                    if (!valid
                            || debit.compareTo(op.amount().amount().negate()) != 0
                            || credit.compareTo(op.amount().amount()) != 0)
                        differences.add(
                                new ReconciliationDifferenceView(
                                        "LEDGER_VALUE",
                                        op.paymentId(),
                                        op.id().toString(),
                                        op.amount().amount(),
                                        credit));
                } else if (!audit.postings().isEmpty())
                    differences.add(
                            new ReconciliationDifferenceView(
                                    "LEDGER_UNCONFIRMED",
                                    op.paymentId(),
                                    op.id().toString(),
                                    null,
                                    op.amount().amount()));
            }
            for (var pg : external.values())
                differences.add(
                        new ReconciliationDifferenceView(
                                "PAYMENT_MISSING",
                                pg.paymentId(),
                                pg.externalReference(),
                                null,
                                pg.amount()));
            runs.finish(id, differences);
            if (!differences.isEmpty()) mismatch.accept(differences.size());
            return new StartReconciliationResult(id, "COMPLETED");
        } catch (RuntimeException error) {
            runs.failed(id);
            throw error;
        }
    }

    public GetReconciliationResult getReconciliation(GetReconciliationCommand c) {
        authorized(c.callerServiceId());
        return runs.get(c.runId());
    }

    private void authorized(String caller) {
        if (caller == null || caller.isBlank())
            throw new PaymentException(PaymentErrorCode.UNAUTHORIZED);
    }
}
