package com.shinhan.pg.aigateway.adapter.out.inspection;

import com.shinhan.pg.aigateway.application.command.RelayLlmCommand;
import com.shinhan.pg.aigateway.application.port.out.PiiInspectionPort;

/** 입력/출력 개인정보 탐지·정책 차단 위치. 실제 탐지 모델과 정책은 미구현이다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class PiiInspectionSkeleton implements PiiInspectionPort {
    @Override
    public void inspect(RelayLlmCommand command) {
        throw new UnsupportedOperationException("SKELETON ai-gateway: inspect");
    }
}
