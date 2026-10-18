package com.shinhan.pg.ledger.application.port.out;

import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.result.PostJournalResult;

/**
 * 계좌 ID 순서로 행 잠금 후 잔액 검사·분개·잔액 갱신·ledger.posted Outbox 기록을 같은 MariaDB 트랜잭션/연결에서 처리한다.
 * transactionId로 중복 분개를 막는다. 원장 잠금 중 외부 호출을 하지 않는다.
 */
public interface LedgerAtomicWritePort {
    PostJournalResult postWithPessimisticLocks(PostJournalCommand command);

    void applyInstitutionResultWithPessimisticLocks(ApplyInstitutionResultCommand command);
}
