package com.shinhan.pg.aigateway.adapter.in.web.request;

/** [MVP3-F01] 보류 중인 외부 추론 요청 DTO 초안. 활성 전송 준비 API는 PrepareAiRequest의 원문 입력을 사용한다. 신원/멱등키/경로 변수는 본문과 구분한다. 검증 제약은 주석의 계약을 따라 추가한다. */
public record RelayLlmRequest(String pseudonymizedPrompt, String requestedModel) {
    @Override
    public String toString() {
        return "RelayLlmRequest[REDACTED]";
    }
}
