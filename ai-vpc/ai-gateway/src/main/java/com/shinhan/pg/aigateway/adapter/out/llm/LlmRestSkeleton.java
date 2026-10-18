package com.shinhan.pg.aigateway.adapter.out.llm;

import com.shinhan.pg.aigateway.application.command.RelayLlmCommand;
import com.shinhan.pg.aigateway.application.port.out.LlmRelayPort;
import com.shinhan.pg.aigateway.application.result.RelayLlmResult;

import java.util.Objects;

/** AI 전용 NAT로 통신할 client 위치. endpoint 검사·redirect 차단·시간 예산 적용 후 호출하도록 구현한다. */
public final class LlmRestSkeleton implements LlmRelayPort {
    private final LlmEndpointPolicy endpointPolicy;

    public LlmRestSkeleton(LlmEndpointPolicy endpointPolicy) {
        this.endpointPolicy = Objects.requireNonNull(endpointPolicy);
    }

    @Override
    public RelayLlmResult relay(RelayLlmCommand command) {
        // TODO: endpointPolicy.requireAllowed(endpoint), redirect 차단, timeout을 적용한 실제 client 구현.
        throw new UnsupportedOperationException("SKELETON ai-gateway: relay");
    }
}
