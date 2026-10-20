package com.shinhan.pg.wealthreport.adapter.out.aigateway;

import com.shinhan.pg.wealthreport.application.port.out.AiReportDraftPort;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;
import com.shinhan.pg.wealthreport.domain.model.ReportDraft;

/**
 * MVP3 has preparations only. No active inference route exists. Never issue external calls or fake
 * success.
 */
public final class AiReportDraftRestSkeleton implements AiReportDraftPort {
    public ReportDraft requestDraft(FinancialMetrics metrics) {
        throw new ReportException(WealthReportErrorCode.AI_NOT_CONFIGURED);
    }
}
