-- Separate aggregate: a hold journal must never complete a domestic transfers row.
create table overseas_transfers (
    id varchar(36) primary key,
    actor_user_id varchar(36) not null,
    from_account_id varchar(36) not null,
    idempotency_key varchar(128) not null,
    request_hash varchar(64) not null,
    uetr varchar(36) not null unique,
    amount numeric(15,2) not null check (amount > 0),
    currency varchar(3) not null check (currency = 'USD'),
    private_payload text,
    status varchar(32) not null check (status in ('RESERVING','UNKNOWN','PROCESSING','COMPLETED','REJECTED','REVIEW','FAILED')),
    external_outcome varchar(8),
    final_transaction_id varchar(36),
    final_journal_id varchar(36),
    lease_until timestamptz,
    lease_owner varchar(36),
    created_at timestamptz not null default now(),
    private_expires_at timestamptz not null default (now() + interval '48 hours'),
    unique(actor_user_id, idempotency_key)
);
create index idx_overseas_recovery on overseas_transfers(status, lease_until);
create table overseas_audit (
    id varchar(36) primary key,
    transfer_id varchar(36) not null references overseas_transfers(id),
    action varchar(32) not null,
    private_payload text,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '48 hours')
);
