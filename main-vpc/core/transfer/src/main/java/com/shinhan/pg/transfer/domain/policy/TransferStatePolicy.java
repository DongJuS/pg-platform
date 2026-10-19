package com.shinhan.pg.transfer.domain.policy;

import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;
import com.shinhan.pg.transfer.domain.model.Transfer;

import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * [MVP1-F16,MVP1-F20] RECEIVED/PROCESSING/COMPLETED/FAILED/UNKNOWN의 허용 전이를 정의한다. UNKNOWN을 404만으로 실패 처리하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 *
 * <p>확정 전이(WO-A6): RECEIVED→PROCESSING, PROCESSING→COMPLETED/FAILED/UNKNOWN,
 * UNKNOWN→COMPLETED/FAILED. COMPLETED·FAILED는 최종 상태이며 역전·재전이를 금지한다.
 * 동일 상태로의 전이는 이 클래스에서 허용하지 않는다 — 멱등 완료의 중복 흡수는
 * 영속성 계층의 조건부 update(completeOnceWithOutbox)가 담당한다.</p>
 */
public final class TransferStatePolicy {
    public static final String RECEIVED = "RECEIVED";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
    public static final String UNKNOWN = "UNKNOWN";

    private static final Map<String, Set<String>> ALLOWED =
            Map.of(
                    RECEIVED, Set.of(PROCESSING),
                    PROCESSING, Set.of(COMPLETED, FAILED, UNKNOWN),
                    UNKNOWN, Set.of(COMPLETED, FAILED));

    /** 현재 상태에서 nextStatus로의 전이가 허용되는지 검증한다. 불허 시 INVALID_STATE(409). */
    public void validateTransition(Transfer current, String nextStatus) {
        java.util.Objects.requireNonNull(current);
        validateTransition(current.status(), nextStatus);
    }

    public void validateTransition(String currentStatus, String nextStatus) {
        if (!ALLOWED.getOrDefault(currentStatus, Set.of()).contains(nextStatus)) {
            throw new TransferException(
                    TransferErrorCode.INVALID_STATE,
                    "Transfer status transition not allowed: " + currentStatus + " -> " + nextStatus);
        }
    }

    /** 최종 상태(COMPLETED/FAILED) 여부. 최종 상태는 어떤 전이도 허용하지 않는다. */
    public boolean isTerminal(String status) {
        return COMPLETED.equals(status) || FAILED.equals(status);
    }
}
