package com.shinhan.pg.transfer.domain.policy;

import com.shinhan.pg.transfer.domain.model.OverseasTransferDetails;

/**
 *
 *
 * <pre>
 * [MVP5-F08] 동일 UETR의 중간/최종 결과를 구분하고 확정·역분개가 중복 수행되지 않게 한다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class OverseasTransferConvergencePolicy {
    public void validateFinalResult(OverseasTransferDetails transfer, String outcomeCode) {
        com.shinhan.pg.common.swiftmessages.SwiftProblem.require(
                transfer != null
                        && java.util.Set.of("ACSC", "RJCT", "REVIEW").contains(outcomeCode));
    }

    public String applied(String previous, String outcome) {
        if ("REVIEW".equals(previous) || "REVIEW".equals(outcome)) return "REVIEW";
        String next = "ACSC".equals(outcome) ? "COMPLETED" : "REJECTED";
        if (("COMPLETED".equals(previous) || "REJECTED".equals(previous)) && !previous.equals(next))
            return "REVIEW";
        return next;
    }
}
