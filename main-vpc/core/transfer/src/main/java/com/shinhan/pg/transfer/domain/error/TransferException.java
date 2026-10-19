package com.shinhan.pg.transfer.domain.error;

import java.util.UUID;

/**
 * [MVP1-F15~F17,F20] transfer 업무 규칙 위반. 웹 계층의 TransferErrorMapper가 HTTP 상태로 변환한다.
 * Spring·JPA·Kafka에 의존하지 않는 순수 도메인 예외다(R1).
 *
 * <p>DUPLICATE_REQUEST(409)일 때 existingTransferId에 기존 이체 식별자를 담아
 * 오류 응답 detail로 전달한다(지시서 WO-A6 계약). 그 외 코드에서는 null이다.</p>
 */
public final class TransferException extends RuntimeException {
    private final TransferErrorCode code;
    private final UUID existingTransferId;

    public TransferException(TransferErrorCode code, String message) {
        this(code, message, null);
    }

    public TransferException(TransferErrorCode code, String message, UUID existingTransferId) {
        super(message);
        this.code = java.util.Objects.requireNonNull(code);
        this.existingTransferId = existingTransferId;
    }

    public TransferErrorCode code() {
        return code;
    }

    /** DUPLICATE_REQUEST에서만 의미가 있는 기존 이체 식별자. 그 외 null. */
    public UUID existingTransferId() {
        return existingTransferId;
    }
}
