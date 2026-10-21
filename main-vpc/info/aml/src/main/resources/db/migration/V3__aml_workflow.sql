-- AML is a local PostgreSQL workflow. No source-service tables or cash/CTR defaults.
create table aml_inbox (
 event_id varchar(36) not null, topic varchar(64) not null, transaction_id varchar(36) not null,
 journal_id varchar(36), status varchar(24) not null default 'PENDING', reason varchar(64),
 attempts int not null default 0, next_attempt_at timestamptz not null default now(),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 primary key(event_id,topic), check(status in ('PENDING','RESOLVED','EXCLUDED','REVIEW'))
);
create index aml_inbox_recovery on aml_inbox(status,next_attempt_at,created_at);
create table aml_facts (
 transaction_id varchar(36) primary key, customer_id varchar(36) not null, business_id varchar(36) not null,
 kind varchar(32) not null, currency varchar(3) not null check(currency in ('KRW','USD')),
 amount numeric(24,4) not null check(amount>0), occurred_at timestamptz not null,
 original_business_id varchar(36), document text not null,
 unique(kind,business_id)
);
create index aml_facts_window on aml_facts(customer_id,currency,occurred_at,transaction_id);
create table aml_rule_sets (version varchar(64) primary key, document text not null, created_at timestamptz not null default now());
create table aml_rules (code varchar(64) not null, version varchar(64) not null references aml_rule_sets(version), document text not null, primary key(code,version));
create table aml_cases (
 id varchar(36) primary key, case_key varchar(128) not null unique,
 transaction_id varchar(36) not null references aml_facts(transaction_id), rule_version varchar(64) not null references aml_rule_sets(version),
 risk_score int not null check(risk_score>=0), status varchar(24) not null,
 assessment text not null, latest_assessment text not null, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 unique(transaction_id,rule_version), check(status in ('NO_REPORT','DETECTED','REPORT_REQUIRED','REVIEW'))
);
create index aml_cases_search on aml_cases(status,created_at,id);
create table aml_reports (
 id varchar(36) primary key, case_id varchar(36) not null references aml_cases(id), report_key varchar(128) not null unique,
 document text not null, content_hash varchar(64) not null, revision int not null default 1, supersedes_id varchar(36) unique,
 status varchar(24) not null default 'READY', receipt_reference varchar(80),
 institution_status varchar(20) not null default 'UNKNOWN', deliveries int not null default 0, acceptances int not null default 0,
 lease_owner varchar(36), lease_until timestamptz,
 query_attempts int not null default 0, next_query_at timestamptz not null default now(),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 check(status in ('READY','SENDING','FEP_RECEIVED','UNKNOWN','FAILED','REPORTED','REJECTED','REVIEW')),
 check(institution_status in ('UNKNOWN','NOT_RECEIVED','PENDING','ACCEPTED','REJECTED','REVIEW')),
 unique(case_id,revision), check(acceptances between 0 and 1)
);
create index aml_reports_recovery on aml_reports(status,next_query_at,lease_until);
create table aml_audit (
 id varchar(36) primary key, resource_id varchar(36) not null, action varchar(48) not null, actor varchar(64) not null,
 created_at timestamptz not null default now()
);

-- Irreversible hash-only tombstones prevent replay after private content expires.
create table aml_expired_effects (effect_hash varchar(64) primary key, expired_at timestamptz not null default now());
