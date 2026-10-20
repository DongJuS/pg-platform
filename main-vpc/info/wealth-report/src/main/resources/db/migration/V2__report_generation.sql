-- Wealth Report local PostgreSQL only. No raw prompts, names, account numbers or token maps.
create table asset_reports (
 id uuid primary key, user_id uuid not null, request_key varchar(128) not null,
 fingerprint varchar(64) not null, as_of timestamptz not null,
 status varchar(24) not null check(status in ('PROCESSING','COMPLETED','FAILED','TIMED_OUT','INTERRUPTED')),
 stage varchar(24) not null, calculation_version varchar(48) not null,
 failure_code varchar(48), draft_source varchar(16) check(draft_source in ('AI','TEST_DOUBLE')),
 created_at timestamptz not null default clock_timestamp(), deadline timestamptz not null,
 completed_at timestamptz,
 unique(user_id,request_key),
 check ((status='COMPLETED')=(completed_at is not null)),
 check (status<>'COMPLETED' or draft_source is not null)
);
create unique index one_generation_per_user on asset_reports(user_id) where status='PROCESSING';
create index report_list on asset_reports(user_id,created_at desc,id);
create table report_stages (
 report_id uuid not null references asset_reports(id), stage varchar(24) not null,
 recorded_at timestamptz not null default clock_timestamp(), primary key(report_id,stage)
);
create table asset_snapshots (
 id uuid primary key, report_id uuid not null unique references asset_reports(id),
 user_id uuid not null, period_from timestamptz not null, period_to timestamptz not null,
 as_of timestamptz not null, captured_at timestamptz not null, source_version varchar(64) not null,
 check(period_from<period_to and period_to<=as_of and as_of<=captured_at)
);
create table snapshot_accounts (
 snapshot_id uuid not null references asset_snapshots(id), account_id uuid not null,
 balance numeric(25,0) not null, cash_in numeric(25,0) not null check(cash_in>=0),
 cash_out numeric(25,0) not null check(cash_out>=0), currency varchar(3) not null check(currency='KRW'),
 source_version bigint not null check(source_version>=0), primary key(snapshot_id,account_id)
);
create table report_files (
 report_id uuid primary key references asset_reports(id), pdf bytea not null,
 check(octet_length(pdf)>4 and octet_length(pdf)<=5242880)
);
-- Event notification index, never an amount accumulator. Two topics may describe the same posting.
create table snapshot_effects (
 transaction_id uuid primary key, journal_id uuid not null unique, occurred_at timestamptz not null,
 received_at timestamptz not null default clock_timestamp()
);
create table snapshot_events (
 topic varchar(48) not null, event_id uuid not null, transaction_id uuid not null references snapshot_effects(transaction_id),
 journal_id uuid not null, occurred_at timestamptz not null, primary key(topic,event_id)
);
