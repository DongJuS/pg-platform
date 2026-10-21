package com.shinhan.pg.ledger.application.service;

import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.LedgerEvidence;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.ledger.application.port.out.AmlFactReadPort;

import java.util.UUID;

public final class AmlFactReadService {
    private final AmlFactReadPort port;

    public AmlFactReadService(AmlFactReadPort port) {
        this.port = port;
    }

    public LedgerEvidence read(String caller, UUID transactionId) {
        if (!"aml".equals(caller))
            throw new AmlProblem("AML-SOURCE-FORBIDDEN", ErrorType.FORBIDDEN);
        return port.read(transactionId);
    }
}
