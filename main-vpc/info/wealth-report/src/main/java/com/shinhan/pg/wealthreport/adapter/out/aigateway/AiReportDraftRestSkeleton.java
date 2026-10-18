package com.shinhan.pg.wealthreport.adapter.out.aigateway;

import com.shinhan.pg.wealthreport.application.port.out.AiReportDraftPort;

/**
 * AI Security Gateway에 내부 REST로 설명 초안을 요청한다. 수치는 Java에서 확정한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다.
 */
public final class AiReportDraftRestSkeleton implements AiReportDraftPort {
    @Override
    public String requestDraft(String pseudonymizedData) {
        throw new UnsupportedOperationException("SKELETON wealth-report: requestDraft");
    }
}
