package com.shinhan.pg.wealthreport.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class ReportJsonConfiguration {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictReportJson() {
        return b ->
                b.postConfigurer(
                        m -> {
                            m.enable(
                                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
                            m.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
                            m.getFactory()
                                    .setStreamReadConstraints(
                                            StreamReadConstraints.builder()
                                                    .maxNestingDepth(16)
                                                    .maxStringLength(16384)
                                                    .maxNumberLength(32)
                                                    .build());
                        });
    }
}
