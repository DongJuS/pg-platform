package com.shinhan.pg.fepadapter.adapter.out.persistence;

import com.shinhan.pg.common.swiftmessages.Iso20022;
import com.shinhan.pg.common.swiftmessages.PrivatePayload;
import com.shinhan.pg.common.swiftmessages.SwiftIds;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.fepadapter.application.command.SendSwiftMessageCommand;
import com.shinhan.pg.fepadapter.application.port.out.SwiftReceiptStore;
import com.shinhan.pg.fepadapter.application.readmodel.SwiftReceipt;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

public final class SwiftReceiptJdbcAdapter implements SwiftReceiptStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PrivatePayload privacy;

    public SwiftReceiptJdbcAdapter(DataSource ds, String key) {
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        privacy = new PrivatePayload(key);
    }

    @Override
    public SwiftReceipt register(SendSwiftMessageCommand c, String reference) {
        tx.executeWithoutResult(
                s -> {
                    String hash = SwiftIds.hash(c.pacs008());
                    jdbc.update(
                            "insert into"
                                + " swift_receipts(transfer_id,uetr,message_id,message_reference,message_key,message_hash,pacs008)"
                                + " values (?,?,?,?,?,?,?) on conflict do nothing",
                            c.transferId().toString(),
                            c.uetr(),
                            SwiftIds.id(c.transferId(), "fep-message").toString(),
                            reference,
                            c.messageKey(),
                            hash,
                            privacy.encrypt(c.uetr(), c.pacs008()));
                    var rows =
                            jdbc.queryForList(
                                    "select transfer_id,message_key,message_hash from"
                                            + " swift_receipts where uetr=?",
                                    c.uetr());
                    if (rows.isEmpty()
                            || !c.transferId().toString().equals(rows.get(0).get("transfer_id"))
                            || !c.messageKey().equals(rows.get(0).get("message_key"))
                            || !hash.equals(rows.get(0).get("message_hash")))
                        throw SwiftProblem.conflict();
                });
        return find(c.uetr()).orElseThrow();
    }

    @Override
    public Optional<SwiftReceipt> find(String uetr) {
        return jdbc
                .query(
                        "select *,private_expires_at > now() as available,case when"
                            + " outcome='SENDING' and lease_until < now() then 'UNKNOWN' else"
                            + " outcome end as effective_outcome from swift_receipts where uetr=?",
                        (r, i) ->
                                new SwiftReceipt(
                                        UUID.fromString(r.getString("transfer_id")),
                                        UUID.fromString(r.getString("message_id")),
                                        uetr,
                                        r.getString("message_reference"),
                                        r.getString("pacs008") == null || !r.getBoolean("available")
                                                ? null
                                                : privacy.decrypt(uetr, r.getString("pacs008")),
                                        r.getString("effective_outcome"),
                                        r.getInt("attempt_count")),
                        uetr)
                .stream()
                .findFirst();
    }

    @Override
    public boolean claim(String uetr, UUID worker, boolean resend, int maximum, long minimum) {
        return tx.execute(
                s -> {
                    jdbc.update(
                            "update swift_receipts set"
                                    + " outcome='UNKNOWN',lease_until=null,lease_owner=null where"
                                    + " uetr=? and outcome='SENDING' and lease_until < now()",
                            uetr);
                    int n =
                            jdbc.update(
                                    "update swift_receipts set"
                                        + " outcome='SENDING',lease_owner=?,lease_until=now()+interval"
                                        + " '30 seconds',attempt_count=attempt_count+1,last_attempt_at=now()"
                                        + " where uetr=? and private_expires_at > now() and pacs008"
                                        + " is not null and outcome="
                                            + (resend ? "'UNKNOWN'" : "'READY'")
                                            + " and attempt_count < ? and (last_attempt_at is null"
                                            + " or last_attempt_at <= now()-(?*interval '1"
                                            + " second'))",
                                    worker.toString(),
                                    uetr,
                                    maximum,
                                    resend ? minimum : 0);
                    if (n == 1) {
                        var row = find(uetr).orElseThrow();
                        jdbc.update(
                                "insert into"
                                    + " swift_transmission_attempts(id,transfer_id,attempt_number,action,outcome)values(?,?,?,?,?)",
                                worker.toString(),
                                row.transferId().toString(),
                                row.attempts(),
                                resend ? "RETRANSMIT" : "SEND",
                                "SENDING");
                    }
                    return n == 1;
                });
    }

    @Override
    public void sent(String uetr, UUID worker, boolean unknown) {
        tx.executeWithoutResult(
                s -> {
                    jdbc.update(
                            "update swift_receipts set outcome=?,lease_owner=null,lease_until=null"
                                    + " where uetr=? and lease_owner=? and outcome='SENDING'",
                            unknown ? "UNKNOWN" : "ACCEPTED",
                            uetr,
                            worker.toString());
                    jdbc.update(
                            "update swift_transmission_attempts set outcome=? where id=?",
                            unknown ? "UNKNOWN" : "ACCEPTED",
                            worker.toString());
                });
    }

    @Override
    public void result(String uetr, String xml) {
        var result = Iso20022.result(xml);
        tx.executeWithoutResult(
                s -> {
                    var rows =
                            jdbc.queryForList(
                                    "select transfer_id,message_reference,outcome from"
                                            + " swift_receipts where uetr=? for update",
                                    uetr);
                    if (rows.isEmpty()
                            || !uetr.equals(result.uetr())
                            || !rows.get(0)
                                    .get("message_reference")
                                    .equals(result.messageReference()))
                        throw SwiftProblem.conflict();
                    int n =
                            jdbc.update(
                                    "insert into"
                                        + " swift_result_reports(id,uetr,event_reference,result_hash,private_payload)values(?,?,?,?,?)on"
                                        + " conflict do nothing",
                                    UUID.randomUUID().toString(),
                                    uetr,
                                    result.eventReference(),
                                    SwiftIds.hash(xml),
                                    privacy.encrypt(uetr, xml));
                    if (n == 1)
                        jdbc.update(
                                "update swift_receipts set result_xml=? where uetr=?",
                                privacy.encrypt(uetr, xml),
                                uetr);
                });
    }

    @Override
    public List<String> reports(String uetr) {
        return jdbc.query(
                "select private_payload from swift_result_reports where uetr=? and expires_at >"
                        + " now() and private_payload is not null order by received_at,id",
                (r, i) -> privacy.decrypt(uetr, r.getString(1)),
                uetr);
    }

    @Override
    public void purge() {
        tx.executeWithoutResult(
                s -> {
                    jdbc.update(
                            "update swift_receipts set pacs008=null,result_xml=null where"
                                    + " private_expires_at <= now()");
                    jdbc.update(
                            "update swift_result_reports set private_payload=null where expires_at"
                                    + " <= now()");
                });
    }
}
