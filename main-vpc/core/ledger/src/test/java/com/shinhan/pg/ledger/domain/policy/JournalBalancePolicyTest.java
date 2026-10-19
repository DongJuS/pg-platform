package com.shinhan.pg.ledger.domain.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;
import com.shinhan.pg.ledger.domain.error.LedgerException;
import com.shinhan.pg.ledger.domain.model.Journal;
import com.shinhan.pg.ledger.domain.model.Posting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/** [MVP1-F13] 통화별 합계 0 불변식. */
class JournalBalancePolicyTest {
    private final JournalBalancePolicy policy = new JournalBalancePolicy();

    @Test
    void balancedTwoLegJournalPasses() {
        var journal =
                journal(
                        posting("-1000", "KRW"),
                        posting("1000", "KRW"));
        assertDoesNotThrow(() -> policy.validateBalanced(journal));
    }

    @Test
    void multiCurrencyJournalPassesWhenEachCurrencySumsToZero() {
        var journal =
                journal(
                        posting("-1000", "KRW"),
                        posting("1000", "KRW"),
                        posting("-10.00", "USD"),
                        posting("10.00", "USD"));
        assertDoesNotThrow(() -> policy.validateBalanced(journal));
    }

    @Test
    void unbalancedJournalIsRejected() {
        var journal = journal(posting("-1000", "KRW"), posting("999", "KRW"));
        var error =
                assertThrows(LedgerException.class, () -> policy.validateBalanced(journal));
        assertEquals(LedgerErrorCode.UNBALANCED_JOURNAL, error.code());
    }

    @Test
    void singleSidedPostingIsRejected() {
        var journal = journal(posting("-1000", "KRW"));
        var error =
                assertThrows(LedgerException.class, () -> policy.validateBalanced(journal));
        assertEquals(LedgerErrorCode.UNBALANCED_JOURNAL, error.code());
    }

    @Test
    void emptyJournalIsRejected() {
        var journal = new Journal(UUID.randomUUID(), UUID.randomUUID(), List.of(), Instant.now());
        var error =
                assertThrows(LedgerException.class, () -> policy.validateBalanced(journal));
        assertEquals(LedgerErrorCode.UNBALANCED_JOURNAL, error.code());
    }

    private static Journal journal(Posting... postings) {
        return new Journal(
                UUID.randomUUID(), UUID.randomUUID(), List.of(postings), Instant.now());
    }

    private static Posting posting(String amount, String currency) {
        return new Posting(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of(new BigDecimal(amount), Currency.getInstance(currency)));
    }
}
