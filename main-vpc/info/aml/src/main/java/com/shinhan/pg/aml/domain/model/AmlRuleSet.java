package com.shinhan.pg.aml.domain.model;

import com.shinhan.pg.common.amlcontracts.AmlProblem;

import java.util.List;

/** Explicit caller-configured policy. No default policy or automatic activation. */
public record AmlRuleSet(String version, int reportThreshold, List<DetectionRule> rules) {
    public AmlRuleSet {
        AmlProblem.require(
                version != null
                        && version.matches("[A-Za-z0-9_.-]{1,64}")
                        && reportThreshold > 0
                        && reportThreshold <= 10000
                        && rules != null
                        && !rules.isEmpty()
                        && rules.size() <= 32);
        rules = List.copyOf(rules);
        AmlProblem.require(
                rules.stream().allMatch(r -> version.equals(r.version()))
                        && rules.stream().map(DetectionRule::code).distinct().count()
                                == rules.size()
                        && rules.stream().mapToInt(DetectionRule::score).sum() <= 10000);
    }

    public long maximumWindow() {
        return rules.stream().mapToLong(DetectionRule::windowSeconds).max().orElseThrow();
    }
}
