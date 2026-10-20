package com.shinhan.pg.wealthreport.domain.policy;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;
import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Proposed deposit-only policy, gated by explicit configuration until user confirmation. Gross
 * per-account flows include internal transfers; no FX, net-worth or returns are inferred. Source
 * interval is [from,to); closing balance includes postings <= asOf.
 */
public final class FinancialMetricsCalculator {
    public static final String VERSION = "krw-deposit-gross-v1";

    public FinancialMetrics calculate(AssetSnapshot s) {
        new SnapshotConsistencyPolicy().validateSnapshot(s, s.asOf());
        try {
            BigDecimal balance = BigDecimal.ZERO, in = BigDecimal.ZERO, out = BigDecimal.ZERO;
            for (var a : s.accounts()) {
                balance = balance.add(a.balance().setScale(0, RoundingMode.UNNECESSARY));
                in = in.add(a.cashIn().setScale(0, RoundingMode.UNNECESSARY));
                out = out.add(a.cashOut().setScale(0, RoundingMode.UNNECESSARY));
            }
            return new FinancialMetrics(
                    Money.of(balance, java.util.Currency.getInstance("KRW")),
                    Money.of(in, java.util.Currency.getInstance("KRW")),
                    Money.of(out, java.util.Currency.getInstance("KRW")),
                    s.asOf(),
                    VERSION);
        } catch (ArithmeticException | IllegalArgumentException e) {
            throw new ReportException(WealthReportErrorCode.SNAPSHOT_INCOMPLETE);
        }
    }
}
