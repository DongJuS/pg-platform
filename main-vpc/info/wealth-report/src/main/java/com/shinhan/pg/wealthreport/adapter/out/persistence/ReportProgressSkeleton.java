package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.wealthreport.application.command.ReportStageDraft;
import com.shinhan.pg.wealthreport.application.port.out.ReportProgressPort;

/**
 * 동기 HTTP 응답과 별개로 생성 단계·고정 기준 시점·복구 상태를 PostgreSQL에 기록할 위치. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다.
 */
public final class ReportProgressSkeleton implements ReportProgressPort {
    @Override
    public void recordStage(ReportStageDraft stage) {
        throw new UnsupportedOperationException("SKELETON wealth-report: recordStage");
    }
}
