package com.shinhan.pg.aigateway.domain.error;

import com.shinhan.pg.aigateway.domain.model.PreparationReason;

/** Deliberately discards provider/SQL exception text and causes which may include raw input. */
public final class AiPreparationException extends RuntimeException {
    private final PreparationReason reason;
    public AiPreparationException(PreparationReason reason) {
        super(reason.name());
        this.reason = reason;
    }
    public PreparationReason reason() { return reason; }
}
