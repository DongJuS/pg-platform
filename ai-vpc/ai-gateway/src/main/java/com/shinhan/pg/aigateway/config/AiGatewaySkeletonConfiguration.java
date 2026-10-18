package com.shinhan.pg.aigateway.config;

import com.shinhan.pg.aigateway.adapter.out.inspection.PiiInspectionSkeleton;
import com.shinhan.pg.aigateway.adapter.out.llm.LlmRestSkeleton;
import com.shinhan.pg.aigateway.adapter.out.persistence.AiAuditWriteSkeleton;
import com.shinhan.pg.aigateway.adapter.out.persistence.AiRequestAuditPersistenceAdapter;
import com.shinhan.pg.aigateway.application.port.out.AiAuditWritePort;
import com.shinhan.pg.aigateway.application.port.out.AiRequestAuditRepository;
import com.shinhan.pg.aigateway.application.port.out.LlmRelayPort;
import com.shinhan.pg.aigateway.application.port.out.PiiInspectionPort;
import com.shinhan.pg.aigateway.application.service.AiGatewayApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 개발용 의존성 조립. 기본/local/aws에는 업무 스켈레톤 bean을 등록하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
@org.springframework.boot.context.properties.EnableConfigurationProperties(
        LlmEndpointProperties.class)
public class AiGatewaySkeletonConfiguration {
    @Bean
    public PiiInspectionPort piiInspectionPort() {
        return new PiiInspectionSkeleton();
    }

    @Bean
    public LlmRelayPort llmRelayPort(LlmEndpointProperties properties) {
        return new LlmRestSkeleton(
                new com.shinhan.pg.aigateway.adapter.out.llm.LlmEndpointPolicy(
                        properties.allowedHosts()));
    }

    @Bean
    public AiAuditWritePort aiAuditWritePort() {
        return new AiAuditWriteSkeleton();
    }

    @Bean
    public AiRequestAuditRepository aiRequestAuditRepository() {
        return new AiRequestAuditPersistenceAdapter();
    }

    @Bean
    public AiGatewayApplicationService aiGatewayApplicationService(
            PiiInspectionPort piiInspectionPort,
            LlmRelayPort llmRelayPort,
            AiAuditWritePort aiAuditWritePort,
            AiRequestAuditRepository aiRequestAuditRepository) {
        return new AiGatewayApplicationService(
                piiInspectionPort, llmRelayPort, aiAuditWritePort, aiRequestAuditRepository);
    }
}
