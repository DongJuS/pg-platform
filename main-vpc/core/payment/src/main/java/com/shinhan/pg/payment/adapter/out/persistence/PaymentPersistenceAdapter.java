package com.shinhan.pg.payment.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.port.out.PaymentStore;
import com.shinhan.pg.payment.application.readmodel.PgResult;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;
import com.shinhan.pg.payment.domain.model.PaymentDetails;
import com.shinhan.pg.payment.domain.model.PaymentOperation;
import com.shinhan.pg.payment.domain.policy.RefundLimitPolicy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

/**
 * F01-F05/F15/F16: PostgreSQL row locks serialize reservations and completion. Financial network
 * effects happen outside every local transaction here. Completion + Outbox append share one
 * DataSource/transaction. Source: ADR 0002, Figma 9:202/206/210.
 */
public final class PaymentPersistenceAdapter implements PaymentStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final OutboxStore outbox;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public PaymentPersistenceAdapter(DataSource source, OutboxStore outbox) {
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        this.outbox = outbox;
    }

    @Override
    public PaymentOperation reserveApproval(ApprovePaymentCommand c) {
        return tx.execute(
                s -> {
                    var id = UUID.randomUUID();
                    int inserted =
                            jdbc.update(
                                    "insert into"
                                        + " payments(id,actor_user_id,merchant_id,order_reference,source_account_id,destination_account_id,amount,currency,idempotency_key)"
                                        + " values (?,?,?,?,?,?,?,?,?) on conflict do nothing",
                                    id.toString(),
                                    c.actorUserId().toString(),
                                    c.merchantId().toString(),
                                    c.orderReference(),
                                    c.sourceAccountId().toString(),
                                    c.destinationAccountId().toString(),
                                    c.amount(),
                                    c.currency(),
                                    c.idempotencyKey());
                    if (inserted == 0) {
                        var ids =
                                jdbc.queryForList(
                                        "select id from payments where actor_user_id=? and"
                                            + " idempotency_key=?",
                                        String.class,
                                        c.actorUserId().toString(),
                                        c.idempotencyKey());
                        if (ids.isEmpty()) throw fail(PaymentErrorCode.IDEMPOTENCY_CONFLICT);
                        var existing = payment(UUID.fromString(ids.getFirst()));
                        if (!existing.merchantId().equals(c.merchantId())
                                || !existing.orderReference().equals(c.orderReference())
                                || !existing.sourceAccountId().equals(c.sourceAccountId())
                                || !existing.destinationAccountId().equals(c.destinationAccountId())
                                || !existing.amount().equals(money(c.amount(), c.currency())))
                            throw fail(PaymentErrorCode.IDEMPOTENCY_CONFLICT);
                        return operation(existing.id());
                    }
                    insertOperation(
                            id, id, "APPROVAL", c.amount(), c.currency(), c.idempotencyKey());
                    return operation(id);
                });
    }

    @Override
    public PaymentOperation reserveAdjustment(
            UUID actor, UUID payment, String kind, BigDecimal amount, String key) {
        return tx.execute(
                s -> {
                    lockPayment(payment);
                    var p = payment(payment);
                    if (!p.actorUserId().equals(actor)) throw fail(PaymentErrorCode.FORBIDDEN);
                    var existing =
                            jdbc.query(
                                    "select * from payment_operations where payment_id=? and kind=?"
                                        + " and idempotency_key=?",
                                    this::mapOperation,
                                    payment.toString(),
                                    kind,
                                    key);
                    if (!existing.isEmpty()) {
                        var op = existing.getFirst();
                        if (amount != null && op.amount().amount().compareTo(amount) != 0)
                            throw fail(PaymentErrorCode.IDEMPOTENCY_CONFLICT);
                        return op;
                    }
                    if (!List.of("APPROVED", "PARTIALLY_REFUNDED").contains(p.status()))
                        throw fail(PaymentErrorCode.INVALID_STATE);
                    if ("CANCEL".equals(kind)) {
                        if (p.refunded().amount().signum() != 0
                                || p.reserved().amount().signum() != 0)
                            throw fail(PaymentErrorCode.INVALID_STATE);

                    } else {
                        var activeCancel =
                                jdbc.queryForObject(
                                        "select count(*) from payment_operations where payment_id=?"
                                            + " and kind='CANCEL' and status<>'DECLINED'",
                                        Long.class,
                                        payment.toString());
                        if (activeCancel != 0) throw fail(PaymentErrorCode.INVALID_STATE);
                    }
                    var requested = "CANCEL".equals(kind) ? p.amount().amount() : amount;
                    new RefundLimitPolicy()
                            .verifyRefundAmount(
                                    p.limitView(),
                                    money(requested, p.amount().currency().getCurrencyCode()));
                    var id = UUID.randomUUID();
                    insertOperation(
                            id,
                            payment,
                            kind,
                            requested,
                            p.amount().currency().getCurrencyCode(),
                            key);
                    jdbc.update(
                            "update payments set"
                                + " reserved_amount=reserved_amount+?,version=version+1 where id=?",
                            requested,
                            payment.toString());
                    return operation(id);
                });
    }

    private void insertOperation(
            UUID id, UUID payment, String kind, BigDecimal amount, String currency, String key) {
        jdbc.update(
                "insert into payment_operations(id,payment_id,kind,amount,currency,idempotency_key)"
                    + " values (?,?,?,?,?,?)",
                id.toString(),
                payment.toString(),
                kind,
                amount,
                currency,
                key);
    }

    @Override
    public PaymentDetails payment(UUID id) {
        var rows =
                jdbc.query(
                        "select * from payments where id=?",
                        (r, i) ->
                                new PaymentDetails(
                                        uuid(r, "id"),
                                        uuid(r, "actor_user_id"),
                                        uuid(r, "merchant_id"),
                                        r.getString("order_reference"),
                                        uuid(r, "source_account_id"),
                                        uuid(r, "destination_account_id"),
                                        money(r.getBigDecimal("amount"), r.getString("currency")),
                                        money(
                                                r.getBigDecimal("refunded_amount"),
                                                r.getString("currency")),
                                        money(
                                                r.getBigDecimal("reserved_amount"),
                                                r.getString("currency")),
                                        r.getString("status"),
                                        r.getLong("version")),
                        id.toString());
        if (rows.isEmpty()) throw fail(PaymentErrorCode.NOT_FOUND);
        return rows.getFirst();
    }

    @Override
    public PaymentOperation operation(UUID id) {
        var rows =
                jdbc.query(
                        "select * from payment_operations where id=?",
                        this::mapOperation,
                        id.toString());
        if (rows.isEmpty()) throw fail(PaymentErrorCode.NOT_FOUND);
        return rows.getFirst();
    }

    @Override
    public List<PaymentOperation> operations(UUID payment) {
        return jdbc.query(
                "select * from payment_operations where payment_id=? order by created_at,id",
                this::mapOperation,
                payment.toString());
    }

    @Override
    public List<PaymentOperation> operationsOn(LocalDate date) {
        return jdbc.query(
                "select * from payment_operations where business_date=? order by id",
                this::mapOperation,
                date);
    }

    @Override
    public boolean claimPg(UUID id) {
        return jdbc.update(
                        "update payment_operations set pg_sent=true,status='DISPATCHED' where id=?"
                            + " and pg_sent=false and status='READY'",
                        id.toString())
                == 1;
    }

    @Override
    public PaymentOperation acceptResult(PgResult r) {
        return tx.execute(
                s -> {
                    lockPayment(r.paymentId());
                    var op = operation(r.operationId());
                    if (!op.paymentId().equals(r.paymentId())
                            || !op.kind().equals(r.kind())
                            || !op.amount().equals(money(r.amount(), r.currency()))
                            || r.version() < 1
                            || !List.of("APPROVED", "DECLINED", "PENDING").contains(r.outcome())
                            || r.eventId() == null
                            || r.occurredAt() == null
                            || r.externalReference() == null)
                        throw fail(PaymentErrorCode.INVALID_WEBHOOK);
                    String fingerprint = fingerprint(r);
                    jdbc.update(
                            "insert into"
                                + " pg_events(event_id,operation_id,payment_id,version,payload_hash)"
                                + " values (?,?,?,?,?) on conflict do nothing",
                            r.eventId().toString(),
                            op.id().toString(),
                            op.paymentId().toString(),
                            r.version(),
                            fingerprint);
                    var recorded =
                            jdbc.queryForObject(
                                    "select payload_hash from pg_events where event_id=?",
                                    String.class,
                                    r.eventId().toString());
                    if (!fingerprint.equals(recorded))
                        throw fail(PaymentErrorCode.IDEMPOTENCY_CONFLICT);
                    if (r.version() < op.pgVersion()) return op;
                    if (op.pgOutcome() != null
                            && !"PENDING".equals(op.pgOutcome())
                            && !op.pgOutcome().equals(r.outcome()))
                        throw fail(PaymentErrorCode.INVALID_STATE);
                    if (r.version() == op.pgVersion()) {
                        if (!r.outcome().equals(op.pgOutcome()))
                            throw fail(PaymentErrorCode.IDEMPOTENCY_CONFLICT);
                        return op;
                    }
                    if (op.completed()) return op;
                    String status =
                            switch (r.outcome()) {
                                case "APPROVED" -> op.ledgerSent() ? op.status() : "PG_APPROVED";
                                case "DECLINED" -> "DECLINED";
                                default -> "UNKNOWN";
                            };
                    jdbc.update(
                            "update payment_operations set pg_outcome=?,pg_version=?,status=? where"
                                + " id=?",
                            r.outcome(),
                            r.version(),
                            status,
                            op.id().toString());
                    if ("DECLINED".equals(r.outcome()) && !"DECLINED".equals(op.status())) {
                        if ("APPROVAL".equals(op.kind()))
                            jdbc.update(
                                    "update payments set status='DECLINED',version=version+1 where"
                                        + " id=?",
                                    op.paymentId().toString());
                        else
                            jdbc.update(
                                    "update payments set"
                                        + " reserved_amount=reserved_amount-?,version=version+1"
                                        + " where id=?",
                                    op.amount().amount(),
                                    op.paymentId().toString());
                    }
                    return operation(op.id());
                });
    }

    @Override
    public boolean claimLedger(UUID id) {
        return jdbc.update(
                        "update payment_operations set ledger_sent=true,status='LEDGER_SENDING'"
                            + " where id=? and pg_outcome='APPROVED' and ledger_sent=false",
                        id.toString())
                == 1;
    }

    @Override
    public void unknown(UUID id, String stage) {
        tx.executeWithoutResult(
                s -> {
                    var op = operation(id);
                    lockPayment(op.paymentId());
                    jdbc.update(
                            "update payment_operations set status=? where id=? and status not in"
                                + " ('COMPLETED','DECLINED')",
                            stage,
                            id.toString());
                    if ("APPROVAL".equals(op.kind()))
                        jdbc.update(
                                "update payments set status='UNKNOWN' where id=? and"
                                    + " status='PROCESSING'",
                                op.paymentId().toString());
                });
    }

    @Override
    public void complete(UUID id, UUID journalId) {
        if (journalId == null) throw fail(PaymentErrorCode.LEDGER_PENDING);
        tx.executeWithoutResult(
                s -> {
                    var initial = operation(id);
                    lockPayment(initial.paymentId());
                    var op = operation(id);
                    if (op.completed()) {
                        if (!journalId.equals(op.journalId()))
                            throw fail(PaymentErrorCode.INVALID_STATE);
                        return;
                    }
                    if (!"APPROVED".equals(op.pgOutcome()) || !op.ledgerSent())
                        throw fail(PaymentErrorCode.INVALID_STATE);
                    var p = payment(op.paymentId());
                    String next = "APPROVED";
                    if (!"APPROVAL".equals(op.kind())) {
                        var refunded = p.refunded().plus(op.amount());
                        next =
                                "CANCEL".equals(op.kind())
                                        ? "CANCELLED"
                                        : refunded.equals(p.amount())
                                                ? "REFUNDED"
                                                : "PARTIALLY_REFUNDED";
                        jdbc.update(
                                "update payments set"
                                    + " refunded_amount=refunded_amount+?,reserved_amount=reserved_amount-?"
                                    + " where id=?",
                                op.amount().amount(),
                                op.amount().amount(),
                                p.id().toString());
                    }
                    new com.shinhan.pg.payment.domain.policy.PaymentStatePolicy()
                            .validateTransition(p.limitView(), next, op.id().toString());
                    jdbc.update(
                            "update payment_operations set status='COMPLETED',journal_id=? where"
                                + " id=?",
                            journalId.toString(),
                            id.toString());
                    jdbc.update(
                            "update payments set status=?,version=version+1 where id=?",
                            next,
                            p.id().toString());
                    {
                        var event = UUID.randomUUID();
                        try {
                            String destination =
                                    switch (op.kind()) {
                                        case "APPROVAL" -> "payment.approved";
                                        case "REFUND" -> "payment.refunded";
                                        default -> "payment.cancelled";
                                    };
                            outbox.append(
                                    new OutboxRecord(
                                            UUID.randomUUID(),
                                            event,
                                            destination,
                                            p.id().toString(),
                                            json.writeValueAsBytes(
                                                    Map.of(
                                                            "eventId",
                                                            event.toString(),
                                                            "paymentId",
                                                            p.id().toString(),
                                                            "operationId",
                                                            id.toString(),
                                                            "transactionId",
                                                            id.toString(),
                                                            "journalId",
                                                            journalId.toString(),
                                                            "status",
                                                            next))));
                        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
                            throw new IllegalStateException("Cannot encode payment completion");
                        }
                    }
                });
    }

    private void lockPayment(UUID id) {
        if (jdbc.queryForList(
                        "select id from payments where id=? for update",
                        String.class,
                        id.toString())
                .isEmpty()) throw fail(PaymentErrorCode.NOT_FOUND);
    }

    private PaymentOperation mapOperation(ResultSet r, int index) throws SQLException {
        String journal = r.getString("journal_id");
        return new PaymentOperation(
                uuid(r, "id"),
                uuid(r, "payment_id"),
                r.getString("kind"),
                money(r.getBigDecimal("amount"), r.getString("currency")),
                r.getString("status"),
                r.getString("pg_outcome"),
                r.getLong("pg_version"),
                r.getBoolean("pg_sent"),
                r.getBoolean("ledger_sent"),
                journal == null ? null : UUID.fromString(journal));
    }

    private static Money money(BigDecimal amount, String currency) {
        return Money.of(amount, Currency.getInstance(currency));
    }

    private static UUID uuid(ResultSet r, String name) throws SQLException {
        return UUID.fromString(r.getString(name));
    }

    private static PaymentException fail(PaymentErrorCode code) {
        return new PaymentException(code);
    }

    private String fingerprint(PgResult r) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            json.writeValueAsBytes(
                                                    List.of(
                                                            r.operationId().toString(),
                                                            r.paymentId().toString(),
                                                            r.kind(),
                                                            r.outcome(),
                                                            r.amount()
                                                                    .stripTrailingZeros()
                                                                    .toPlainString(),
                                                            r.currency(),
                                                            r.version(),
                                                            r.eventId().toString(),
                                                            r.externalReference(),
                                                            r.occurredAt().toString()))));
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot fingerprint PG result");
        }
    }
}
