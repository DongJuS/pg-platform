package com.shinhan.pg.wealthreport.application.port.out;

/** AI Security Gateway에 내부 REST로 설명 초안을 요청한다. 수치는 Java에서 확정한다. */
public interface AiReportDraftPort {
    String requestDraft(String pseudonymizedData);
}
