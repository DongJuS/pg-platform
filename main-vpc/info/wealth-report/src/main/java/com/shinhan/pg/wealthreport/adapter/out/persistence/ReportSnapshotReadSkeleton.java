package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.application.readmodel.ReportSnapshot;

import java.time.Instant;
import java.util.UUID;

/** 고정 기준 시점의 내부 스냅샷 조회. 숫자 계산은 Java의 순수 업무 로직에서 처리한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class ReportSnapshotReadSkeleton implements ReportSnapshotPort {
    @Override
    public ReportSnapshot loadAt(UUID userId, Instant asOf) {
        throw new UnsupportedOperationException("SKELETON wealth-report: loadAt");
    }
}
