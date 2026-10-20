package com.shinhan.pg.aigateway;

import com.shinhan.pg.aigateway.application.port.in.PrepareAiRequestUseCase;
import com.shinhan.pg.aigateway.config.AiGatewaySkeletonConfiguration;
import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void preparationRegistersNoExternalLlmClientOrInferenceUseCase() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("skeleton");
            context.registerBean(javax.sql.DataSource.class,
                    () -> org.mockito.Mockito.mock(javax.sql.DataSource.class));
            context.register(AiGatewaySkeletonConfiguration.class);
            context.refresh();
            org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(
                    com.shinhan.pg.aigateway.application.port.out.LlmRelayPort.class).isEmpty());
            org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(
                    com.shinhan.pg.aigateway.application.port.in.RelayLlmUseCase.class).isEmpty());
        }
    }
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                AiGatewaySkeletonConfiguration.class, PrepareAiRequestUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assemblesWithInfrastructure(
                AiGatewaySkeletonConfiguration.class, PrepareAiRequestUseCase.class);
    }
}
