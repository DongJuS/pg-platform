package com.shinhan.pg.aml.config;

import com.shinhan.pg.aml.application.service.AmlApplicationService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@Profile("aml-test")
@EnableScheduling
@ConditionalOnProperty(name = "pg.aml.enabled", havingValue = "true")
public class AmlRecoveryConfiguration {
    private final AmlApplicationService service;

    public AmlRecoveryConfiguration(AmlApplicationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${pg.aml.recovery-interval-ms:5000}")
    public void recover() {
        try {
            service.recover();
        } catch (RuntimeException unavailable) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("AML recovery deferred; durable state retained");
        }
    }
}
