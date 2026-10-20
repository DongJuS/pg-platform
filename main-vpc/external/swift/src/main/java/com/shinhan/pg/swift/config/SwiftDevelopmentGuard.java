package com.shinhan.pg.swift.config;

import com.shinhan.pg.common.swiftmessages.PrivatePayload;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Enabling a development contract must never silently activate a production/bank integration. */
@Component
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class SwiftDevelopmentGuard {
    public SwiftDevelopmentGuard(Environment env) {
        var profiles = java.util.Set.of(env.getActiveProfiles());
        if (profiles.contains("aws")
                || profiles.contains("prod")
                || profiles.contains("production")
                || !profiles.contains("swift-test"))
            throw new IllegalStateException(
                    "MVP5 is restricted to an explicit swift-test development profile");
        new PrivatePayload(env.getProperty("pg.swift.payload-key", ""));
    }

    /** Force time-based private-log rotation even when no new remittance arrives. */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
    public void rotatePrivateLog() {
        org.slf4j.LoggerFactory.getLogger("swift.private").info("retention-sweep");
    }
}
