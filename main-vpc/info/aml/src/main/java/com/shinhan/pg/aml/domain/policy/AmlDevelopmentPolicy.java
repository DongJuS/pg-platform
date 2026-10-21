package com.shinhan.pg.aml.domain.policy;

import com.shinhan.pg.aml.domain.model.AmlRuleSet;
import com.shinhan.pg.aml.domain.model.DetectionRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** User-confirmed 2026-10-21 development simulation. F06/cash/CTR compliance is out of scope. */
public final class AmlDevelopmentPolicy {
    private AmlDevelopmentPolicy() {}

    public static AmlRuleSet versionOne() {
        String v = "development-v1";
        var kinds = Set.of("DOMESTIC_TRANSFER", "PAYMENT_APPROVAL", "OVERSEAS_TRANSFER");
        var rules = new java.util.ArrayList<DetectionRule>();
        for (String currency : List.of("KRW", "USD")) {
            String single = "KRW".equals(currency) ? "5000000" : "5000";
            String total = "KRW".equals(currency) ? "20000000" : "20000";
            rules.add(
                    new DetectionRule(
                            "SINGLE_" + currency,
                            v,
                            "Development single-transaction amount",
                            60,
                            currency,
                            kinds,
                            new BigDecimal(single),
                            1,
                            86400,
                            "SINGLE_AMOUNT"));
            rules.add(
                    new DetectionRule(
                            "FREQUENCY_" + currency,
                            v,
                            "Development one-hour transaction count",
                            40,
                            currency,
                            kinds,
                            BigDecimal.ZERO,
                            5,
                            3600,
                            "WINDOW_COUNT"));
            rules.add(
                    new DetectionRule(
                            "TOTAL_" + currency,
                            v,
                            "Development 24-hour gross turnover",
                            70,
                            currency,
                            kinds,
                            new BigDecimal(total),
                            1,
                            86400,
                            "WINDOW_AMOUNT"));
        }
        return new AmlRuleSet(v, 70, rules);
    }
}
