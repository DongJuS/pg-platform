package com.shinhan.pg.aigateway.application.result;

import java.util.UUID;

/** [MVP3-F01] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record RelayLlmResult(
        UUID requestId, String output, Integer inputTokens, Integer outputTokens) {
    @Override
    public String toString() {
        return "RelayLlmResult[REDACTED]";
    }
}
