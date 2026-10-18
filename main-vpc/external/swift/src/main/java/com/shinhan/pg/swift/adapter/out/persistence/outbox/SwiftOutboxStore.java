package com.shinhan.pg.swift.adapter.out.persistence.outbox;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.common.outbox.OutboxStore;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * <pre>
 * [CMN-F01] 서비스 로컬 Outbox 저장 구현 자리.
 * TODO: 해당 서비스 DB의 같은 connection/transaction으로 업무 변경과 append를 묶는다.
 * TODO: claim은 persisted lease + claimToken 비교로 보호하고 commit된 행만 가져온다.
 * TODO: stable eventId·aggregate key·schemaVersion을 보존한다. broker ack 후 published를 반영한다.
 * 현재 SQL·테이블·Spring bean 등록 없음. outbox.enabled의 실제 store로 사용하기 전에 DB 통합 검증한다.
 * </pre>
 */
public final class SwiftOutboxStore implements OutboxStore {
    @Override
    public void append(OutboxRecord record) {
        throw new FeatureNotImplementedException("CMN-F01");
    }

    @Override
    public List<OutboxRecord> claim(int limit, UUID claimToken, Duration lease) {
        throw new FeatureNotImplementedException("CMN-F01");
    }

    @Override
    public void published(UUID eventId, UUID claimToken) {
        throw new FeatureNotImplementedException("CMN-F01");
    }

    @Override
    public void retry(UUID eventId, UUID claimToken) {
        throw new FeatureNotImplementedException("CMN-F01");
    }
}
