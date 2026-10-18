package com.shinhan.pg.transfer.adapter.out.ledger;

import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.LedgerPostingPort;
import com.shinhan.pg.transfer.application.readmodel.LedgerPostingReceiptDraft;

import java.util.Optional;
import java.util.UUID;

/**
 * 동일 transactionId로 내부 원장 REST를 호출·조회한다. 타임아웃을 새 출금으로 대체하지 않는다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지
 * 않는다.
 */
public final class LedgerPostingRestSkeleton implements LedgerPostingPort {
    @Override
    public LedgerPostingReceiptDraft post(UUID transactionId, RequestTransferCommand command) {
        throw new UnsupportedOperationException("SKELETON transfer: post");
    }

    @Override
    public Optional<LedgerPostingReceiptDraft> findJournal(UUID transactionId) {
        throw new UnsupportedOperationException("SKELETON transfer: findJournal");
    }
}
