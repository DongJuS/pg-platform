package com.shinhan.pg.payment.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/** F01: actor is trusted identity, accounts are explicit; requestId is never the financial key. */
public record ApprovePaymentCommand(
        UUID requestId,
        UUID actorUserId,
        String orderReference,
        UUID merchantId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        UUID sourceAccountId,
        UUID destinationAccountId) {}
