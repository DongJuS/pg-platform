package com.shinhan.pg.aml.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.domain.model.AmlCase;
import com.shinhan.pg.aml.domain.model.AmlRuleSet;
import com.shinhan.pg.aml.domain.model.CaseAssessment;
import com.shinhan.pg.aml.domain.model.FinancialFact;
import com.shinhan.pg.aml.domain.policy.AmlDeduplicationPolicy;
import com.shinhan.pg.aml.domain.policy.AmlReportingDecisionPolicy;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.common.outbox.OutboxStore;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

/**
 * Service-local atomics. PostgreSQL transaction locks protect customer/currency windows and
 * immutable reporting. No HTTP here.
 */
public final class AmlWorkflowAdapter
        implements com.shinhan.pg.aml.application.port.out.AmlAtomicWritePort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final OutboxStore outbox;
    private final int maximumQueries;
    private final long queryInterval;
    private final AmlDeduplicationPolicy keys = new AmlDeduplicationPolicy();
    private final AmlReportingDecisionPolicy evaluator = new AmlReportingDecisionPolicy();

    public AmlWorkflowAdapter(DataSource ds, ObjectMapper json, OutboxStore outbox) {
        this(ds, json, outbox, 12, 10);
    }

    public AmlWorkflowAdapter(
            DataSource ds,
            ObjectMapper json,
            OutboxStore outbox,
            int maximumQueries,
            long queryInterval) {
        AmlProblem.require(
                maximumQueries >= 1
                        && maximumQueries <= 1000
                        && queryInterval >= 1
                        && queryInterval <= 86400);
        this.maximumQueries = maximumQueries;
        this.queryInterval = queryInterval;
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        this.json = json;
        this.outbox = outbox;
    }

    public void receive(EvaluateTransactionCommand c) {
        tx.executeWithoutResult(
                t -> {
                    AmlProblem.require(
                            c.eventId() != null
                                    && c.transactionId() != null
                                    && java.util.Set.of("ledger.posted", "transfer.completed")
                                            .contains(c.topic()));
                    jdbc.update(
                            "insert into aml_inbox(event_id,topic,transaction_id,journal_id)"
                                    + " values(?,?,?,?) on conflict do nothing",
                            c.eventId().toString(),
                            c.topic(),
                            c.transactionId().toString(),
                            str(c.journalId()));
                    var r =
                            jdbc.queryForMap(
                                    "select * from aml_inbox where event_id=? and topic=? for"
                                            + " update",
                                    c.eventId().toString(),
                                    c.topic());
                    if (!c.transactionId().toString().equals(r.get("transaction_id"))
                            || !java.util.Objects.equals(str(c.journalId()), r.get("journal_id")))
                        throw AmlProblem.conflict();
                });
    }

    public List<EvaluateTransactionCommand> pending(int limit) {
        return jdbc.query(
                "select * from aml_inbox where status='PENDING' and next_attempt_at<=now() order by"
                        + " created_at,event_id limit ?",
                (r, n) ->
                        new EvaluateTransactionCommand(
                                UUID.randomUUID(),
                                UUID.fromString(r.getString("event_id")),
                                UUID.fromString(r.getString("transaction_id")),
                                r.getString("topic"),
                                uuid(r.getString("journal_id"))),
                limit);
    }

    public void resolve(
            EvaluateTransactionCommand c, FinancialFact fact, String excluded, AmlRuleSet policy) {
        tx.executeWithoutResult(
                t -> {
                    var inbox =
                            jdbc.queryForMap(
                                    "select * from aml_inbox where event_id=? and topic=? for"
                                            + " update",
                                    c.eventId().toString(),
                                    c.topic());
                    if (!"PENDING".equals(inbox.get("status"))) return;
                    if (excluded != null) {
                        jdbc.update(
                                "update aml_inbox set status='EXCLUDED',reason=?,updated_at=now()"
                                        + " where event_id=? and topic=?",
                                excluded,
                                c.eventId().toString(),
                                c.topic());
                        return;
                    }
                    AmlProblem.require(
                            fact != null && c.transactionId().equals(fact.transactionId()));
                    // Serialize same customer's facts across topics, windows and rule versions; no
                    // long
                    // external call under this lock.
                    lock("transaction|" + fact.transactionId());
                    lock("business|" + fact.kind() + "|" + fact.businessId());
                    lock(fact.customerId() + "|" + fact.currency());
                    var sameBusiness =
                            jdbc.queryForList(
                                    "select transaction_id from aml_facts where kind=? and"
                                            + " business_id=?",
                                    String.class,
                                    fact.kind(),
                                    fact.businessId().toString());
                    if (!sameBusiness.isEmpty()
                            && !sameBusiness.getFirst().equals(fact.transactionId().toString())) {
                        markInbox(c, "REVIEW", "BUSINESS_EFFECT_CONFLICT");
                        return;
                    }

                    if (fact.occurredAt().isBefore(Instant.now().minusSeconds(172800))
                            || Boolean.TRUE.equals(
                                    jdbc.queryForObject(
                                            "select exists(select 1 from aml_expired_effects where"
                                                    + " effect_hash=?)",
                                            Boolean.class,
                                            digest(fact.transactionId().toString())))) {
                        markInbox(c, "REVIEW", "SOURCE_EXPIRED");
                        return;
                    }
                    var previous =
                            jdbc.queryForList(
                                    "select document from aml_facts where transaction_id=?",
                                    fact.transactionId().toString());
                    if (!previous.isEmpty()
                            && !encode(fact).equals(previous.getFirst().get("document"))) {
                        markInbox(c, "REVIEW", "SOURCE_CONFLICT");
                        jdbc.update(
                                "update aml_cases set status='REVIEW' where transaction_id=?",
                                fact.transactionId().toString());
                        jdbc.update(
                                "update aml_reports set status='REVIEW' where status='READY' and"
                                        + " case_id in(select id from aml_cases where"
                                        + " transaction_id=?)",
                                fact.transactionId().toString());
                        return;
                    }
                    jdbc.update(
                            "insert into"
                                + " aml_facts(transaction_id,customer_id,business_id,kind,currency,amount,occurred_at,original_business_id,document)"
                                + " values(?,?,?,?,?,?,?,?,?) on conflict(transaction_id) do"
                                + " nothing",
                            fact.transactionId().toString(),
                            fact.customerId().toString(),
                            fact.businessId().toString(),
                            fact.kind(),
                            fact.currency(),
                            fact.amount(),
                            java.sql.Timestamp.from(fact.occurredAt()),
                            str(fact.originalBusinessId()),
                            encode(fact));
                    if (policy != null) {
                        registerPolicy(policy);
                        evaluateAffected(fact, policy);
                    }
                    markInbox(
                            c,
                            "RESOLVED",
                            policy == null
                                    ? "POLICY_PENDING"
                                    : fact.reversal() ? "LINKED_REVERSAL" : "EVALUATED");
                });
    }

    private void markInbox(EvaluateTransactionCommand c, String status, String reason) {
        jdbc.update(
                "update aml_inbox set status=?,reason=?,updated_at=now() where event_id=? and"
                        + " topic=?",
                status,
                reason,
                c.eventId().toString(),
                c.topic());
    }

    public void defer(EvaluateTransactionCommand c, String reason) {
        tx.executeWithoutResult(
                t -> {
                    String state = "SOURCE_CONFLICT".equals(reason) ? "REVIEW" : "PENDING";
                    jdbc.update(
                            "update aml_inbox set"
                                + " status=?,reason=?,attempts=attempts+1,next_attempt_at=now()+interval"
                                + " '5 seconds',updated_at=now() where event_id=? and topic=? and"
                                + " status='PENDING'",
                            state,
                            reason,
                            c.eventId().toString(),
                            c.topic());
                });
    }

    private void registerPolicy(AmlRuleSet policy) {
        String document = encode(policy);
        jdbc.update(
                "insert into aml_rule_sets(version,document) values(?,?) on conflict do nothing",
                policy.version(),
                document);
        if (!document.equals(
                jdbc.queryForObject(
                        "select document from aml_rule_sets where version=?",
                        String.class,
                        policy.version()))) throw AmlProblem.conflict();
        for (var r : policy.rules())
            jdbc.update(
                    "insert into aml_rules(code,version,document) values(?,?,?) on conflict do"
                            + " nothing",
                    r.code(),
                    r.version(),
                    encode(r));
    }

    private void lock(String key) {
        jdbc.queryForObject(
                "select pg_advisory_xact_lock(hashtextextended(?,0))", (r, n) -> 0, key);
    }

    private List<FinancialFact> facts(UUID customer, String currency, Instant from, Instant to) {
        return jdbc.query(
                "select document from aml_facts where customer_id=? and currency=? and"
                        + " occurred_at>=? and occurred_at<=? order by occurred_at,transaction_id",
                (r, n) -> decode(r.getString(1), FinancialFact.class),
                customer.toString(),
                currency,
                java.sql.Timestamp.from(from),
                java.sql.Timestamp.from(to));
    }

    private void evaluateAffected(FinancialFact changed, AmlRuleSet policy) {
        var anchors =
                facts(
                        changed.customerId(),
                        changed.currency(),
                        changed.occurredAt(),
                        changed.occurredAt().plusSeconds(policy.maximumWindow()));
        for (var anchor : anchors) {
            if (anchor.reversal()) continue;
            var window =
                    facts(
                            anchor.customerId(),
                            anchor.currency(),
                            anchor.occurredAt().minusSeconds(policy.maximumWindow()),
                            anchor.occurredAt());
            assess(anchor, evaluator.assess(anchor, window, policy), policy);
        }
    }

    private void assess(FinancialFact fact, CaseAssessment a, AmlRuleSet policy) {
        UUID id = keys.caseId(fact.transactionId(), policy.version());
        String assessment = encode(a);
        var previous =
                jdbc.queryForList(
                        "select assessment from aml_cases where id=? for update", id.toString());
        var reported =
                jdbc.queryForList("select id from aml_reports where case_id=?", id.toString());
        if (!reported.isEmpty()) {
            if (!previous.isEmpty() && !assessment.equals(previous.getFirst().get("assessment"))) {
                jdbc.update(
                        "update aml_cases set status='REVIEW',latest_assessment=?,updated_at=now()"
                                + " where id=?",
                        assessment,
                        id.toString());
                // Additional valid facts do not revoke an already confirmed report or introduce
                // human approval before its initial send. The case review concerns amendment only.

                audit(id, "LATE_EVIDENCE_REVIEW", "system");
            }
            return;
        }
        String state =
                a.reportRequired()
                        ? "REPORT_REQUIRED"
                        : a.riskScore() > 0 ? "DETECTED" : "NO_REPORT";
        jdbc.update(
                "insert into"
                    + " aml_cases(id,case_key,transaction_id,rule_version,risk_score,status,assessment,latest_assessment)"
                    + " values(?,?,?,?,?,?,?,?) on conflict(id) do update set"
                    + " risk_score=excluded.risk_score,status=excluded.status,assessment=excluded.assessment,latest_assessment=excluded.latest_assessment,updated_at=now()",
                id.toString(),
                "case_" + id,
                fact.transactionId().toString(),
                policy.version(),
                a.riskScore(),
                state,
                assessment,
                assessment);
        if (a.reportRequired()) {
            UUID reportId = keys.reportId(id);
            var d =
                    new AmlReportDocument(
                            reportId,
                            "report_" + reportId,
                            id,
                            "STR_SIMULATION",
                            fact.customerId(),
                            fact.currency(),
                            a.totalAmount(),
                            a.transactionCount(),
                            policy.version(),
                            a.riskScore(),
                            a.windowStart(),
                            a.windowEnd(),
                            a.reasons());
            jdbc.update(
                    "insert into aml_reports(id,case_id,report_key,document,content_hash)"
                            + " values(?,?,?,?,?)",
                    reportId.toString(),
                    id.toString(),
                    d.reportKey(),
                    encode(d),
                    d.hash());
            outbox.append(
                    new OutboxRecord(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "aml.report-ready",
                            reportId.toString(),
                            encode(
                                            Map.of(
                                                    "reportId",
                                                    reportId,
                                                    "caseId",
                                                    id,
                                                    "ruleVersion",
                                                    policy.version()))
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            audit(id, "AUTO_REPORT_REQUIRED", "system");
        }
    }

    public void reevaluate(AmlRuleSet policy) {
        tx.executeWithoutResult(
                t -> {
                    registerPolicy(policy);
                    var customers =
                            jdbc.queryForList(
                                    "select distinct customer_id,currency from aml_facts order by"
                                            + " customer_id,currency");
                    for (var customer : customers) {
                        UUID user = UUID.fromString((String) customer.get("customer_id"));
                        String currency = (String) customer.get("currency");
                        lock(user + "|" + currency);
                        for (var fact :
                                jdbc.query(
                                        "select document from aml_facts where customer_id=? and"
                                                + " currency=? order by occurred_at,transaction_id",
                                        (r, n) -> decode(r.getString(1), FinancialFact.class),
                                        user.toString(),
                                        currency)) {
                            if (!fact.reversal())
                                assess(
                                        fact,
                                        evaluator.assess(
                                                fact,
                                                facts(
                                                        user,
                                                        currency,
                                                        fact.occurredAt()
                                                                .minusSeconds(
                                                                        policy.maximumWindow()),
                                                        fact.occurredAt()),
                                                policy),
                                        policy);
                        }
                    }
                });
    }

    public List<AmlReportDocument> readyReports(int limit) {
        return jdbc.query(
                "select document from aml_reports where created_at>=now()-interval '48 hours' and"
                        + " status='READY' order by created_at,id limit ?",
                (r, n) -> decode(r.getString(1), AmlReportDocument.class),
                limit);
    }

    public boolean claimReport(UUID id, UUID worker) {
        return Boolean.TRUE.equals(
                tx.execute(
                        t -> {
                            int updated =
                                    jdbc.update(
                                            "update aml_reports set"
                                                + " status='SENDING',lease_owner=?,lease_until=now()+interval"
                                                + " '30 seconds',updated_at=now() where id=? and"
                                                + " status='READY'",
                                            worker.toString(),
                                            id.toString());
                            if (updated == 1) audit(id, "INITIAL_SEND_CLAIM", "system");
                            return updated == 1;
                        }));
    }

    public void reportResult(AmlReportDocument d, UUID worker, KofiuResult result) {
        tx.executeWithoutResult(
                t -> {
                    var r = reportRow(d.reportId(), true);
                    if (worker != null && !worker.toString().equals(r.get("lease_owner"))) return;
                    if (!d.hash().equals(r.get("content_hash"))) throw AmlProblem.conflict();
                    String old = (String) r.get("status");
                    String state = "UNKNOWN",
                            institution = (String) r.get("institution_status"),
                            receipt = (String) r.get("receipt_reference");
                    int deliveries = ((Number) r.get("deliveries")).intValue(),
                            acceptances = ((Number) r.get("acceptances")).intValue();
                    if (result != null) {
                        if (!result.reportId().equals(d.reportId())
                                || !result.reportKey().equals(d.reportKey())
                                || !result.contentHash().equals(d.hash()))
                            throw AmlProblem.conflict();
                        institution = result.institutionStatus();
                        receipt = result.receiptReference();
                        deliveries = Math.max(deliveries, result.deliveries());
                        acceptances = Math.max(acceptances, result.acceptances());
                        state =
                                switch (institution) {
                                    case "ACCEPTED" -> "REPORTED";
                                    case "REJECTED" -> "REJECTED";
                                    case "REVIEW" -> "REVIEW";
                                    default ->
                                            "UNKNOWN".equals(result.transmissionStatus())
                                                    ? "UNKNOWN"
                                                    : "FEP_RECEIVED";
                                };
                    }
                    if (java.util.Set.of("REPORTED", "REJECTED", "REVIEW").contains(old)
                            && !old.equals(state)) {
                        if (result == null
                                || !java.util.Set.of("REPORTED", "REJECTED", "REVIEW")
                                        .contains(state)) {
                            state = old;
                            institution = (String) r.get("institution_status");
                        } else {
                            state = "REVIEW";
                            institution = "REVIEW";
                        }
                    }
                    jdbc.update(
                            "update aml_reports set"
                                + " status=?,institution_status=?,receipt_reference=?,deliveries=?,acceptances=?,lease_owner=null,lease_until=null,next_query_at=now()+(?"
                                + " * interval '1"
                                + " second'),query_attempts=query_attempts+?,updated_at=now() where"
                                + " id=?",
                            state,
                            institution,
                            receipt,
                            deliveries,
                            acceptances,
                            queryInterval,
                            worker == null ? 1 : 0,
                            d.reportId().toString());
                    audit(d.reportId(), state, "system");
                });
    }

    public List<AmlReportDocument> unresolvedReports(int limit) {
        jdbc.update(
                "update aml_reports set status='REVIEW' where status in"
                        + " ('UNKNOWN','FAILED','FEP_RECEIVED') and query_attempts>=? and"
                        + " next_query_at<=now()",
                maximumQueries);
        return jdbc.query(
                "select document from aml_reports where created_at>=now()-interval '48 hours' and"
                    + " (status in ('UNKNOWN','FAILED','FEP_RECEIVED') or(status='SENDING' and"
                    + " lease_until<now())) and next_query_at<=now() order by updated_at,id limit"
                    + " ?",
                (r, n) -> decode(r.getString(1), AmlReportDocument.class),
                limit);
    }

    public List<AmlCase> list(String status, Instant from, Instant to, int page, int size) {
        return jdbc.query(
                "select * from aml_cases where created_at>=now()-interval '48 hours' and"
                    + " (?::varchar is null or status=?) and (?::timestamptz is null or"
                    + " created_at>=?::timestamptz) and (?::timestamptz is null or"
                    + " created_at<?::timestamptz) order by created_at desc,id desc limit ? offset"
                    + " ?",
                (r, n) ->
                        new AmlCase(
                                UUID.fromString(r.getString("id")),
                                r.getString("case_key"),
                                UUID.fromString(r.getString("transaction_id")),
                                r.getString("rule_version"),
                                r.getInt("risk_score"),
                                r.getString("status"),
                                r.getTimestamp("created_at").toInstant()),
                status,
                status,
                str(from),
                str(from),
                str(to),
                str(to),
                size,
                (long) page * size);
    }

    public AmlCase caseById(UUID id) {
        return new AmlCasePersistenceAdapter(jdbc).findById(id).orElseThrow(AmlProblem::missing);
    }

    public com.shinhan.pg.aml.application.readmodel.AmlCaseDetails caseDetails(UUID id) {
        var summary = caseById(id);
        var row =
                jdbc.queryForMap(
                        "select assessment,latest_assessment from aml_cases where id=?",
                        id.toString());
        return new com.shinhan.pg.aml.application.readmodel.AmlCaseDetails(
                summary,
                decode((String) row.get("assessment"), CaseAssessment.class),
                decode((String) row.get("latest_assessment"), CaseAssessment.class));
    }

    public AmlReportDocument reportForCase(UUID id) {
        caseById(id);
        var docs =
                jdbc.queryForList(
                        "select document from aml_reports where case_id=? order by revision desc"
                                + " limit 1",
                        String.class,
                        id.toString());
        if (docs.isEmpty())
            throw new AmlProblem(
                    "AML-NO-CONFIRMED-REPORT",
                    com.shinhan.pg.common.commonerror.ErrorType.CONFLICT);
        return decode(docs.getFirst(), AmlReportDocument.class);
    }

    public AmlReportDocument reportDocument(UUID id) {
        return decode((String) reportRow(id, false).get("document"), AmlReportDocument.class);
    }

    public KofiuResult reportState(UUID id) {
        var r = reportRow(id, false);
        String status = (String) r.get("status");
        String transport =
                switch (status) {
                    case "READY" -> "READY";
                    case "SENDING" -> "SENDING";
                    case "UNKNOWN" -> "UNKNOWN";
                    case "FAILED" -> "FAILED";
                    default -> "SENT";
                };
        return new KofiuResult(
                id,
                (String) r.get("report_key"),
                (String) r.get("content_hash"),
                (String) r.get("receipt_reference"),
                transport,
                (String) r.get("institution_status"),
                ((Number) r.get("deliveries")).intValue(),
                ((Number) r.get("acceptances")).intValue(),
                status);
    }

    private Map<String, Object> reportRow(UUID id, boolean lock) {
        var rows =
                jdbc.queryForList(
                        "select * from aml_reports where created_at>=now()-interval '48 hours' and"
                                + " id=?"
                                + (lock ? " for update" : ""),
                        id.toString());
        if (rows.isEmpty()) throw AmlProblem.missing();
        return rows.getFirst();
    }

    public AmlReportDocument amend(UUID caseId, UUID previousReportId) {
        AmlProblem.require(caseId != null && previousReportId != null);
        return tx.execute(
                t -> {
                    var cases =
                            jdbc.queryForList(
                                    "select * from aml_cases where id=? and"
                                            + " created_at>=now()-interval '48 hours' for update",
                                    caseId.toString());
                    if (cases.isEmpty()) throw AmlProblem.missing();
                    var c = cases.getFirst();
                    var existing =
                            jdbc.queryForList(
                                    "select document from aml_reports where supersedes_id=?",
                                    String.class,
                                    previousReportId.toString());
                    if (!existing.isEmpty())
                        return decode(existing.getFirst(), AmlReportDocument.class);
                    var prior = reportRow(previousReportId, true);
                    if (!caseId.toString().equals(prior.get("case_id"))) throw AmlProblem.missing();
                    if (!"REPORTED".equals(prior.get("status"))
                            || !"REVIEW".equals(c.get("status"))) throw AmlProblem.conflict();
                    var old = decode((String) prior.get("document"), AmlReportDocument.class);
                    var latest = decode((String) c.get("latest_assessment"), CaseAssessment.class);
                    if (!latest.reportRequired() || latest.reasons().isEmpty())
                        throw AmlProblem.conflict();
                    if (old.amount().compareTo(latest.totalAmount()) == 0
                            && old.transactionCount() == latest.transactionCount()
                            && old.riskScore() == latest.riskScore()
                            && old.windowStart().equals(latest.windowStart())
                            && old.windowEnd().equals(latest.windowEnd())
                            && old.reasons().equals(latest.reasons())) throw AmlProblem.conflict();
                    UUID id =
                            UUID.nameUUIDFromBytes(
                                    ("aml-amendment|" + previousReportId)
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    var d =
                            new AmlReportDocument(
                                    id,
                                    "report_" + id,
                                    caseId,
                                    "STR_AMENDMENT_SIMULATION",
                                    old.customerId(),
                                    old.currency(),
                                    latest.totalAmount(),
                                    latest.transactionCount(),
                                    old.ruleVersion(),
                                    latest.riskScore(),
                                    latest.windowStart(),
                                    latest.windowEnd(),
                                    latest.reasons(),
                                    previousReportId,
                                    old.revision() + 1,
                                    "LATE_EVIDENCE");
                    jdbc.update(
                            "insert into"
                                + " aml_reports(id,case_id,report_key,document,content_hash,revision,supersedes_id)"
                                + " values(?,?,?,?,?,?,?)",
                            id.toString(),
                            caseId.toString(),
                            d.reportKey(),
                            encode(d),
                            d.hash(),
                            d.revision(),
                            previousReportId.toString());
                    outbox.append(
                            new OutboxRecord(
                                    UUID.randomUUID(),
                                    UUID.randomUUID(),
                                    "aml.report-ready",
                                    id.toString(),
                                    encode(
                                                    Map.of(
                                                            "reportId",
                                                            id,
                                                            "supersedesReportId",
                                                            previousReportId))
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    audit(id, "AMENDMENT_CREATED", "aml-operations");
                    return d;
                });
    }

    public void purgeExpired() {
        tx.executeWithoutResult(
                t -> {
                    jdbc.update(
                            "insert into aml_expired_effects(effect_hash) select"
                                + " encode(sha256(convert_to(transaction_id,'UTF8')),'hex') from"
                                + " aml_facts where occurred_at<now()-interval '48 hours' on"
                                + " conflict do nothing");
                    jdbc.update(
                            "delete from aml_reports where created_at<now()-interval '48 hours' or"
                                    + " case_id in(select c.id from aml_cases c join aml_facts f on"
                                    + " f.transaction_id=c.transaction_id where"
                                    + " c.created_at<now()-interval '48 hours' or"
                                    + " f.occurred_at<now()-interval '48 hours')");
                    jdbc.update("delete from aml_audit where created_at<now()-interval '48 hours'");
                    jdbc.update(
                            "delete from aml_cases where (created_at<now()-interval '48 hours' or"
                                + " transaction_id in(select transaction_id from aml_facts where"
                                + " occurred_at<now()-interval '48 hours')) and not exists(select 1"
                                + " from aml_reports r where r.case_id=aml_cases.id)");
                    jdbc.update(
                            "delete from aml_facts where occurred_at<now()-interval '48 hours' and"
                                    + " not exists(select 1 from aml_cases c where"
                                    + " c.transaction_id=aml_facts.transaction_id)");
                    jdbc.update("delete from aml_inbox where created_at<now()-interval '48 hours'");
                    // Ready-event content contains only IDs; purge private local copies after the
                    // same
                    // lifetime, even when unpublished.
                    jdbc.update(
                            "delete from outbox_events where destination='aml.report-ready' and"
                                    + " created_at<now()-interval '48 hours'");
                });
    }

    public void auditAccess(UUID id, String action, String actor) {
        tx.executeWithoutResult(t -> audit(id, action, actor));
    }

    private static String digest(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(
                            java.security.MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            value.getBytes(
                                                    java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Hash unavailable");
        }
    }

    public void audit(UUID resource, String action, String actor) {
        jdbc.update(
                "insert into aml_audit(id,resource_id,action,actor) values(?,?,?,?)",
                UUID.randomUUID().toString(),
                resource.toString(),
                action,
                actor);
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw AmlProblem.invalid();
        }
    }

    private <T> T decode(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("AML storage unavailable");
        }
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static UUID uuid(String s) {
        return s == null ? null : UUID.fromString(s);
    }
}
