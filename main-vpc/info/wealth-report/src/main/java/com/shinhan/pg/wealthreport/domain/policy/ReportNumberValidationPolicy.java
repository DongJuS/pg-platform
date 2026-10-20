package com.shinhan.pg.wealthreport.domain.policy;

import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;
import com.shinhan.pg.wealthreport.domain.model.ReportDraft;

/**
 * Numeric assertions are structured and exact. Free prose may not contain any number, currency
 * assertion or restoration token. This is a conservative bounded format, not a PII detector.
 */
public final class ReportNumberValidationPolicy {
    public void validate(FinancialMetrics m, ReportDraft d) {
        if (d == null
                || !m.totalAssets().amount().toPlainString().equals(d.totalAssets())
                || !m.cashIn().amount().toPlainString().equals(d.cashIn())
                || !m.cashOut().amount().toPlainString().equals(d.cashOut())
                || !"KRW".equals(d.currency())
                || !m.asOf().equals(d.asOf())
                || !m.calculationVersion().equals(d.calculationVersion())
                || !("TEST_DOUBLE".equals(d.source()) || "AI".equals(d.source()))
                || d.explanation() == null
                || d.explanation().isBlank()
                || d.explanation().length() > 3000
                || !d.explanation().matches("[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z .,\n]+")
                || d.explanation().matches("(?is).*(KRW|USD|EUR|JPY|원|달러|엔|유로).*"))
            throw new ReportException(WealthReportErrorCode.AI_RESULT_INVALID);
    }
}
