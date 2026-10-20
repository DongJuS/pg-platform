package com.shinhan.pg.payment.application.service;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.payment.application.command.ApplyPaymentConfirmationCommand;
import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.command.CancelPaymentCommand;
import com.shinhan.pg.payment.application.command.GetPaymentCommand;
import com.shinhan.pg.payment.application.command.ListRefundsCommand;
import com.shinhan.pg.payment.application.command.ReceivePgWebhookCommand;
import com.shinhan.pg.payment.application.command.RecoverPaymentCommand;
import com.shinhan.pg.payment.application.command.RequestRefundCommand;
import com.shinhan.pg.payment.application.port.in.ApplyPaymentConfirmationUseCase;
import com.shinhan.pg.payment.application.port.in.ApprovePaymentUseCase;
import com.shinhan.pg.payment.application.port.in.CancelPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.GetPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.ListRefundsUseCase;
import com.shinhan.pg.payment.application.port.in.ReceivePgWebhookUseCase;
import com.shinhan.pg.payment.application.port.in.RecoverPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.RequestRefundUseCase;
import com.shinhan.pg.payment.application.port.out.AccountAuthorityPort;
import com.shinhan.pg.payment.application.port.out.FepPaymentPort;
import com.shinhan.pg.payment.application.port.out.LedgerPostingPort;
import com.shinhan.pg.payment.application.port.out.PaymentStore;
import com.shinhan.pg.payment.application.port.out.PgWebhookDecoderPort;
import com.shinhan.pg.payment.application.readmodel.PgResult;
import com.shinhan.pg.payment.application.readmodel.RefundSummary;
import com.shinhan.pg.payment.application.result.ApprovePaymentResult;
import com.shinhan.pg.payment.application.result.CancelPaymentResult;
import com.shinhan.pg.payment.application.result.GetPaymentResult;
import com.shinhan.pg.payment.application.result.ListRefundsResult;
import com.shinhan.pg.payment.application.result.RequestRefundResult;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;
import com.shinhan.pg.payment.domain.model.PaymentOperation;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/**
 * F01-F06/F14-F16. Wait for PG final result AND ledger confirmation; never hold a DB transaction
 * across HTTP.
 */
public final class PaymentApplicationService
        implements ApprovePaymentUseCase,
                ApplyPaymentConfirmationUseCase,
                CancelPaymentUseCase,
                GetPaymentUseCase,
                ListRefundsUseCase,
                ReceivePgWebhookUseCase,
                RecoverPaymentUseCase,
                RequestRefundUseCase {
    private final PaymentStore store;
    private final FepPaymentPort fep;
    private final LedgerPostingPort ledger;
    private final AccountAuthorityPort accounts;
    private final PgWebhookDecoderPort webhook;
    private final boolean recoveryQueriesEnabled;

    public PaymentApplicationService(
            PaymentStore store,
            FepPaymentPort fep,
            LedgerPostingPort ledger,
            AccountAuthorityPort accounts,
            PgWebhookDecoderPort webhook,
            boolean recoveryQueriesEnabled) {
        this.store = store;
        this.fep = fep;
        this.ledger = ledger;
        this.accounts = accounts;
        this.webhook = webhook;
        this.recoveryQueriesEnabled = recoveryQueriesEnabled;
    }

    @Override
    public ApprovePaymentResult approvePayment(ApprovePaymentCommand c) {
        actor(c.actorUserId());
        required(
                c.merchantId() != null
                        && c.sourceAccountId() != null
                        && c.destinationAccountId() != null
                        && !c.sourceAccountId().equals(c.destinationAccountId()));
        text(c.orderReference());
        text(c.idempotencyKey());
        positive(c.amount(), c.currency());
        accounts.verify(
                c.actorUserId(), c.sourceAccountId(), c.destinationAccountId(), c.currency());
        var op = execute(store.reserveApproval(c));
        return new ApprovePaymentResult(op.paymentId(), store.payment(op.paymentId()).status());
    }

    @Override
    public CancelPaymentResult cancelPayment(CancelPaymentCommand c) {
        actor(c.actorUserId());
        required(c.paymentId() != null);
        var op =
                execute(
                        store.reserveAdjustment(
                                c.actorUserId(), c.paymentId(), "CANCEL", null, "full-cancel"));
        return new CancelPaymentResult(op.paymentId(), store.payment(op.paymentId()).status());
    }

    @Override
    public RequestRefundResult requestRefund(RequestRefundCommand c) {
        actor(c.actorUserId());
        required(c.paymentId() != null);
        text(c.idempotencyKey());
        var p = owned(c.actorUserId(), c.paymentId());
        positive(c.amount(), p.amount().currency().getCurrencyCode());
        var op =
                execute(
                        store.reserveAdjustment(
                                c.actorUserId(),
                                c.paymentId(),
                                "REFUND",
                                c.amount(),
                                c.idempotencyKey()));
        return new RequestRefundResult(op.id(), op.paymentId(), op.status());
    }

    private PaymentOperation execute(PaymentOperation op) {
        if (op.completed()) return op;
        if (store.claimPg(op.id())) {
            try {
                apply(fep.submit(op));
            } catch (PaymentException error) {
                if (store.operation(op.id()).pgOutcome() == null) store.unknown(op.id(), "UNKNOWN");
                throw new PaymentException(error.code(), op.paymentId(), op.id());
            }
        }
        return requireComplete(store.operation(op.id()));
    }

    /**
     * Shared synchronous response/webhook path. Operation ID prevents repeated financial effects.
     */
    public void apply(PgResult result) {
        var op = store.acceptResult(result);
        if ("DECLINED".equals(op.status()))
            throw new PaymentException(PaymentErrorCode.PG_DECLINED, op.paymentId(), op.id());
        if (op.completed()) return;
        if (store.claimLedger(op.id())) {
            try {
                store.complete(op.id(), ledger.post(store.payment(op.paymentId()), op));
                org.slf4j.LoggerFactory.getLogger(getClass()).info("Payment operation confirmed");
            } catch (PaymentException failure) {
                store.unknown(
                        op.id(),
                        failure.code() == PaymentErrorCode.LEDGER_PENDING
                                ? "LEDGER_UNKNOWN"
                                : "LEDGER_REJECTED");
                throw new PaymentException(
                        PaymentErrorCode.LEDGER_PENDING, op.paymentId(), op.id());
            }
        }
        requireComplete(store.operation(op.id()));
    }

    @Override
    public GetPaymentResult getPayment(GetPaymentCommand c) {
        var p = owned(c.actorUserId(), c.paymentId());
        return new GetPaymentResult(
                p.id(),
                p.orderReference(),
                p.amount().amount(),
                store.operation(p.id()).completed() ? p.amount().amount() : BigDecimal.ZERO,
                p.refunded().amount(),
                p.reserved().amount(),
                p.amount().currency().getCurrencyCode(),
                p.status());
    }

    @Override
    public ListRefundsResult listRefunds(ListRefundsCommand c) {
        owned(c.actorUserId(), c.paymentId());
        return new ListRefundsResult(
                store.operations(c.paymentId()).stream()
                        .filter(o -> "REFUND".equals(o.kind()))
                        .map(o -> new RefundSummary(o.id(), o.amount().amount(), o.status()))
                        .toList());
    }

    @Override
    public void receivePgWebhook(ReceivePgWebhookCommand c) {
        try {
            apply(webhook.verifyAndDecode(c.rawPayload(), c.signature()));
        } catch (PaymentException e) {
            if (e.code() != PaymentErrorCode.PG_DECLINED) throw e;
        }
    }

    @Override
    public void applyPaymentConfirmation(ApplyPaymentConfirmationCommand c) {
        // Legacy Kafka command has no verified amount/version/provider contract; never invent a
        // final result.
        throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK, c.paymentId(), null);
    }

    @Override
    public void recoverPayment(RecoverPaymentCommand c) {
        if (!recoveryQueriesEnabled) throw new PaymentException(PaymentErrorCode.INVALID_STATE);
        var op = store.operation(c.resourceId());
        if (op.completed() || "DECLINED".equals(op.status())) return;
        if (op.ledgerSent()) {
            var journal = ledger.find(op.id());
            if (journal.isPresent()) {
                store.complete(op.id(), journal.get());
                return;
            }
            // An empty lookup never authorizes another posting or a recovered-success response.
            throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN, op.paymentId(), op.id());
        }
        var result = fep.query(op.id());
        if (result.isPresent()) apply(result.get());
        else
            throw new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN, op.paymentId(), op.id());
    }

    private com.shinhan.pg.payment.domain.model.PaymentDetails owned(UUID actor, UUID id) {
        actor(actor);
        required(id != null);
        var p = store.payment(id);
        if (!p.actorUserId().equals(actor)) throw new PaymentException(PaymentErrorCode.FORBIDDEN);
        return p;
    }

    private static PaymentOperation requireComplete(PaymentOperation op) {
        if (op.completed()) return op;
        throw new PaymentException(
                "DECLINED".equals(op.status())
                        ? PaymentErrorCode.PG_DECLINED
                        : PaymentErrorCode.PG_RESULT_UNKNOWN,
                op.paymentId(),
                op.id());
    }

    private static void actor(UUID actor) {
        if (actor == null) throw new PaymentException(PaymentErrorCode.UNAUTHORIZED);
    }

    private static void required(boolean valid) {
        if (!valid) throw new PaymentException(PaymentErrorCode.INVALID_REQUEST);
    }

    private static void text(String s) {
        required(s != null && !s.isBlank() && s.length() <= 100);
    }

    private static void positive(BigDecimal amount, String currency) {
        try {
            required(
                    amount != null
                            && amount.signum() > 0
                            && amount.precision() <= 24
                            && amount.precision() - amount.scale() <= 15);
            Money.of(amount, Currency.getInstance(currency));
        } catch (IllegalArgumentException | NullPointerException | ArithmeticException error) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST);
        }
    }
}
