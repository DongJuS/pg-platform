package com.shinhan.pg.swift.adapter.out.persistence;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.common.eventcontracts.SwiftAcknowledgedEvent;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.common.swiftmessages.Iso20022;
import com.shinhan.pg.common.swiftmessages.PrivatePayload;
import com.shinhan.pg.common.swiftmessages.Remittance;
import com.shinhan.pg.common.swiftmessages.SwiftIds;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.swift.application.port.out.SwiftSubmissionStore;
import com.shinhan.pg.swift.application.readmodel.StoredSwiftMessage;
import com.shinhan.pg.swift.domain.policy.SwiftFinalityPolicy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

/** Stable instruction bytes, fenced sending leases, result inbox and atomic fep.ack Outbox. */
public final class SwiftSubmissionJdbcAdapter implements SwiftSubmissionStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final OutboxStore outbox;
    private final PrivatePayload privacy;

    public SwiftSubmissionJdbcAdapter(DataSource ds, OutboxStore outbox, String key) {
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        this.outbox = outbox;
        privacy = key.isBlank() ? null : new PrivatePayload(key);
    }

    @Override
    public StoredSwiftMessage register(Remittance r, String mt, String xml) {
        String hash = SwiftIds.hash(xml);
        tx.executeWithoutResult(
                s -> {
                    int n =
                            jdbc.update(
                                    "insert into"
                                        + " swift_messages(transfer_id,uetr,message_reference,message_hash,mt103,pacs008)"
                                        + " values (?,?,?,?,?,?) on conflict do nothing",
                                    r.transferId().toString(),
                                    r.uetr(),
                                    r.reference(),
                                    hash,
                                    crypt(r.uetr(), mt),
                                    crypt(r.uetr(), xml));
                    var rows =
                            jdbc.queryForList(
                                    "select transfer_id,message_hash from swift_messages where"
                                            + " uetr=?",
                                    r.uetr());
                    if (rows.isEmpty()
                            || !r.transferId().toString().equals(rows.get(0).get("transfer_id"))
                            || !hash.equals(rows.get(0).get("message_hash")))
                        throw SwiftProblem.conflict();
                    if (n == 1) audit(r.transferId(), "CREATED", mt + "\n" + xml, r.uetr());
                });
        return find(r.uetr()).orElseThrow();
    }

    @Override
    public Optional<StoredSwiftMessage> find(String uetr) {
        return jdbc
                .query(
                        "select *,private_expires_at > now() as available from swift_messages where"
                                + " uetr=?",
                        (r, i) ->
                                new StoredSwiftMessage(
                                        UUID.fromString(r.getString("transfer_id")),
                                        r.getString("uetr"),
                                        r.getString("message_reference"),
                                        r.getString("pacs008") == null || !r.getBoolean("available")
                                                ? null
                                                : privacy().decrypt(uetr, r.getString("pacs008")),
                                        r.getString("outcome"),
                                        r.getInt("attempt_count")),
                        uetr)
                .stream()
                .findFirst();
    }

    @Override
    public boolean claim(String uetr, UUID worker, boolean resend, int maximum, long minimum) {
        return tx.execute(
                s -> {
                    int n =
                            jdbc.update(
                                    "update swift_messages set"
                                        + " outcome='SENDING',lease_owner=?,lease_until=now()+interval"
                                        + " '30 seconds',attempt_count=attempt_count+1,last_attempt_at=now()"
                                        + " where uetr=? and private_expires_at > now() and pacs008"
                                        + " is not null and outcome="
                                            + (resend ? "'UNKNOWN'" : "'READY'")
                                            + " and attempt_count < ? and (lease_until is null or"
                                            + " lease_until < now()) and (last_attempt_at is null"
                                            + " or last_attempt_at <= now() - (? * interval '1"
                                            + " second'))",
                                    worker.toString(),
                                    uetr,
                                    maximum,
                                    resend ? minimum : 0);
                    if (n == 1) {
                        var row = find(uetr).orElseThrow();
                        audit(row.transferId(), resend ? "RETRANSMIT" : "SEND", null, uetr);
                    }
                    return n == 1;
                });
    }

    @Override
    public void sent(String uetr, UUID worker, boolean unknown) {
        jdbc.update(
                "update swift_messages set outcome=?,lease_until=null,lease_owner=null where uetr=?"
                        + " and lease_owner=? and outcome='SENDING'",
                unknown ? "UNKNOWN" : "PROCESSING",
                uetr,
                worker.toString());
    }

    @Override
    public void result(String uetr, String xml, Iso20022.Result r) {
        tx.executeWithoutResult(
                s -> {
                    var rows =
                            jdbc.queryForList(
                                    "select * from swift_messages where uetr=? for update", uetr);
                    if (rows.isEmpty()) throw SwiftProblem.conflict();
                    var row = rows.get(0);
                    if (!uetr.equals(r.uetr())
                            || !row.get("message_reference").equals(r.messageReference()))
                        throw SwiftProblem.conflict();
                    UUID id = UUID.fromString((String) row.get("transfer_id"));
                    String hash = SwiftIds.hash(xml);
                    var inbox =
                            jdbc.queryForList(
                                    "select transfer_id,result_hash from swift_result_inbox where"
                                            + " event_reference=?",
                                    r.eventReference());
                    String incoming = new SwiftFinalityPolicy().classify(r.code());
                    if (!inbox.isEmpty()) {
                        if (id.toString().equals(inbox.get(0).get("transfer_id"))
                                && hash.equals(inbox.get(0).get("result_hash"))) return;
                        incoming = "REVIEW";
                    } else
                        jdbc.update(
                                "insert into"
                                    + " swift_result_inbox(event_reference,transfer_id,result_hash)values(?,?,?)",
                                r.eventReference(),
                                id.toString(),
                                hash);
                    String previous = (String) row.get("outcome");
                    String outcome = new SwiftFinalityPolicy().converge(previous, incoming);
                    jdbc.update(
                            "update swift_messages set"
                                + " outcome=?,result_xml=?,lease_until=null,lease_owner=null where"
                                + " uetr=?",
                            outcome,
                            crypt(uetr, xml),
                            uetr);
                    audit(id, "RESULT_" + r.code(), xml, uetr);
                    if (!outcome.equals(previous)
                            && java.util.Set.of("ACSC", "RJCT", "REVIEW").contains(outcome)) {
                        UUID eventId = UUID.randomUUID();
                        outbox.append(
                                new OutboxRecord(
                                        UUID.randomUUID(),
                                        eventId,
                                        EventTopics.FEP_ACK,
                                        id.toString(),
                                        EventCodec.toBytes(
                                                new SwiftAcknowledgedEvent(
                                                        eventId,
                                                        id,
                                                        uetr,
                                                        outcome,
                                                        Instant.now()))));
                    }
                });
    }

    @Override
    public void transmissionUnknown(String uetr) {
        jdbc.update(
                "update swift_messages set outcome='UNKNOWN' where uetr=? and outcome='PROCESSING'",
                uetr);
    }

    @Override
    public List<StoredSwiftMessage> due() {
        // An abandoned send becomes UNKNOWN, never a new initial submission.
        jdbc.update(
                "update swift_messages set outcome='UNKNOWN',lease_until=null,lease_owner=null"
                        + " where outcome='SENDING' and lease_until < now()");
        return jdbc
                .query(
                        "select uetr from swift_messages where outcome <> 'REVIEW' and"
                                + " next_query_at <= now() and private_expires_at > now() order by"
                                + " next_query_at limit 50",
                        (r, i) -> r.getString(1))
                .stream()
                .map(u -> find(u).orElseThrow())
                .toList();
    }

    @Override
    public void deferQuery(String uetr) {
        jdbc.update(
                "update swift_messages set next_query_at=now()+interval '10 seconds' where uetr=?",
                uetr);
    }

    @Override
    public List<String> history(String uetr, boolean raw) {
        var found = find(uetr).orElseThrow(SwiftProblem::invalid);
        return jdbc.query(
                "select action,private_payload from swift_private_audit where transfer_id=? and"
                        + " expires_at > now() order by created_at,id",
                (r, i) ->
                        r.getString("action")
                                + (raw && r.getString("private_payload") != null
                                        ? ":"
                                                + privacy()
                                                        .decrypt(
                                                                uetr,
                                                                r.getString("private_payload"))
                                        : ":[MASKED]"),
                found.transferId().toString());
    }

    @Override
    public void purge() {
        tx.executeWithoutResult(
                s -> {
                    var expired =
                            jdbc.queryForList(
                                    "select transfer_id,uetr from swift_messages where"
                                        + " private_expires_at <= now() and outcome in"
                                        + " ('READY','SENDING','UNKNOWN','PROCESSING') for update");
                    for (var row : expired) {
                        UUID id = UUID.fromString((String) row.get("transfer_id"));
                        String uetr = (String) row.get("uetr");
                        jdbc.update(
                                "update swift_messages set"
                                    + " outcome='REVIEW',lease_owner=null,lease_until=null where"
                                    + " transfer_id=?",
                                id.toString());
                        UUID eventId = UUID.randomUUID();
                        outbox.append(
                                new OutboxRecord(
                                        UUID.randomUUID(),
                                        eventId,
                                        EventTopics.FEP_ACK,
                                        id.toString(),
                                        EventCodec.toBytes(
                                                new SwiftAcknowledgedEvent(
                                                        eventId,
                                                        id,
                                                        uetr,
                                                        "REVIEW",
                                                        Instant.now()))));
                    }
                    jdbc.update(
                            "update swift_messages set mt103=null,pacs008=null,result_xml=null"
                                    + " where private_expires_at <= now()");
                    jdbc.update("delete from swift_private_audit where expires_at <= now()");
                });
    }

    private void audit(UUID id, String action, String data, String uetr) {
        jdbc.update(
                "insert into"
                    + " swift_private_audit(id,transfer_id,action,private_payload)values(?,?,?,?)",
                UUID.randomUUID().toString(),
                id.toString(),
                action,
                data == null ? null : crypt(uetr, data));
        if (data != null)
            org.slf4j.LoggerFactory.getLogger("swift.private")
                    .info(
                            "action={} uetr={} payload={}",
                            action,
                            uetr,
                            new String(
                                    EventCodec.toBytes(data),
                                    java.nio.charset.StandardCharsets.UTF_8));
    }

    private String crypt(String id, String text) {
        return privacy().encrypt(id, text);
    }

    private PrivatePayload privacy() {
        if (privacy == null) throw SwiftProblem.unavailable();
        return privacy;
    }
}
