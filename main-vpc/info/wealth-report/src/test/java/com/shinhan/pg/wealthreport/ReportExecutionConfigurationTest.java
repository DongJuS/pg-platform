package com.shinhan.pg.wealthreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.wealthreport.config.ReportExecutionProperties;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.time.Duration;
import java.util.Map;

class ReportExecutionConfigurationTest {
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.context.properties.EnableConfigurationProperties(
            ReportExecutionProperties.class)
    static class PropertiesOnly {}

    @Test
    void skeletonBindsOperatorOverridesForSynchronousGeneration() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("skeleton");
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(
                            new MapPropertySource(
                                    "test",
                                    Map.of(
                                            "platform.report.execution.total-timeout",
                                            "80s",
                                            "platform.report.execution.maximum-concurrent-generations",
                                            "3")));
            context.register(PropertiesOnly.class);
            context.refresh();
            var properties = context.getBean(ReportExecutionProperties.class);
            assertEquals(Duration.ofSeconds(80), properties.totalTimeout());
            assertEquals(3, properties.maximumConcurrentGenerations());
        }
    }

    @Test
    void downstreamBudgetsMustFitInsideTheWholeRequestBudget() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ReportExecutionProperties(
                                Duration.ofSeconds(60),
                                Duration.ofSeconds(45),
                                Duration.ofSeconds(15),
                                Duration.ofSeconds(15),
                                2));
    }
}
