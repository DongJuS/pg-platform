package com.shinhan.pg.aigateway.application.port.out;

import com.shinhan.pg.aigateway.application.command.RelayLlmCommand;
import com.shinhan.pg.aigateway.application.result.RelayLlmResult;

/** AI 전용 NAT 경로의 외부 LLM HTTPS client 위치. endpoint 허용 정책·시간 예산을 적용한 뒤 호출하도록 구현한다. */
public interface LlmRelayPort {
    RelayLlmResult relay(RelayLlmCommand command);
}
