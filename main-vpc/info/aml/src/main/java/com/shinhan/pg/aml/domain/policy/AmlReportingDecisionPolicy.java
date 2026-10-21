package com.shinhan.pg.aml.domain.policy;

import com.shinhan.pg.aml.domain.model.AmlRuleSet;
import com.shinhan.pg.aml.domain.model.CaseAssessment;
import com.shinhan.pg.aml.domain.model.FinancialFact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic event-time inclusive [anchor-window,anchor] windows. Currencies are never combined.
 */
public final class AmlReportingDecisionPolicy {
    public CaseAssessment assess(
            FinancialFact anchor, List<FinancialFact> facts, AmlRuleSet policy) {
        int score = 0, count = 0;
        BigDecimal total = BigDecimal.ZERO;
        var reasons = new ArrayList<String>();
        for (var rule : policy.rules()) {
            if (!anchor.currency().equals(rule.currency())
                    || !rule.kinds().contains(anchor.kind())
                    || anchor.reversal()) continue;
            var eligible =
                    facts.stream()
                            .filter(
                                    f ->
                                            (!"SINGLE_AMOUNT".equals(rule.condition())
                                                            || anchor.transactionId()
                                                                    .equals(f.transactionId()))
                                                    && !f.reversal()
                                                    && anchor.customerId().equals(f.customerId())
                                                    && rule.currency().equals(f.currency())
                                                    && rule.kinds().contains(f.kind())
                                                    && !f.occurredAt()
                                                            .isBefore(
                                                                    anchor.occurredAt()
                                                                            .minusSeconds(
                                                                                    rule
                                                                                            .windowSeconds()))
                                                    && !f.occurredAt().isAfter(anchor.occurredAt()))
                            .toList();
            BigDecimal amount =
                    eligible.stream()
                            .map(FinancialFact::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (eligible.size() >= rule.minimumCount()
                    && amount.compareTo(rule.minimumAmount()) >= 0) {
                score += rule.score();
                reasons.add(rule.code() + "=" + rule.score());
                if (amount.compareTo(total) > 0) total = amount;
                count = Math.max(count, eligible.size());
            }
        }
        return new CaseAssessment(
                score,
                score >= policy.reportThreshold(),
                total,
                count,
                anchor.occurredAt().minusSeconds(policy.maximumWindow()),
                anchor.occurredAt(),
                reasons);
    }
}
