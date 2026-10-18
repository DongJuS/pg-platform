package com.shinhan.pg.aigateway.adapter.in.web.request;

/** [MVP3-F01] 요청 DTO 초안. 신원/멱등키/경로 변수는 본문과 구분한다. 검증 제약은 주석의 계약을 따라 추가한다. */
public record RelayLlmRequest(String pseudonymizedPrompt, String requestedModel) {
    @Override
    public String toString() {
        return "RelayLlmRequest[REDACTED]";
    }
}
