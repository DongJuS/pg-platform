package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.application.command.ReportStageDraft;

/** 동기 HTTP 응답과 별개로 생성 단계·고정 기준 시점·복구 상태를 PostgreSQL에 기록할 위치. */
public interface ReportProgressPort {
    void recordStage(ReportStageDraft stage);
}
