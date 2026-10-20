package com.shinhan.pg.mockpg.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.mockpg.api.PgOperationRequest;
import com.shinhan.pg.mockpg.api.PgOperationResult;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

/** A durable synthetic provider ledger. Never accesses Payment or Ledger databases. */
public final class PgOperationStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public PgOperationStore(DataSource source) {
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
    }

    public void attempt(UUID id) {
        jdbc.update(
                "insert into mock_pg_attempts(operation_id,attempts) values (?,1) on"
                    + " conflict(operation_id) do update set attempts=mock_pg_attempts.attempts+1",
                id.toString());
    }

    public void scenario(String mode) {
        if (!List.of(
                        "SUCCESS",
                        "DECLINE",
                        "PENDING",
                        "TIMEOUT_AFTER_COMMIT",
                        "TIMEOUT_BEFORE_COMMIT",
                        "ERROR_BEFORE_COMMIT")
                .contains(mode)) throw new IllegalArgumentException();
        jdbc.update("update mock_pg_scenario set next_mode=? where id=1", mode);
    }

    public String takeScenario() {
        return tx.execute(
                s -> {
                    var mode =
                            jdbc.queryForObject(
                                    "select next_mode from mock_pg_scenario where id=1 for update",
                                    String.class);
                    jdbc.update("update mock_pg_scenario set next_mode='SUCCESS' where id=1");
                    return mode;
                });
    }

    public PgOperationResult find(UUID id) {
        var rows =
                jdbc.queryForList(
                        "select result_json from mock_pg_operations where operation_id=?",
                        String.class,
                        id.toString());
        if (rows.isEmpty()) return null;
        try {
            return json.readValue(rows.getFirst(), PgOperationResult.class);
        } catch (Exception invalid) {
            throw new IllegalStateException("Invalid provider data");
        }
    }

    public PgOperationResult submit(PgOperationRequest request, String outcome) {
        return tx.execute(
                s -> {
                    jdbc.update(
                            "insert into mock_pg_payments(payment_id,currency) values (?,?) on"
                                + " conflict do nothing",
                            request.paymentId().toString(),
                            request.currency());
                    var account =
                            jdbc.queryForMap(
                                    "select * from mock_pg_payments where payment_id=? for update",
                                    request.paymentId().toString());
                    var existing = find(request.operationId());
                    if (existing != null) {
                        same(existing, request);
                        return existing;
                    }
                    return persist(request, outcome, 1, account);
                });
    }

    public PgOperationResult confirm(UUID id, String outcome) {
        return tx.execute(
                s -> {
                    var before = find(id);
                    if (before == null) throw new java.util.NoSuchElementException();
                    var account =
                            jdbc.queryForMap(
                                    "select * from mock_pg_payments where payment_id=? for update",
                                    before.paymentId().toString());
                    var existing = find(id);
                    if (!"PENDING".equals(existing.outcome())) {
                        if (!outcome.equals(existing.outcome()))
                            throw new IllegalArgumentException();
                        return existing;
                    }
                    return persist(
                            new PgOperationRequest(
                                    id,
                                    existing.paymentId(),
                                    existing.kind(),
                                    existing.amount(),
                                    existing.currency()),
                            outcome,
                            existing.version() + 1,
                            account);
                });
    }

    private PgOperationResult persist(
            PgOperationRequest r,
            String outcome,
            long version,
            java.util.Map<String, Object> account) {
        var approved = (BigDecimal) account.get("approved_amount");
        var refunded = (BigDecimal) account.get("refunded_amount");
        if (!r.currency().equals(account.get("currency"))) throw new IllegalArgumentException();
        if ("APPROVED".equals(outcome)) {
            if ("APPROVAL".equals(r.kind())) {
                if (approved.signum() != 0) outcome = "DECLINED";
                else
                    jdbc.update(
                            "update mock_pg_payments set approved_amount=? where payment_id=?",
                            r.amount(),
                            r.paymentId().toString());
            } else {
                boolean valid =
                        approved.signum() > 0 && refunded.add(r.amount()).compareTo(approved) <= 0;
                if ("CANCEL".equals(r.kind()))
                    valid = valid && refunded.signum() == 0 && r.amount().compareTo(approved) == 0;
                if (!valid) outcome = "DECLINED";
                else
                    jdbc.update(
                            "update mock_pg_payments set refunded_amount=refunded_amount+? where"
                                + " payment_id=?",
                            r.amount(),
                            r.paymentId().toString());
            }
        }
        var result =
                new PgOperationResult(
                        r.operationId(),
                        r.paymentId(),
                        r.kind(),
                        outcome,
                        r.amount(),
                        r.currency(),
                        version,
                        UUID.randomUUID(),
                        "MOCK-" + r.operationId(),
                        Instant.now());
        try {
            jdbc.update(
                    "insert into mock_pg_operations(operation_id,payment_id,result_json) values"
                        + " (?,?,?) on conflict(operation_id) do update set"
                        + " result_json=excluded.result_json",
                    r.operationId().toString(),
                    r.paymentId().toString(),
                    json.writeValueAsString(result));
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw new IllegalStateException("Cannot encode provider result");
        }
        return result;
    }

    public List<PgOperationResult> results(LocalDate date) {
        return jdbc.query(
                "select result_json from mock_pg_operations where business_date=? order by"
                    + " operation_id",
                (r, i) -> {
                    try {
                        return json.readValue(r.getString(1), PgOperationResult.class);
                    } catch (Exception invalid) {
                        throw new IllegalStateException("Invalid provider data");
                    }
                },
                date);
    }

    private void same(PgOperationResult old, PgOperationRequest r) {
        if (!old.paymentId().equals(r.paymentId())
                || !old.kind().equals(r.kind())
                || old.amount().compareTo(r.amount()) != 0
                || !old.currency().equals(r.currency())) throw new IllegalArgumentException();
    }
}
