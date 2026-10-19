package com.shinhan.pg.ledger.domain.policy;

import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;
import com.shinhan.pg.ledger.domain.error.LedgerException;
import com.shinhan.pg.ledger.domain.model.Journal;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Objects;

/**
 * <pre>
 * [MVP1-F13] 분개가 통화별 합계 0인지 검사한다. 부분 반영을 허용하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class JournalBalancePolicy {
    /**
     * 통화별 signedAmount 합계가 정확히 0이 아니면 LedgerException(UNBALANCED_JOURNAL).
     * posting이 없는 분개도 불균형으로 거절한다. 금액·통화 자체의 유효성(스케일 등)은
     * Money 생성 시점에 이미 강제된다.
     */
    public void validateBalanced(Journal journal) {
        Objects.requireNonNull(journal, "journal");
        if (journal.postings() == null || journal.postings().isEmpty()) {
            throw new LedgerException(
                    LedgerErrorCode.UNBALANCED_JOURNAL, "journal has no postings");
        }
        var sumsByCurrency = new HashMap<String, BigDecimal>();
        for (var posting : journal.postings()) {
            if (posting == null || posting.signedAmount() == null) {
                throw new LedgerException(
                        LedgerErrorCode.UNBALANCED_JOURNAL, "journal contains an invalid posting");
            }
            sumsByCurrency.merge(
                    posting.signedAmount().currency().getCurrencyCode(),
                    posting.signedAmount().amount(),
                    BigDecimal::add);
        }
        for (var entry : sumsByCurrency.entrySet()) {
            if (entry.getValue().signum() != 0) {
                throw new LedgerException(
                        LedgerErrorCode.UNBALANCED_JOURNAL,
                        "unbalanced journal: currency="
                                + entry.getKey()
                                + " sum="
                                + entry.getValue().toPlainString());
            }
        }
    }
}
