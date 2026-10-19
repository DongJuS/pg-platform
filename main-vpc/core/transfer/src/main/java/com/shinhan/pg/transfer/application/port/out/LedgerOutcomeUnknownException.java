package com.shinhan.pg.transfer.application.port.out;

/** 원장 REST 호출 결과를 확정할 수 없을 때(타임아웃·연결 실패·5xx·기타 비정상) 던진다.
 * 호출자는 이 예외를 실패 확정이 아닌 UNKNOWN 전이 신호로만 사용한다. 재출금으로 대체하지 않는다. */
public final class LedgerOutcomeUnknownException extends RuntimeException {
    public LedgerOutcomeUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
