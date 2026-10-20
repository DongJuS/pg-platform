package com.shinhan.pg.fepadapter.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.fepadapter.application.port.out.PgReceiptStore;
import com.shinhan.pg.fepadapter.domain.model.PgRequest;
import com.shinhan.pg.fepadapter.domain.model.PgResult;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import javax.sql.DataSource;

/** FEP local durable receipt and one dispatch permit; no HTTP under these locks. */
public final class PgReceiptPersistenceAdapter implements PgReceiptStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public PgReceiptPersistenceAdapter(DataSource source) {
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
    }

    @Override
    public UUID reserve(PgRequest r) {
        return tx.execute(
                s -> {
                    jdbc.update(
                            "insert into"
                                + " pg_receipts(operation_id,receipt_id,payment_id,kind,amount,currency)"
                                + " values (?,?,?,?,?,?) on conflict do nothing",
                            r.operationId().toString(),
                            UUID.randomUUID().toString(),
                            r.paymentId().toString(),
                            r.kind(),
                            r.amount(),
                            r.currency());
                    var row =
                            jdbc.queryForMap(
                                    "select * from pg_receipts where operation_id=? for update",
                                    r.operationId().toString());
                    if (!r.paymentId().toString().equals(row.get("payment_id"))
                            || !r.kind().equals(row.get("kind"))
                            || r.amount().compareTo((java.math.BigDecimal) row.get("amount")) != 0
                            || !r.currency().equals(row.get("currency")))
                        throw new IllegalArgumentException("Operation identity conflict");
                    return UUID.fromString((String) row.get("receipt_id"));
                });
    }

    @Override
    public boolean claim(UUID id) {
        return jdbc.update(
                        "update pg_receipts set dispatched=true where operation_id=? and"
                            + " dispatched=false",
                        id.toString())
                == 1;
    }

    @Override
    public UUID receipt(UUID id) {
        var ids =
                jdbc.queryForList(
                        "select receipt_id from pg_receipts where operation_id=?",
                        String.class,
                        id.toString());
        if (ids.isEmpty()) throw new java.util.NoSuchElementException();
        return UUID.fromString(ids.getFirst());
    }

    @Override
    public PgResult result(UUID id) {
        var rows =
                jdbc.queryForList(
                        "select result_json from pg_receipts where operation_id=?",
                        String.class,
                        id.toString());
        if (rows.isEmpty() || rows.getFirst() == null) return null;
        try {
            return json.readValue(rows.getFirst(), PgResult.class);
        } catch (Exception invalid) {
            throw new IllegalStateException("Invalid stored PG result");
        }
    }

    @Override
    public void result(PgResult r) {
        tx.executeWithoutResult(
                s -> {
                    var row =
                            jdbc.queryForMap(
                                    "select * from pg_receipts where operation_id=? for update",
                                    r.operationId().toString());
                    if (!r.paymentId().toString().equals(row.get("payment_id"))
                            || !r.kind().equals(row.get("kind"))
                            || r.amount().compareTo((java.math.BigDecimal) row.get("amount")) != 0
                            || !r.currency().equals(row.get("currency")))
                        throw new IllegalArgumentException("PG result identity mismatch");
                    var old = result(r.operationId());
                    if (old != null && r.version() < old.version()) return;
                    if (old != null
                            && !"PENDING".equals(old.outcome())
                            && !old.outcome().equals(r.outcome()))
                        throw new IllegalArgumentException("Conflicting PG final result");
                    try {
                        jdbc.update(
                                "update pg_receipts set result_json=? where operation_id=?",
                                json.writeValueAsString(r),
                                r.operationId().toString());
                    } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
                        throw new IllegalStateException("Cannot encode PG result");
                    }
                });
    }

    @Override
    public void callback(UUID event, byte[] raw, String signature) {
        tx.executeWithoutResult(
                s -> {
                    jdbc.update(
                            "insert into pg_callbacks(event_id,raw_payload,signature) values"
                                + " (?,?,?) on conflict do nothing",
                            event.toString(),
                            raw,
                            signature);
                    var old =
                            jdbc.queryForObject(
                                    "select raw_payload from pg_callbacks where event_id=?",
                                    byte[].class,
                                    event.toString());
                    if (!java.util.Arrays.equals(raw, old))
                        throw new IllegalArgumentException("Webhook event identity conflict");
                });
    }
}
