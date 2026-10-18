package com.shinhan.pg.aigateway.adapter.out.persistence;

import com.shinhan.pg.aigateway.application.command.RelayLlmCommand;
import com.shinhan.pg.aigateway.application.port.out.AiAuditWritePort;

/** 민감 원문을 제외한 중계 감사 기록의 저장 위치. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class AiAuditWriteSkeleton implements AiAuditWritePort {
    @Override
    public void record(RelayLlmCommand command) {
        throw new UnsupportedOperationException("SKELETON ai-gateway: record");
    }
}
