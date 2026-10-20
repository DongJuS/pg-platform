-- MVP2: one local PostgreSQL aggregate for reservations and confirmed financial effects.
create table payments (
 id varchar(36) primary key, actor_user_id varchar(36) not null, merchant_id varchar(36) not null,
 order_reference varchar(100) not null, source_account_id varchar(36) not null, destination_account_id varchar(36) not null,
 amount numeric(24,8) not null check(amount>0), currency varchar(3) not null,
 refunded_amount numeric(24,8) not null default 0 check(refunded_amount>=0),
 reserved_amount numeric(24,8) not null default 0 check(reserved_amount>=0),
 status varchar(32) not null default 'PROCESSING' check(status in ('PROCESSING','UNKNOWN','APPROVED','PARTIALLY_REFUNDED','REFUNDED','CANCELLED','DECLINED')),
 version bigint not null default 0, idempotency_key varchar(100) not null,
 created_at timestamptz not null default now(),
 unique(actor_user_id,idempotency_key), unique(actor_user_id,merchant_id,order_reference),
 check(source_account_id<>destination_account_id), check(refunded_amount+reserved_amount<=amount)
);
create table payment_operations (
 id varchar(36) primary key, payment_id varchar(36) not null references payments(id),
 kind varchar(16) not null check(kind in ('APPROVAL','REFUND','CANCEL')),
 amount numeric(24,8) not null check(amount>0), currency varchar(3) not null,
 idempotency_key varchar(100) not null,
 status varchar(32) not null default 'READY' check(status in ('READY','DISPATCHED','UNKNOWN','PG_APPROVED','LEDGER_SENDING','LEDGER_UNKNOWN','LEDGER_REJECTED','COMPLETED','DECLINED')),
 pg_outcome varchar(16) check(pg_outcome in ('PENDING','APPROVED','DECLINED')),
 pg_version bigint not null default 0 check(pg_version>=0), pg_sent boolean not null default false,
 ledger_sent boolean not null default false, journal_id varchar(36),
 business_date date not null default (now() at time zone 'Asia/Seoul')::date,
 created_at timestamptz not null default now(), unique(payment_id,kind,idempotency_key),
 check(status<>'COMPLETED' or (pg_outcome is not null and pg_outcome='APPROVED' and ledger_sent and journal_id is not null))
);
create unique index one_approval_per_payment on payment_operations(payment_id) where kind='APPROVAL';
create unique index one_cancel_per_payment on payment_operations(payment_id) where kind='CANCEL';
create index payment_operations_date on payment_operations(business_date);
create table pg_events (
 event_id varchar(36) primary key, operation_id varchar(36) not null references payment_operations(id),
 payment_id varchar(36) not null references payments(id), version bigint not null check(version>0),
 payload_hash varchar(64) not null, received_at timestamptz not null default now()
);
