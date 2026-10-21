package com.shinhan.pg.aml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.aml.domain.model.FinancialFact;
import com.shinhan.pg.aml.domain.policy.AmlDevelopmentPolicy;
import com.shinhan.pg.aml.domain.policy.AmlReportingDecisionPolicy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class AmlConfirmedPolicyTest {
    final UUID user = UUID.randomUUID(), source = UUID.randomUUID(), target = UUID.randomUUID();
    final Instant at = Instant.parse("2026-10-21T00:00:00Z");

    FinancialFact fact(String kind, String currency, String amount, Instant time) {
        var id = UUID.randomUUID();
        return new FinancialFact(
                id, id, kind, user, source, target, new BigDecimal(amount), currency, time, null);
    }

    @Test
    void singleAmountAtBoundaryDetectsButDoesNotAloneReport() {
        var p = AmlDevelopmentPolicy.versionOne();
        var rule = new AmlReportingDecisionPolicy();
        for (String kind : List.of("DOMESTIC_TRANSFER", "PAYMENT_APPROVAL", "OVERSEAS_TRANSFER")) {
            for (String currency : List.of("KRW", "USD")) {
                var f = fact(kind, currency, "KRW".equals(currency) ? "5000000" : "5000", at);
                var a = rule.assess(f, List.of(f), p);
                assertEquals(60, a.riskScore());
                assertFalse(a.reportRequired());
            }
        }
    }

    @Test
    void twoBelowSingleThresholdDoNotIncorrectlyTriggerSingleAmountRule() {
        var x = fact("DOMESTIC_TRANSFER", "KRW", "3000000", at.minusSeconds(1));
        var y = fact("DOMESTIC_TRANSFER", "KRW", "3000000", at);
        assertEquals(
                0,
                new AmlReportingDecisionPolicy()
                        .assess(y, List.of(x, y), AmlDevelopmentPolicy.versionOne())
                        .riskScore());
    }

    @Test
    void fiveWithinHourAndLargeSingleCombineToReport() {
        var fs = new ArrayList<FinancialFact>();
        for (int i = 0; i < 5; i++)
            fs.add(
                    fact(
                            "DOMESTIC_TRANSFER",
                            "KRW",
                            i == 4 ? "5000000" : "1",
                            at.minusSeconds(4 - i)));
        var a =
                new AmlReportingDecisionPolicy()
                        .assess(fs.getLast(), fs, AmlDevelopmentPolicy.versionOne());
        assertEquals(100, a.riskScore());
        assertTrue(a.reportRequired());
    }

    @Test
    void exact24HourTotalReportsWithoutCurrencyConversion() {
        for (String currency : List.of("KRW", "USD")) {
            var fs = new ArrayList<FinancialFact>();
            for (int i = 0; i < 4; i++)
                fs.add(
                        fact(
                                "OVERSEAS_TRANSFER",
                                currency,
                                "KRW".equals(currency) ? "5000000" : "5000",
                                at.minusSeconds((3 - i) * 7200L)));
            var a =
                    new AmlReportingDecisionPolicy()
                            .assess(fs.getLast(), fs, AmlDevelopmentPolicy.versionOne());
            assertEquals(130, a.riskScore());
            assertTrue(a.reportRequired());
        }
    }

    @Test
    void countAtHourBoundaryIncludesFiveButOutsideDoesNot() {
        var fs = new ArrayList<FinancialFact>();
        for (int i = 0; i < 5; i++)
            fs.add(
                    fact(
                            "PAYMENT_APPROVAL",
                            "KRW",
                            "1",
                            i == 0 ? at.minusSeconds(3600) : at.minusSeconds(4 - i)));
        var evaluator = new AmlReportingDecisionPolicy();
        assertEquals(
                40,
                evaluator.assess(fs.getLast(), fs, AmlDevelopmentPolicy.versionOne()).riskScore());
        fs.set(0, fact("PAYMENT_APPROVAL", "KRW", "1", at.minusSeconds(3601)));
        assertEquals(
                0,
                evaluator.assess(fs.getLast(), fs, AmlDevelopmentPolicy.versionOne()).riskScore());
    }
}
