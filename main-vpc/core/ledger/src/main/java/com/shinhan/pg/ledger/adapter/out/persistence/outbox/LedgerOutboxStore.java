package com.shinhan.pg.ledger.adapter.out.persistence.outbox;

import com.shinhan.pg.common.outbox.JdbcOutboxStore;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <pre>
 * [CMN-F01] 서비스 로컬 Outbox 저장. 공통 JdbcOutboxStore를 상속하며,
 * 업무 변경과 append를 같은 로컬 트랜잭션으로 묶어 호출한다.
 * outbox_events 테이블은 V2__outbox_events.sql 마이그레이션이 만든다.
 * relay는 outbox.enabled=true와 KafkaTemplate bean이 모두 있을 때만 동작한다.
 * </pre>
 */
public final class LedgerOutboxStore extends JdbcOutboxStore {
    public LedgerOutboxStore(JdbcTemplate jdbc) {
        super(jdbc);
    }
}
