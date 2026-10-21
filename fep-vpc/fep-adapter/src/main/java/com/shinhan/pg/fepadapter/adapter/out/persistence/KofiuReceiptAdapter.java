package com.shinhan.pg.fepadapter.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.fepadapter.application.port.out.KofiuReceiptPort;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

public final class KofiuReceiptAdapter implements KofiuReceiptPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;

    public KofiuReceiptAdapter(DataSource ds, ObjectMapper json) {
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        this.json = json;
    }

    public KofiuResult register(AmlReportDocument d) {
        return tx.execute(
                t -> {
                    if (Boolean.TRUE.equals(
                            jdbc.queryForObject(
                                    "select exists(select 1 from kofiu_expired_reports where"
                                            + " key_hash=?)",
                                    Boolean.class,
                                    com.shinhan.pg.common.amlcontracts.AmlHashes.hash(
                                            d.reportKey())))) throw AmlProblem.conflict();
                    jdbc.update(
                            "insert into"
                                + " kofiu_receipts(report_id,report_key,content_hash,document,receipt_reference)"
                                + " values(?,?,?,?,?) on conflict do nothing",
                            d.reportId().toString(),
                            d.reportKey(),
                            d.hash(),
                            encode(d),
                            "FEP_" + d.reportId());
                    var rows =
                            jdbc.queryForList(
                                    "select * from kofiu_receipts where created_at>=now()-interval"
                                            + " '48 hours' and report_key=? for update",
                                    d.reportKey());
                    if (rows.isEmpty()) throw AmlProblem.conflict();
                    var r = rows.getFirst();
                    if (!d.hash().equals(r.get("content_hash"))
                            || !d.reportId().toString().equals(r.get("report_id")))
                        throw AmlProblem.conflict();
                    return result(r);
                });
    }

    public KofiuResult get(String key) {
        return result(row(key, false));
    }

    public AmlReportDocument document(String key) {
        try {
            return json.readValue(
                    (String) row(key, false).get("document"), AmlReportDocument.class);
        } catch (AmlProblem e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("AML storage unavailable");
        }
    }

    private Map<String, Object> row(String key, boolean lock) {
        AmlProblem.require(key != null && key.matches("[A-Za-z0-9_-]{1,128}"));
        var rows =
                jdbc.queryForList(
                        "select * from kofiu_receipts where created_at>=now()-interval '48 hours'"
                                + " and report_key=?"
                                + (lock ? " for update" : ""),
                        key);
        if (rows.isEmpty()) throw AmlProblem.missing();
        return rows.getFirst();
    }

    public boolean claim(String key, UUID worker, boolean retry, int maximum, long interval) {
        return Boolean.TRUE.equals(
                tx.execute(
                        t -> {
                            var r = row(key, true);
                            String status = (String) r.get("transmission_status");
                            int attempts = ((Number) r.get("attempts")).intValue();
                            if (retry) {
                                if (!"NOT_RECEIVED".equals(r.get("institution_status"))
                                        || attempts == 0
                                        || attempts >= maximum
                                        || "SENDING".equals(status)) return false;
                                var last = (java.sql.Timestamp) r.get("last_sent_at");
                                if (last == null
                                        || last.toInstant()
                                                .plusSeconds(interval)
                                                .isAfter(Instant.now())) return false;
                            } else if (!"READY".equals(status) || attempts != 0) return false;
                            jdbc.update(
                                    "update kofiu_receipts set transmission_status='SENDING',"
                                        + " attempts=attempts+1,lease_owner=?,lease_until=now()+interval"
                                        + " '30 seconds',last_sent_at=now(),updated_at=now() where"
                                        + " report_key=?",
                                    worker.toString(),
                                    key);
                            jdbc.update(
                                    "insert into kofiu_attempts(id,report_id,attempt_number,status)"
                                            + " values(?,?,?,'SENDING')",
                                    worker.toString(),
                                    r.get("report_id"),
                                    attempts + 1);
                            return true;
                        }));
    }

    public void finish(String key, UUID worker, KofiuResult result) {
        tx.executeWithoutResult(
                t -> {
                    var r = row(key, true);
                    if (!worker.toString().equals(r.get("lease_owner"))) return;
                    if (result != null) apply(key, r, result);
                    jdbc.update(
                            "update kofiu_receipts set"
                                + " transmission_status=?,lease_owner=null,lease_until=null,updated_at=now()"
                                + " where report_key=? and lease_owner=?",
                            result == null ? "UNKNOWN" : "SENT",
                            key,
                            worker.toString());
                    jdbc.update(
                            "update kofiu_attempts set status=? where id=?",
                            result == null ? "UNKNOWN" : "SENT",
                            worker.toString());
                });
    }

    public void purgeExpired() {
        tx.executeWithoutResult(
                t -> {
                    jdbc.update(
                            "insert into kofiu_expired_reports(key_hash,content_hash) select"
                                + " encode(sha256(convert_to(report_key,'UTF8')),'hex'),content_hash"
                                + " from kofiu_receipts where created_at<now()-interval '48 hours'"
                                + " on conflict do nothing");
                    jdbc.update(
                            "delete from kofiu_attempts where report_id in(select report_id from"
                                + " kofiu_receipts where created_at<now()-interval '48 hours')");
                    jdbc.update(
                            "delete from kofiu_receipts where created_at<now()-interval '48"
                                    + " hours'");
                });
    }

    public void observe(String key, KofiuResult result) {
        tx.executeWithoutResult(
                t -> {
                    var r = row(key, true);
                    apply(key, r, result);
                    // A dead sender is uncertain, never READY; its original attempt cannot be
                    // automatically
                    // repeated.
                    jdbc.update(
                            "update kofiu_receipts set"
                                + " transmission_status='UNKNOWN',lease_owner=null,lease_until=null"
                                + " where report_key=? and transmission_status='SENDING' and"
                                + " lease_until<now()",
                            key);
                });
    }

    private void apply(String key, Map<String, Object> r, KofiuResult v) {
        if (!v.reportId().toString().equals(r.get("report_id"))
                || !key.equals(v.reportKey())
                || !v.contentHash().equals(r.get("content_hash"))) throw AmlProblem.conflict();
        String old = (String) r.get("institution_status"), next = v.institutionStatus();
        if ("REVIEW".equals(old)
                || (("ACCEPTED".equals(old) || "REJECTED".equals(old)) && !old.equals(next))) {
            if ("ACCEPTED".equals(next) || "REJECTED".equals(next)) next = "REVIEW";
            else next = old;
        }
        jdbc.update(
                "update kofiu_receipts set"
                    + " institution_status=?,deliveries=greatest(deliveries,?),acceptances=greatest(acceptances,?),updated_at=now()"
                    + " where report_key=?",
                next,
                v.deliveries(),
                v.acceptances(),
                key);
    }

    private KofiuResult result(Map<String, Object> r) {
        String transmission = (String) r.get("transmission_status");
        var lease = (java.sql.Timestamp) r.get("lease_until");
        if ("SENDING".equals(transmission)
                && lease != null
                && lease.toInstant().isBefore(Instant.now())) transmission = "UNKNOWN";
        return new KofiuResult(
                UUID.fromString((String) r.get("report_id")),
                (String) r.get("report_key"),
                (String) r.get("content_hash"),
                (String) r.get("receipt_reference"),
                transmission,
                (String) r.get("institution_status"),
                ((Number) r.get("deliveries")).intValue(),
                ((Number) r.get("acceptances")).intValue());
    }

    private String encode(AmlReportDocument d) {
        try {
            return json.writeValueAsString(d);
        } catch (Exception e) {
            throw AmlProblem.invalid();
        }
    }
}
