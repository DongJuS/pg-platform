package com.shinhan.pg.aigateway.application.port.out;

import com.shinhan.pg.aigateway.application.command.RelayLlmCommand;

/** 민감 원문을 제외한 중계 감사 기록의 저장 위치. */
public interface AiAuditWritePort {
    void record(RelayLlmCommand command);
}
