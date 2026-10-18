package com.shinhan.pg.aigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/** 허용 hostname 미설정은 기본 차단. 실제 LLM client는 아직 등록하지 않는다. */
@ConfigurationProperties("platform.llm")
public record LlmEndpointProperties(Set<String> allowedHosts) {
    public LlmEndpointProperties {
        allowedHosts = allowedHosts == null ? Set.of() : Set.copyOf(allowedHosts);
    }
}
