package com.shinhan.pg.aml.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.aml.adapter.in.batch.RetryAmlReportBatchSkeleton;
import com.shinhan.pg.aml.adapter.in.kafka.EvaluateTransactionConsumerSkeleton;
import com.shinhan.pg.aml.adapter.out.fep.FepKofiuRestAdapter;
import com.shinhan.pg.aml.adapter.out.fep.FepKofiuRestSkeleton;
import com.shinhan.pg.aml.adapter.out.ledger.AmlFactsRestAdapter;
import com.shinhan.pg.aml.adapter.out.persistence.AmlWorkflowAdapter;
import com.shinhan.pg.aml.adapter.out.persistence.outbox.AmlOutboxStore;
import com.shinhan.pg.aml.application.service.AmlApplicationService;
import com.shinhan.pg.aml.domain.model.AmlRuleSet;
import com.shinhan.pg.common.outbox.OutboxStore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class AmlSkeletonConfiguration {
    @Bean
    public AmlApplicationService amlApplicationService(
            DataSource ds, OutboxStore outbox, Environment e) {
        boolean enabled = e.getProperty("pg.aml.enabled", Boolean.class, false);
        if (!enabled
                && (e.getProperty("pg.aml.events-enabled", Boolean.class, false)
                        || e.getProperty("pg.aml.dispatch-enabled", Boolean.class, false)
                        || e.getProperty("pg.aml.automatic-query-enabled", Boolean.class, false)))
            throw new IllegalStateException(
                    "AML activity requires explicit development activation");
        AmlRuleSet policy = null;
        var json = new ObjectMapper().findAndRegisterModules();
        if (enabled) {
            var profiles = java.util.Set.of(e.getActiveProfiles());
            if (!profiles.contains("aml-test")
                    || profiles.contains("aws")
                    || profiles.contains("prod")
                    || profiles.contains("production"))
                throw new IllegalStateException(
                        "MVP6 requires explicit development aml-test profile");
            if ("development-v1".equals(e.getProperty("pg.aml.policy", "")))
                policy = com.shinhan.pg.aml.domain.policy.AmlDevelopmentPolicy.versionOne();
            String raw = e.getProperty("pg.aml.rule-set-json", "");
            if (!raw.isBlank())
                try {
                    policy = json.readValue(raw, AmlRuleSet.class);
                } catch (Exception ex) {
                    throw new IllegalStateException("Invalid AML rule configuration");
                }
        }
        return new AmlApplicationService(
                enabled ? new FepKofiuRestAdapter(e) : new FepKofiuRestSkeleton(),
                new AmlWorkflowAdapter(
                        ds,
                        json,
                        outbox,
                        e.getProperty("pg.aml.maximum-queries", Integer.class, 12),
                        e.getProperty("pg.aml.query-interval-seconds", Long.class, 10L)),
                enabled ? new AmlFactsRestAdapter(e) : null,
                policy,
                enabled,
                e.getProperty("pg.aml.dispatch-enabled", Boolean.class, false),
                e.getProperty("pg.aml.automatic-query-enabled", Boolean.class, false),
                e.getProperty("pg.aml.retransmit-enabled", Boolean.class, false));
    }

    @Bean
    public EvaluateTransactionConsumerSkeleton evaluateTransactionKafka(
            AmlApplicationService service) {
        return new EvaluateTransactionConsumerSkeleton(service);
    }

    @Bean
    public RetryAmlReportBatchSkeleton retryAmlReportBatch(AmlApplicationService service) {
        return new RetryAmlReportBatchSkeleton(service);
    }

    @Bean
    public OutboxStore outboxStore(DataSource ds) {
        return new AmlOutboxStore(new JdbcTemplate(ds));
    }
}
