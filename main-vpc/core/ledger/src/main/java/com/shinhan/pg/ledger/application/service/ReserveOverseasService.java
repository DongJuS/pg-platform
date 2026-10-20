package com.shinhan.pg.ledger.application.service;

import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.swiftmessages.SwiftIds;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.ledger.application.command.ReserveOverseasCommand;
import com.shinhan.pg.ledger.application.port.out.AccountQueryPort;
import com.shinhan.pg.ledger.application.port.out.OverseasLedgerPort;
import com.shinhan.pg.ledger.application.result.PostJournalResult;

public final class ReserveOverseasService {
    private final AccountQueryPort accounts;
    private final OverseasLedgerPort ledger;

    public ReserveOverseasService(AccountQueryPort accounts, OverseasLedgerPort ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    public PostJournalResult reserve(ReserveOverseasCommand command) {
        SwiftProblem.require(
                command != null
                        && command.transferId() != null
                        && command.actorUserId() != null
                        && command.fromAccountId() != null);
        SwiftIds.uetr(command.uetr());
        SwiftProblem.require(
                "USD".equals(command.currency())
                        && command.amount() != null
                        && command.amount().signum() > 0);
        try {
            SwiftProblem.require(
                    command.amount().setScale(2, java.math.RoundingMode.UNNECESSARY).precision()
                            <= 15);
        } catch (ArithmeticException invalid) {
            throw SwiftProblem.invalid();
        }
        var existing = ledger.existingReservation(command);
        if (existing.isPresent()) return existing.get();
        var account = accounts.getAccount(command.fromAccountId());
        if (!command.actorUserId().equals(account.ownerId()))
            throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
        SwiftProblem.require(
                "ACTIVE".equals(account.status()) && command.currency().equals(account.currency()));
        return ledger.reserveOverseas(command);
    }
}
