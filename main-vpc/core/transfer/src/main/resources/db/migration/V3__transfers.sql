-- [MVP1-F15,MVP1-F16,MVP1-F17,MVP1-F20] transfer 업무 테이블. PostgreSQL(transfer_db)용.
-- UUID는 서비스 간 공통 계약으로 varchar(36)에 저장한다(V2__outbox_events.sql·auth V2 패턴 준용).
-- ADR 0001: 이체 멱등키는 DB unique(actor_user_id, idempotency_key) 제약으로 강제한다(Redis 아님).
-- otpToken은 인증 문맥 값이며 이 테이블에 저장하지 않는다(검증 정책 미정 — WO-A6 §5-④).

create table transfers (
    id varchar(36) primary key,
    actor_user_id varchar(36) not null,
    from_account_id varchar(36) not null,
    to_account_id varchar(36) not null,
    amount numeric(19, 4) not null,
    currency varchar(3) not null,
    idempotency_key varchar(255) not null,
    request_hash varchar(64) not null,
    transaction_id varchar(36) not null unique,
    status varchar(16) not null,
    created_at timestamptz not null default now(),
    constraint uq_transfers_actor_idempotency unique (actor_user_id, idempotency_key),
    constraint ck_transfers_amount_positive check (amount > 0),
    constraint ck_transfers_status check (status in ('RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED', 'UNKNOWN'))
);

-- 본인 목록 조회(F17)와 UNKNOWN 복구 스캔(F20)용 인덱스.
create index idx_transfers_actor_created on transfers (actor_user_id, created_at);
create index idx_transfers_status_created on transfers (status, created_at);

create table transfer_histories (
    id varchar(36) primary key,
    transfer_id varchar(36) not null references transfers (id),
    previous_status varchar(16),
    next_status varchar(16) not null,
    reason_code varchar(64),
    occurred_at timestamptz not null default now()
);

create index idx_transfer_histories_transfer on transfer_histories (transfer_id, occurred_at);
