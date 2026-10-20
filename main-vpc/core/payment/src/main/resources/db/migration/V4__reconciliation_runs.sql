create table reconciliation_runs(id varchar(36) primary key,business_date date not null,
 status varchar(16) not null check(status in ('RUNNING','COMPLETED','FAILED')),created_at timestamptz not null default now());
create unique index one_active_reconciliation_per_date on reconciliation_runs(business_date) where status='RUNNING';
create table reconciliation_differences(id varchar(36) primary key,run_id varchar(36) not null references reconciliation_runs(id),
 difference_type varchar(32) not null,payment_id varchar(36),external_reference varchar(100),internal_amount numeric(24,8),external_amount numeric(24,8));
