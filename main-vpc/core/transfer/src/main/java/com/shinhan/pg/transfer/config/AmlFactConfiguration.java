package com.shinhan.pg.transfer.config;

import com.shinhan.pg.transfer.adapter.out.persistence.AmlFactReadAdapter;
import com.shinhan.pg.transfer.application.service.AmlFactReadService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("aml-test")
@ConditionalOnProperty(name = "pg.aml.enabled", havingValue = "true")
public class AmlFactConfiguration {
    @Bean
    AmlFactReadService amlFactReadService(
            javax.sql.DataSource ds, org.springframework.core.env.Environment env) {
        var profiles = java.util.Set.of(env.getActiveProfiles());
        if (profiles.contains("aws")
                || profiles.contains("prod")
                || profiles.contains("production"))
            throw new IllegalStateException("AML source reads are development only");
        return new AmlFactReadService(new AmlFactReadAdapter(new JdbcTemplate(ds)));
    }
}
