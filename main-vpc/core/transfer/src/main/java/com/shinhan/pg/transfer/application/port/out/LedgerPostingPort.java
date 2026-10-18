package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.readmodel.LedgerPostingReceiptDraft;

import java.util.Optional;
import java.util.UUID;

/** 동일 transactionId로 내부 원장 REST를 호출·조회한다. 타임아웃을 새 출금으로 대체하지 않는다. */
public interface LedgerPostingPort {
    LedgerPostingReceiptDraft post(UUID transactionId, RequestTransferCommand command);

    Optional<LedgerPostingReceiptDraft> findJournal(UUID transactionId);
}
