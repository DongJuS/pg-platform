package com.shinhan.pg.payment.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.payment.application.port.out.ReconciliationStore;
import com.shinhan.pg.payment.application.readmodel.ReconciliationDifferenceView;
import com.shinhan.pg.payment.application.result.GetReconciliationResult;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

public final class ReconciliationRunPersistenceAdapter implements ReconciliationStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final OutboxStore outbox;
    private final ObjectMapper json = new ObjectMapper();

    public ReconciliationRunPersistenceAdapter(DataSource source, OutboxStore outbox) {
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        this.outbox = outbox;
    }

    public UUID begin(LocalDate date) {
        var id = UUID.randomUUID();
        try {
            jdbc.update(
                    "insert into reconciliation_runs(id,business_date,status) values"
                        + " (?,?,'RUNNING')",
                    id.toString(),
                    date);
        } catch (org.springframework.dao.DuplicateKeyException conflict) {
            throw new PaymentException(PaymentErrorCode.INVALID_STATE);
        }
        return id;
    }

    public void finish(UUID id, List<ReconciliationDifferenceView> differences) {
        tx.executeWithoutResult(
                s -> {
                    jdbc.queryForObject(
                            "select id from reconciliation_runs where id=? for update",
                            String.class,
                            id.toString());
                    for (var d : differences)
                        jdbc.update(
                                "insert into"
                                    + " reconciliation_differences(id,run_id,difference_type,payment_id,external_reference,internal_amount,external_amount)"
                                    + " values (?,?,?,?,?,?,?)",
                                UUID.randomUUID().toString(),
                                id.toString(),
                                d.differenceType(),
                                d.paymentId() == null ? null : d.paymentId().toString(),
                                d.externalReference(),
                                d.internalAmount(),
                                d.externalAmount());
                    jdbc.update(
                            "update reconciliation_runs set status='COMPLETED' where id=?",
                            id.toString());
                    if (!differences.isEmpty()) {
                        var event = UUID.randomUUID();
                        try {
                            outbox.append(
                                    new OutboxRecord(
                                            UUID.randomUUID(),
                                            event,
                                            "recon.mismatch",
                                            id.toString(),
                                            json.writeValueAsBytes(
                                                    Map.of(
                                                            "eventId",
                                                            event.toString(),
                                                            "runId",
                                                            id.toString(),
                                                            "count",
                                                            differences.size()))));
                        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
                            throw new IllegalStateException("Cannot encode reconciliation result");
                        }
                    }
                });
    }

    public void failed(UUID id) {
        jdbc.update(
                "update reconciliation_runs set status='FAILED' where id=? and status='RUNNING'",
                id.toString());
    }

    public GetReconciliationResult get(UUID id) {
        var status =
                jdbc.queryForList(
                        "select status from reconciliation_runs where id=?",
                        String.class,
                        id.toString());
        if (status.isEmpty()) throw new PaymentException(PaymentErrorCode.NOT_FOUND);
        var differences =
                jdbc.query(
                        "select * from reconciliation_differences where run_id=? order by id",
                        (r, i) ->
                                new ReconciliationDifferenceView(
                                        r.getString("difference_type"),
                                        r.getString("payment_id") == null
                                                ? null
                                                : UUID.fromString(r.getString("payment_id")),
                                        r.getString("external_reference"),
                                        r.getBigDecimal("internal_amount"),
                                        r.getBigDecimal("external_amount")),
                        id.toString());
        return new GetReconciliationResult(id, status.getFirst(), differences);
    }
}
