package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.common.amlcontracts.AmlSourceFact;

import java.util.UUID;

public interface AmlFactReadPort {
    AmlSourceFact read(UUID transactionId);
}
