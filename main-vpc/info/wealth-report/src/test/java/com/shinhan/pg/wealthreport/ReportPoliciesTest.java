package com.shinhan.pg.wealthreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.wealthreport.adapter.out.aigateway.AiReportDraftRestSkeleton;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.AccountFigures;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;
import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;
import com.shinhan.pg.wealthreport.domain.model.ReportDraft;
import com.shinhan.pg.wealthreport.domain.policy.FinancialMetricsCalculator;
import com.shinhan.pg.wealthreport.domain.policy.ReportNumberValidationPolicy;
import com.shinhan.pg.wealthreport.domain.policy.SnapshotConsistencyPolicy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

class ReportPoliciesTest {
    static final Instant AS_OF = Instant.parse("2026-10-19T00:00:00Z");

    static AccountFigures figures(String b, String in, String out) {
        return new AccountFigures(
                UUID.randomUUID(),
                new BigDecimal(b),
                new BigDecimal(in),
                new BigDecimal(out),
                "KRW",
                1);
    }

    static AssetSnapshot snapshot(List<AccountFigures> a) {
        return new AssetSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AS_OF.minusSeconds(86400),
                AS_OF,
                AS_OF,
                AS_OF,
                "source-test",
                a);
    }

    @Test
    void sumsExactKrwWithoutFxOrHiddenRounding() {
        var m =
                new FinancialMetricsCalculator()
                        .calculate(
                                snapshot(
                                        List.of(
                                                figures("100000.0000", "50000", "10000"),
                                                figures("25000", "10000", "5000"))));
        assertEquals("125000", m.totalAssets().amount().toPlainString());
        assertEquals("60000", m.cashIn().amount().toPlainString());
        assertEquals("15000", m.cashOut().amount().toPlainString());
    }

    @Test
    void rejectsFractionalCurrencyMixedCurrencyAndDuplicateAccounts() {
        assertThrows(
                ReportException.class,
                () ->
                        new FinancialMetricsCalculator()
                                .calculate(snapshot(List.of(figures("0.1", "0", "0")))));
        var a = figures("1", "0", "0");
        assertThrows(
                ReportException.class,
                () -> new FinancialMetricsCalculator().calculate(snapshot(List.of(a, a))));
        assertThrows(
                ReportException.class,
                () ->
                        new FinancialMetricsCalculator()
                                .calculate(
                                        snapshot(
                                                List.of(
                                                        new AccountFigures(
                                                                a.accountId(),
                                                                a.balance(),
                                                                a.cashIn(),
                                                                a.cashOut(),
                                                                "USD",
                                                                1)))));
    }

    @Test
    void rejectsEmptyIncompleteAndMismatchedCutoff() {
        assertThrows(
                ReportException.class,
                () -> new FinancialMetricsCalculator().calculate(snapshot(List.of())));
        assertThrows(
                ReportException.class,
                () ->
                        new SnapshotConsistencyPolicy()
                                .validateSnapshot(
                                        snapshot(List.of(figures("0", "0", "0"))),
                                        AS_OF.plusSeconds(1)));
    }

    static ReportDraft draft(
            FinancialMetrics m, String total, String currency, Instant asOf, String prose) {
        return new ReportDraft(
                total,
                m.cashIn().amount().toPlainString(),
                m.cashOut().amount().toPlainString(),
                currency,
                asOf,
                m.calculationVersion(),
                prose,
                "TEST_DOUBLE");
    }

    @Test
    void rejectsHallucinatedAmountsDatesCurrenciesAndFreeTextNumbers() {
        var m =
                new FinancialMetricsCalculator()
                        .calculate(snapshot(List.of(figures("100", "20", "30"))));
        var p = new ReportNumberValidationPolicy();
        p.validate(m, draft(m, "100", "KRW", AS_OF, "계좌의 흐름을 확인하세요."));
        for (var d :
                List.of(
                        draft(m, "101", "KRW", AS_OF, "설명"),
                        draft(m, "100.0", "KRW", AS_OF, "설명"),
                        draft(m, "100", "USD", AS_OF, "설명"),
                        draft(m, "100", "KRW", AS_OF.plusSeconds(1), "설명"),
                        draft(m, "100", "KRW", AS_OF, "총액은 999입니다"),
                        draft(m, "100", "KRW", AS_OF, "[PII_NAME_1]"),
                        draft(m, "100", "KRW", AS_OF, "원화")))
            assertThrows(ReportException.class, () -> p.validate(m, d));
    }

    @Test
    void unconfiguredInferenceCannotReturnPreparationAsDraft() {
        var m =
                new FinancialMetricsCalculator()
                        .calculate(snapshot(List.of(figures("0", "0", "0"))));
        assertEquals(
                WealthReportErrorCode.AI_NOT_CONFIGURED,
                assertThrows(
                                ReportException.class,
                                () -> new AiReportDraftRestSkeleton().requestDraft(m))
                        .code());
    }
}
