package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.application.readmodel.PgResult;
import com.shinhan.pg.payment.domain.model.PaymentOperation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** FEP receipt alone never represents PG final success. No financial retransmission. */
public interface FepPaymentPort {
    PgResult submit(PaymentOperation operation);

    Optional<PgResult> query(UUID operationId);

    List<PgResult> results(LocalDate date);
}
