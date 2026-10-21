package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.domain.model.FinancialFact;

import java.util.UUID;

public interface AmlSourcePort {
    Resolution resolve(UUID transactionId, UUID expectedJournal);

    record Resolution(FinancialFact fact, String excludedReason) {}
}
