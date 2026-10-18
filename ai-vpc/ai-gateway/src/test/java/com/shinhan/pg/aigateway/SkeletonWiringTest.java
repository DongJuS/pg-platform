package com.shinhan.pg.aigateway;

import com.shinhan.pg.aigateway.application.port.in.RelayLlmUseCase;
import com.shinhan.pg.aigateway.config.AiGatewaySkeletonConfiguration;
import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                AiGatewaySkeletonConfiguration.class, RelayLlmUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assembles(
                AiGatewaySkeletonConfiguration.class, RelayLlmUseCase.class);
    }
}
