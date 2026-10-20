package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.common.eventcontracts.OverseasLedgerAppliedEvent;
import com.shinhan.pg.common.eventcontracts.TransferCompletedEvent;
import com.shinhan.pg.common.eventcontracts.TransferOutboundEvent;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.common.swiftmessages.PrivatePayload;
import com.shinhan.pg.common.swiftmessages.Remittance;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.transfer.application.command.RequestOverseasTransferCommand;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferStore;
import com.shinhan.pg.transfer.application.readmodel.OverseasSnapshot;
import com.shinhan.pg.transfer.domain.model.OverseasTransferDetails;
import com.shinhan.pg.transfer.domain.policy.OverseasTransferConvergencePolicy;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

/**
 * Transfer-owned aggregate. State and events share one local transaction; private data expires in
 * 48h.
 */
public final class OverseasTransferDetailsPersistenceAdapter implements OverseasTransferStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final OutboxStore outbox;
    private final PrivatePayload privacy;

    public OverseasTransferDetailsPersistenceAdapter(
            DataSource ds, OutboxStore outbox, String key) {
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        this.outbox = outbox;
        privacy = key.isBlank() ? null : new PrivatePayload(key);
    }

    @Override
    public Optional<OverseasSnapshot> find(UUID id) {
        return findRows("id = ?", id.toString()).stream().findFirst();
    }

    @Override
    public Optional<OverseasSnapshot> find(UUID actor, String key) {
        return findRows("actor_user_id = ? and idempotency_key = ?", actor.toString(), key).stream()
                .findFirst();
    }

    private List<OverseasSnapshot> findRows(String condition, Object... args) {
        return jdbc.query(
                "select *, (private_expires_at > now()) as private_available from"
                        + " overseas_transfers where "
                        + condition,
                (row, index) -> {
                    String data = row.getString("private_payload");
                    Remittance r =
                            data == null || !row.getBoolean("private_available")
                                    ? null
                                    : EventCodec.fromBytes(
                                            privacy()
                                                    .decrypt(row.getString("id"), data)
                                                    .getBytes(StandardCharsets.UTF_8),
                                            Remittance.class);
                    return new OverseasSnapshot(
                            UUID.fromString(row.getString("id")),
                            UUID.fromString(row.getString("actor_user_id")),
                            UUID.fromString(row.getString("from_account_id")),
                            row.getString("uetr"),
                            row.getString("request_hash"),
                            row.getString("status"),
                            r);
                },
                args);
    }

    @Override
    public OverseasSnapshot create(RequestOverseasTransferCommand c, String hash) {
        UUID id = UUID.randomUUID();
        String uetr = UUID.randomUUID().toString();
        var r =
                new Remittance(
                        id,
                        uetr,
                        id.toString().replace("-", "").substring(0, 16),
                        LocalDate.now(ZoneOffset.UTC),
                        "SYNTHETIC SENDER",
                        c.fromAccountId().toString().replace("-", ""),
                        c.recipientName(),
                        c.recipientAccount(),
                        c.bic(),
                        c.amount(),
                        c.currency(),
                        null,
                        "SHA");
        String encrypted =
                privacy()
                        .encrypt(
                                id.toString(),
                                new String(EventCodec.toBytes(r), StandardCharsets.UTF_8));
        try {
            tx.executeWithoutResult(
                    status -> {
                        jdbc.update(
                                "insert into overseas_transfers"
                                    + " (id,actor_user_id,from_account_id,idempotency_key,request_hash,uetr,amount,currency,private_payload,status)"
                                    + " values (?,?,?,?,?,?,?,?,?,'RESERVING')",
                                id.toString(),
                                c.actorUserId().toString(),
                                c.fromAccountId().toString(),
                                c.idempotencyKey(),
                                hash,
                                uetr,
                                r.amount(),
                                r.currency(),
                                encrypted);
                        auditRecord(id, "RECEIVED", encrypted);
                    });
        } catch (DuplicateKeyException concurrent) {
            var existing =
                    find(c.actorUserId(), c.idempotencyKey()).orElseThrow(SwiftProblem::conflict);
            if (!existing.requestHash().equals(hash)) throw SwiftProblem.conflict();
            return existing;
        }
        return find(id).orElseThrow();
    }

    @Override
    public boolean claim(UUID id, UUID worker) {
        return jdbc.update(
                        "update overseas_transfers set lease_owner=?,lease_until=now()+interval '30"
                                + " seconds' where id=? and status in ('RESERVING','UNKNOWN') and"
                                + " (lease_until is null or lease_until < now())",
                        worker.toString(),
                        id.toString())
                == 1;
    }

    @Override
    public void ready(UUID id, UUID worker) {
        tx.executeWithoutResult(
                status -> {
                    int n =
                            jdbc.update(
                                    "update overseas_transfers set"
                                        + " status='PROCESSING',lease_until=null,lease_owner=null"
                                        + " where id=? and lease_owner=? and status in"
                                        + " ('RESERVING','UNKNOWN')",
                                    id.toString(),
                                    worker.toString());
                    if (n == 0) return;
                    var row = find(id).orElseThrow();
                    var eventId = UUID.randomUUID();
                    outbox.append(
                            new OutboxRecord(
                                    UUID.randomUUID(),
                                    eventId,
                                    EventTopics.TRANSFER_OUTBOUND,
                                    id.toString(),
                                    EventCodec.toBytes(
                                            new TransferOutboundEvent(
                                                    eventId, id, row.uetr(), Instant.now()))));
                    auditRecord(id, "OUTBOUND", null);
                });
    }

    @Override
    public void uncertain(UUID id, UUID worker, boolean rejected) {
        tx.executeWithoutResult(
                status -> {
                    int n =
                            jdbc.update(
                                    "update overseas_transfers set"
                                        + " status=?,lease_until=null,lease_owner=null where id=?"
                                        + " and lease_owner=? and status in"
                                        + " ('RESERVING','UNKNOWN')",
                                    rejected ? "FAILED" : "UNKNOWN",
                                    id.toString(),
                                    worker.toString());
                    if (n == 1)
                        auditRecord(id, rejected ? "LEDGER_REJECTED" : "LEDGER_UNKNOWN", null);
                });
    }

    @Override
    public List<UUID> recoverable() {
        return jdbc.query(
                "select id from overseas_transfers where status in ('RESERVING','UNKNOWN') and"
                    + " private_expires_at > now() and (lease_until is null or lease_until < now())"
                    + " order by created_at limit 50",
                (row, index) -> UUID.fromString(row.getString(1)));
    }

    @Override
    public void applied(OverseasLedgerAppliedEvent e) {
        tx.executeWithoutResult(
                status -> {
                    var rows =
                            jdbc.queryForList(
                                    "select status,uetr from overseas_transfers where id=? for"
                                            + " update",
                                    e.transferId().toString());
                    if (rows.isEmpty() || !e.uetr().equals(rows.get(0).get("uetr")))
                        throw SwiftProblem.conflict();
                    String previous = (String) rows.get(0).get("status");
                    String next =
                            new OverseasTransferConvergencePolicy().applied(previous, e.outcome());
                    if (next.equals(previous)) return;
                    jdbc.update(
                            "update overseas_transfers set"
                                + " status=?,external_outcome=?,final_transaction_id=coalesce(?,final_transaction_id),final_journal_id=coalesce(?,final_journal_id)"
                                + " where id=?",
                            next,
                            e.outcome(),
                            e.transactionId() == null ? null : e.transactionId().toString(),
                            e.journalId() == null ? null : e.journalId().toString(),
                            e.transferId().toString());
                    auditRecord(e.transferId(), next, null);
                    if ("COMPLETED".equals(next)) {
                        var eventId = UUID.randomUUID();
                        outbox.append(
                                new OutboxRecord(
                                        UUID.randomUUID(),
                                        eventId,
                                        EventTopics.TRANSFER_COMPLETED,
                                        e.transferId().toString(),
                                        EventCodec.toBytes(
                                                new TransferCompletedEvent(
                                                        eventId,
                                                        e.transferId(),
                                                        e.transactionId(),
                                                        e.journalId(),
                                                        Instant.now()))));
                    }
                });
    }

    private void auditRecord(UUID id, String action, String encrypted) {
        jdbc.update(
                "insert into overseas_audit(id,transfer_id,action,private_payload) values"
                        + " (?,?,?,?)",
                UUID.randomUUID().toString(),
                id.toString(),
                action,
                encrypted);
        if (encrypted != null)
            org.slf4j.LoggerFactory.getLogger("swift.private")
                    .info(
                            "action={} transfer={} payload={}",
                            action,
                            id,
                            new String(
                                    EventCodec.toBytes(privacy().decrypt(id.toString(), encrypted)),
                                    StandardCharsets.UTF_8));
    }

    @Override
    public List<String> audit(UUID id, boolean administrator) {
        return jdbc.query(
                "select action,private_payload from overseas_audit where transfer_id=? and"
                        + " expires_at > now() order by created_at,id",
                (row, index) -> {
                    var encrypted = row.getString("private_payload");
                    return row.getString("action")
                            + (administrator && encrypted != null
                                    ? ":" + privacy().decrypt(id.toString(), encrypted)
                                    : ":[MASKED]");
                },
                id.toString());
    }

    @Override
    public void purgePrivateData() {
        tx.executeWithoutResult(
                s -> {
                    jdbc.update(
                            "update overseas_transfers set private_payload=null where"
                                + " private_expires_at <= now() and private_payload is not null");
                    jdbc.update("delete from overseas_audit where expires_at <= now()");
                });
    }

    @Override
    public Optional<OverseasTransferDetails> findByTransferId(UUID id) {
        return find(id).map(
                        r ->
                                new OverseasTransferDetails(
                                        id,
                                        r.uetr(),
                                        r.remittance() == null ? null : r.remittance().bic(),
                                        r.remittance() == null
                                                ? null
                                                : r.remittance().recipientAccount(),
                                        r.remittance() == null
                                                ? null
                                                : r.remittance().recipientName(),
                                        r.status()));
    }

    @Override
    public void save(OverseasTransferDetails value) {
        throw SwiftProblem.conflict();
    }

    private PrivatePayload privacy() {
        if (privacy == null) throw SwiftProblem.unavailable();
        return privacy;
    }
}
