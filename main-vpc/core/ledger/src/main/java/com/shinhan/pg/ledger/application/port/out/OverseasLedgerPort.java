package com.shinhan.pg.ledger.application.port.out;

import com.shinhan.pg.ledger.application.command.ReserveOverseasCommand;
import com.shinhan.pg.ledger.application.result.PostJournalResult;

public interface OverseasLedgerPort {
    PostJournalResult reserveOverseas(ReserveOverseasCommand command);

    java.util.Optional<PostJournalResult> existingReservation(ReserveOverseasCommand command);
}
