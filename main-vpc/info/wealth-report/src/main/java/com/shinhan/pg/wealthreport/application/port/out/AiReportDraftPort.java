package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;
import com.shinhan.pg.wealthreport.domain.model.ReportDraft;

/** Inference+response inspection required. Preparation readiness is never a ReportDraft. */
public interface AiReportDraftPort {
    ReportDraft requestDraft(FinancialMetrics metrics);
}
