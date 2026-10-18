package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.application.readmodel.ReportSnapshot;

import java.time.Instant;
import java.util.UUID;

/** 고정 기준 시점의 내부 스냅샷 조회. 숫자 계산은 Java의 순수 업무 로직에서 처리한다. */
public interface ReportSnapshotPort {
    ReportSnapshot loadAt(UUID userId, Instant asOf);
}
