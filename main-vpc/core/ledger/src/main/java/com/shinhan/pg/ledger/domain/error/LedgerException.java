package com.shinhan.pg.ledger.domain.error;

import java.util.Objects;

/**
 * 원장 업무 거절 신호. Spring·JPA 등 인프라에 의존하지 않는다(R1).
 * HTTP 상태 매핑은 adapter/in/web의 LedgerErrorMapper + LedgerExceptionHandler가 담당한다.
 */
public final class LedgerException extends RuntimeException {
    private final LedgerErrorCode code;

    public LedgerException(LedgerErrorCode code) {
        super(code.name());
        this.code = Objects.requireNonNull(code);
    }

    public LedgerException(LedgerErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code);
    }

    public LedgerErrorCode code() {
        return code;
    }
}
