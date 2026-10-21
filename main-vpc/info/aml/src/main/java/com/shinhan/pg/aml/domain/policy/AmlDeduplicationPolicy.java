package com.shinhan.pg.aml.domain.policy;

import com.shinhan.pg.common.amlcontracts.AmlProblem;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class AmlDeduplicationPolicy {
    public String effectKey(UUID transactionId, String ruleVersion, String windowKey) {
        AmlProblem.require(transactionId != null);
        return "ledger_" + transactionId;
    }

    public UUID caseId(UUID transactionId, String version) {
        return UUID.nameUUIDFromBytes(
                ("aml-case|" + transactionId + "|" + version).getBytes(StandardCharsets.UTF_8));
    }

    public UUID reportId(UUID caseId) {
        return UUID.nameUUIDFromBytes(("aml-report|" + caseId).getBytes(StandardCharsets.UTF_8));
    }
}
