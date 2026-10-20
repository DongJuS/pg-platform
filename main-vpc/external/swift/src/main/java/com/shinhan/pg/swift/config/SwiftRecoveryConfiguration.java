package com.shinhan.pg.swift.config;

import com.shinhan.pg.swift.application.service.SwiftApplicationService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
@EnableScheduling
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public class SwiftRecoveryConfiguration {
    private final SwiftApplicationService service;

    public SwiftRecoveryConfiguration(SwiftApplicationService service) {
        this.service = service;
    }
}
