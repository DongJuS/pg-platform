package com.shinhan.pg.aigateway.application.port.out;

import com.shinhan.pg.aigateway.application.command.RelayLlmCommand;

/** 입력/출력 개인정보 탐지·정책 차단 위치. 실제 탐지 모델과 정책은 미구현이다. */
public interface PiiInspectionPort {
    void inspect(RelayLlmCommand command);
}
