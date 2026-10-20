package com.shinhan.pg.aigateway.config;

import com.shinhan.pg.aigateway.adapter.out.inspection.RuleBasedPiiInspectionAdapter;
import com.shinhan.pg.aigateway.adapter.out.persistence.AiRequestAuditPersistenceAdapter;
import com.shinhan.pg.aigateway.application.port.out.PiiInspectionPort;
import com.shinhan.pg.aigateway.application.service.AiGatewayApplicationService;
import com.shinhan.pg.aigateway.domain.policy.AiRequestInspectionPolicy;
import com.shinhan.pg.aigateway.domain.policy.PiiMaskingPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Explicit local implementation profile, matching MVP1/2. No inference/LLM bean is registered. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class AiGatewaySkeletonConfiguration {
    @Bean public PiiInspectionPort piiInspectionPort() { return new RuleBasedPiiInspectionAdapter(); }
    @Bean public AiRequestAuditPersistenceAdapter aiRequestAuditRepository(DataSource source) {
        return new AiRequestAuditPersistenceAdapter(source);
    }
    @Bean public AiGatewayApplicationService aiGatewayApplicationService(PiiInspectionPort inspection,
            AiRequestAuditPersistenceAdapter audits, Environment env) {
        Set<String> allowed = Arrays.stream(env.getProperty("pg.ai.preparation.allowed-callers", "wealth-report").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
        if (allowed.stream().anyMatch(s -> !s.matches("[a-z][a-z0-9-]{0,47}")))
            throw new IllegalArgumentException("Invalid AI caller configuration");
        return new AiGatewayApplicationService(new PiiMaskingPolicy(), inspection,
                new AiRequestInspectionPolicy(), audits, allowed, Clock.systemUTC());
    }
}
