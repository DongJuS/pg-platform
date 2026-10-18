package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.port.out.LedgerAtomicWritePort;
import com.shinhan.pg.ledger.application.result.PostJournalResult;

/**
 * 계좌 ID 순서로 행 잠금 후 잔액 검사·분개·잔액 갱신·ledger.posted Outbox 기록을 같은 MariaDB 트랜잭션/연결에서 처리한다.
 * transactionId로 중복 분개를 막는다. 원장 잠금 중 외부 호출을 하지 않는다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다.
 */
public final class LockedLedgerWriteSkeleton implements LedgerAtomicWritePort {
    @Override
    public PostJournalResult postWithPessimisticLocks(PostJournalCommand command) {
        throw new UnsupportedOperationException("SKELETON ledger: postWithPessimisticLocks");
    }

    @Override
    public void applyInstitutionResultWithPessimisticLocks(ApplyInstitutionResultCommand command) {
        throw new UnsupportedOperationException(
                "SKELETON ledger: applyInstitutionResultWithPessimisticLocks");
    }
}
