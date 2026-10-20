package com.shinhan.pg.fepadapter.config;

import com.shinhan.pg.fepadapter.adapter.out.institutions.swift.SwiftGatewayRestAdapter;
import com.shinhan.pg.fepadapter.adapter.out.persistence.SwiftReceiptJdbcAdapter;
import com.shinhan.pg.fepadapter.application.service.SwiftFepService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
@EnableScheduling
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public class SwiftFepConfiguration {
    @Bean
    public SwiftFepService swiftFepService(DataSource ds, Environment env) {
        return new SwiftFepService(
                new SwiftReceiptJdbcAdapter(ds, env.getProperty("pg.swift.payload-key", "")),
                new SwiftGatewayRestAdapter(env),
                env.getProperty("pg.swift.retransmit-enabled", Boolean.class, false),
                env.getProperty("pg.swift.max-send-attempts", Integer.class, 3),
                env.getProperty("pg.swift.minimum-retransmit-seconds", Long.class, 30L));
    }

    @Bean
    public SwiftRetention swiftRetention(SwiftFepService service) {
        return new SwiftRetention(service);
    }

    public static final class SwiftRetention {
        private final SwiftFepService service;

        public SwiftRetention(SwiftFepService service) {
            this.service = service;
        }

        @Scheduled(fixedDelayString = "${pg.swift.retention-interval-ms:60000}")
        public void purge() {
            service.purge();
        }
    }
}
