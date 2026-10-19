package com.shinhan.pg.auth.application.command;
import java.util.UUID;
import java.math.BigDecimal;
/** Internal proof consumption; raw token is never persisted or logged. */
public record ConsumeOtpCommand(UUID actorUserId, String otpToken, String idempotencyKey,
        UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
    @Override public String toString() { return "ConsumeOtpCommand[REDACTED]"; }
}
