-- [CMN-F01] 서비스 로컬 Outbox 테이블. MariaDB용.
-- 공통 JdbcOutboxStore 계약과 동일한 컬럼 구성을 유지한다.

create table outbox_events (
    id varchar(36) primary key,
    event_id varchar(36) not null,
    destination varchar(255) not null,
    routing_key varchar(255),
    payload blob not null,
    claim_token varchar(36),
    claimed_at timestamp(6) null,
    published_at timestamp(6) null,
    attempts int not null default 0,
    created_at timestamp(6) not null default now()
);

create index idx_outbox_events_claim on outbox_events (published_at, claimed_at, created_at);
