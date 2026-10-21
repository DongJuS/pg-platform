package com.shinhan.pg.common.amlcontracts;

import java.util.Set;
import java.util.UUID;

/**
 * Durable FEP receipt and institution outcome are separate. NOT_RECEIVED is authoritative Mock
 * lookup only.
 */
public record KofiuResult(
        UUID reportId,
        String reportKey,
        String contentHash,
        String receiptReference,
        String transmissionStatus,
        String institutionStatus,
        int deliveries,
        int acceptances,
        String workflowStatus) {
    public KofiuResult(
            UUID reportId,
            String reportKey,
            String contentHash,
            String receiptReference,
            String transmissionStatus,
            String institutionStatus,
            int deliveries,
            int acceptances) {
        this(
                reportId,
                reportKey,
                contentHash,
                receiptReference,
                transmissionStatus,
                institutionStatus,
                deliveries,
                acceptances,
                null);
    }

    public KofiuResult {
        AmlProblem.require(
                reportId != null
                        && reportKey != null
                        && contentHash != null
                        && contentHash.matches("[a-f0-9]{64}")
                        && transmissionStatus != null
                        && institutionStatus != null
                        && Set.of("READY", "SENDING", "SENT", "UNKNOWN", "FAILED")
                                .contains(transmissionStatus)
                        && Set.of(
                                        "NOT_RECEIVED",
                                        "PENDING",
                                        "ACCEPTED",
                                        "REJECTED",
                                        "UNKNOWN",
                                        "REVIEW")
                                .contains(institutionStatus)
                        && deliveries >= 0
                        && acceptances >= 0
                        && acceptances <= 1);
    }
}
