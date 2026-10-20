create table swift_receipts (
    transfer_id varchar(36) primary key,
    uetr varchar(36) not null unique,
    message_id varchar(36) not null unique,
    message_reference varchar(16) not null unique,
    message_key varchar(128) not null unique,
    message_hash varchar(64) not null,
    pacs008 text,
    result_xml text,
    outcome varchar(16) not null default 'READY',
    attempt_count integer not null default 0,
    last_attempt_at timestamptz,
    lease_until timestamptz,
    lease_owner varchar(36),
    created_at timestamptz not null default now(),
    private_expires_at timestamptz not null default (now() + interval '48 hours')
);
create table swift_transmission_attempts (
    id varchar(36) primary key,
    transfer_id varchar(36) not null references swift_receipts(transfer_id),
    attempt_number integer not null,
    action varchar(16) not null,
    outcome varchar(16) not null,
    created_at timestamptz not null default now(),
    unique(transfer_id,attempt_number)
);
create table swift_result_reports (
    id varchar(36) primary key,
    uetr varchar(36) not null references swift_receipts(uetr),
    event_reference varchar(35) not null,
    result_hash varchar(64) not null,
    private_payload text,
    received_at timestamptz not null default now(),
    expires_at timestamptz not null default (now()+interval '48 hours'),
    unique(uetr,event_reference,result_hash)
);
