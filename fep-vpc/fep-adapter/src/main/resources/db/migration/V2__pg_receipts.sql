create table pg_receipts (
 operation_id varchar(36) primary key,receipt_id varchar(36) not null unique,payment_id varchar(36) not null,
 kind varchar(16) not null,amount numeric(24,8) not null check(amount>0),currency varchar(3) not null,
 dispatched boolean not null default false,result_json text,created_at timestamptz not null default now()
);
create table pg_callbacks (
 event_id varchar(36) primary key,raw_payload bytea not null,signature varchar(64) not null,
 received_at timestamptz not null default now()
);
