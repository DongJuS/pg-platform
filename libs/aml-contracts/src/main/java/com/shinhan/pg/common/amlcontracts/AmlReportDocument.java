package com.shinhan.pg.common.amlcontracts;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Development STR_SIMULATION contract. Contains internal identifiers only; no KoFIU compliance
 * claim.
 */
public record AmlReportDocument(
        UUID reportId,
        String reportKey,
        UUID caseId,
        String reportType,
        UUID customerId,
        String currency,
        BigDecimal amount,
        int transactionCount,
        String ruleVersion,
        int riskScore,
        Instant windowStart,
        Instant windowEnd,
        List<String> reasons,
        UUID supersedesReportId,
        int revision,
        String revisionReason) {
    public AmlReportDocument {
        if (revision == 0) revision = 1;
        AmlProblem.require(
                revision >= 1
                        && revision <= 100
                        && ((revision == 1
                                        && supersedesReportId == null
                                        && revisionReason == null
                                        && "STR_SIMULATION".equals(reportType))
                                || (revision > 1
                                        && supersedesReportId != null
                                        && "LATE_EVIDENCE".equals(revisionReason)
                                        && "STR_AMENDMENT_SIMULATION".equals(reportType))));
        AmlProblem.require(
                reportId != null
                        && caseId != null
                        && customerId != null
                        && reportKey != null
                        && reportKey.matches("[A-Za-z0-9_-]{1,128}")
                        && ("STR_SIMULATION".equals(reportType)
                                || "STR_AMENDMENT_SIMULATION".equals(reportType))
                        && currency != null
                        && List.of("KRW", "USD").contains(currency)
                        && amount != null
                        && amount.signum() > 0
                        && amount.precision() <= 24
                        && amount.scale() <= 4
                        && transactionCount > 0
                        && riskScore > 0
                        && riskScore <= 10000
                        && ruleVersion != null
                        && ruleVersion.matches("[A-Za-z0-9_.-]{1,64}")
                        && windowStart != null
                        && windowEnd != null
                        && !windowEnd.isBefore(windowStart)
                        && reasons != null
                        && !reasons.isEmpty()
                        && reasons.size() <= 32);
        for (String r : reasons)
            AmlProblem.require(r != null && r.matches("[A-Za-z0-9_.:= -]{1,200}"));
        reasons = List.copyOf(reasons);
        amount = amount.stripTrailingZeros();
    }

    public AmlReportDocument(
            UUID reportId,
            String reportKey,
            UUID caseId,
            String reportType,
            UUID customerId,
            String currency,
            BigDecimal amount,
            int transactionCount,
            String ruleVersion,
            int riskScore,
            Instant windowStart,
            Instant windowEnd,
            List<String> reasons) {
        this(
                reportId,
                reportKey,
                caseId,
                reportType,
                customerId,
                currency,
                amount,
                transactionCount,
                ruleVersion,
                riskScore,
                windowStart,
                windowEnd,
                reasons,
                null,
                1,
                null);
    }

    public String hash() {
        String canonical =
                String.join(
                        "|",
                        reportId.toString(),
                        reportKey,
                        caseId.toString(),
                        reportType,
                        customerId.toString(),
                        currency,
                        amount.toPlainString(),
                        Integer.toString(transactionCount),
                        ruleVersion,
                        Integer.toString(riskScore),
                        windowStart.toString(),
                        windowEnd.toString(),
                        String.join(";", reasons),
                        supersedesReportId == null ? "" : supersedesReportId.toString(),
                        Integer.toString(revision),
                        revisionReason == null ? "" : revisionReason);
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
}
