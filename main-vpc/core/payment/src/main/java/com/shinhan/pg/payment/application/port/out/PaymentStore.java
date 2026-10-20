package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.readmodel.PgResult;
import com.shinhan.pg.payment.domain.model.PaymentDetails;
import com.shinhan.pg.payment.domain.model.PaymentOperation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Every mutation is one owning PostgreSQL transaction; no HTTP inside these methods. */
public interface PaymentStore {
    PaymentOperation reserveApproval(ApprovePaymentCommand command);

    PaymentOperation reserveAdjustment(
            UUID actor, UUID payment, String kind, BigDecimal amount, String key);

    PaymentDetails payment(UUID id);

    PaymentOperation operation(UUID id);

    List<PaymentOperation> operations(UUID payment);

    List<PaymentOperation> operationsOn(LocalDate date);

    boolean claimPg(UUID operation);

    PaymentOperation acceptResult(PgResult result);

    boolean claimLedger(UUID operation);

    void unknown(UUID operation, String stage);

    void complete(UUID operation, UUID journalId);
}
