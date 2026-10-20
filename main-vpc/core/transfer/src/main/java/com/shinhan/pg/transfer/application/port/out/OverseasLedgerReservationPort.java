package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.application.readmodel.OverseasSnapshot;

public interface OverseasLedgerReservationPort {
    void reserve(OverseasSnapshot transfer);
}
