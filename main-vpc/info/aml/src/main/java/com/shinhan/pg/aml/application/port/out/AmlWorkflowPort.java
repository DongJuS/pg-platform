package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.domain.model.AmlCase;
import com.shinhan.pg.aml.domain.model.AmlRuleSet;
import com.shinhan.pg.aml.domain.model.FinancialFact;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AmlWorkflowPort {
    void receive(EvaluateTransactionCommand command);

    List<EvaluateTransactionCommand> pending(int limit);

    void resolve(
            EvaluateTransactionCommand command,
            FinancialFact fact,
            String excluded,
            AmlRuleSet policy);

    void defer(EvaluateTransactionCommand command, String reason);

    List<AmlReportDocument> readyReports(int limit);

    boolean claimReport(UUID id, UUID worker);

    void reportResult(AmlReportDocument document, UUID worker, KofiuResult result);

    List<AmlReportDocument> unresolvedReports(int limit);

    List<AmlCase> list(String status, Instant from, Instant to, int page, int size);

    AmlCase caseById(UUID id);

    com.shinhan.pg.aml.application.readmodel.AmlCaseDetails caseDetails(UUID id);

    AmlReportDocument reportForCase(UUID caseId);

    AmlReportDocument reportDocument(UUID reportId);

    KofiuResult reportState(UUID id);

    void reevaluate(AmlRuleSet policy);

    AmlReportDocument amend(UUID caseId, UUID previousReportId);

    void purgeExpired();

    void auditAccess(UUID resourceId, String action, String actor);
}
