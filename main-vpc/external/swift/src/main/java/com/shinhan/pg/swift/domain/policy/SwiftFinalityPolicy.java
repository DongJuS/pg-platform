package com.shinhan.pg.swift.domain.policy;

import com.shinhan.pg.swift.domain.model.SwiftMessage;

/**
 *
 *
 * <pre>
 * [MVP5-F08] pacs.002 ACSC/RJCT와 진행 중 응답을 구분한다. 최종성·지원 상태 상세는 후속이다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class SwiftFinalityPolicy {
    public void validateAcknowledgement(SwiftMessage message, String statusCode) {
        com.shinhan.pg.common.swiftmessages.SwiftProblem.require(
                message != null && statusCode != null && statusCode.matches("[A-Z]{4}"));
    }

    public String classify(String code) {
        if ("ACSC".equals(code) || "RJCT".equals(code)) return code;
        return java.util.Set.of("ACCP", "ACTC", "ACSP", "PDNG", "RCVD").contains(code)
                ? "PROCESSING"
                : "REVIEW";
    }

    public String converge(String previous, String incoming) {
        if ("REVIEW".equals(previous) || "REVIEW".equals(incoming)) return "REVIEW";
        if ("ACSC".equals(previous) || "RJCT".equals(previous))
            return "PROCESSING".equals(incoming) || previous.equals(incoming) ? previous : "REVIEW";
        return incoming;
    }
}
