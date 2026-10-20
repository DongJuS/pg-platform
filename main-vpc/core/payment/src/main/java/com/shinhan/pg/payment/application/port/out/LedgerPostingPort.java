package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.domain.model.PaymentDetails;
import com.shinhan.pg.payment.domain.model.PaymentOperation;

import java.util.Optional;
import java.util.UUID;

public interface LedgerPostingPort {
    UUID post(PaymentDetails payment, PaymentOperation operation);

    Optional<UUID> find(UUID operationId);

    com.shinhan.pg.payment.application.readmodel.LedgerAudit audit(UUID operationId);
}
