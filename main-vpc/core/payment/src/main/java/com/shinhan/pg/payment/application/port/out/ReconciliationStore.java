package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.application.readmodel.ReconciliationDifferenceView;
import com.shinhan.pg.payment.application.result.GetReconciliationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReconciliationStore {
    UUID begin(LocalDate date);

    void finish(UUID id, List<ReconciliationDifferenceView> differences);

    void failed(UUID id);

    GetReconciliationResult get(UUID id);
}
