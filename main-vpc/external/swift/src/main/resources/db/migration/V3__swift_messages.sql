create table swift_messages (
    transfer_id varchar(36) primary key,
    uetr varchar(36) not null unique,
    message_reference varchar(16) not null unique,
    message_hash varchar(64) not null,
    mt103 text,
    pacs008 text,
    result_xml text,
    outcome varchar(16) not null default 'READY',
    attempt_count integer not null default 0 check (attempt_count >= 0),
    last_attempt_at timestamptz,
    next_query_at timestamptz not null default now(),
    lease_until timestamptz,
    lease_owner varchar(36),
    created_at timestamptz not null default now(),
    private_expires_at timestamptz not null default (now() + interval '48 hours')
);
create table swift_result_inbox (
    event_reference varchar(35) primary key,
    transfer_id varchar(36) not null references swift_messages(transfer_id),
    result_hash varchar(64) not null,
    created_at timestamptz not null default now()
);
create table swift_private_audit (
    id varchar(36) primary key,
    transfer_id varchar(36) not null references swift_messages(transfer_id),
    action varchar(32) not null,
    private_payload text,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '48 hours')
);
