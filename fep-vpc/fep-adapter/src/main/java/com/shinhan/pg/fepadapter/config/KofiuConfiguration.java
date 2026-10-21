package com.shinhan.pg.fepadapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.fepadapter.adapter.out.institution.kofiu.KofiuMockRestAdapter;
import com.shinhan.pg.fepadapter.adapter.out.persistence.KofiuReceiptAdapter;
import com.shinhan.pg.fepadapter.application.service.KofiuFepService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@org.springframework.scheduling.annotation.EnableScheduling
@Configuration(proxyBeanMethods = false)
@Profile("aml-test")
@ConditionalOnProperty(name = "pg.aml.enabled", havingValue = "true")
public class KofiuConfiguration {
    @Bean
    public Object kofiuRetention(KofiuFepService service) {
        return new Object() {
            @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
            public void purge() {
                service.purgeExpired();
            }
        };
    }

    @Bean
    KofiuFepService kofiuFepService(javax.sql.DataSource ds, ObjectMapper json, Environment e) {
        var profiles = java.util.Set.of(e.getActiveProfiles());
        if (profiles.contains("aws")
                || profiles.contains("prod")
                || profiles.contains("production"))
            throw new IllegalStateException("MVP6 development profile only");
        return new KofiuFepService(
                new KofiuReceiptAdapter(ds, json),
                new KofiuMockRestAdapter(e),
                e.getProperty("pg.aml.retransmit-enabled", Boolean.class, false),
                e.getProperty("pg.aml.maximum-sends", Integer.class, 3),
                e.getProperty("pg.aml.minimum-retransmit-seconds", Long.class, 30L));
    }
}
