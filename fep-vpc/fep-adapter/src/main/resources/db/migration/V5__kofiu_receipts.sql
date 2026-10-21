create table kofiu_receipts (
 report_id varchar(36) primary key, report_key varchar(128) not null unique,
 content_hash varchar(64) not null, document text not null,
 receipt_reference varchar(80) not null unique,
 transmission_status varchar(16) not null default 'READY', institution_status varchar(20) not null default 'UNKNOWN',
 attempts int not null default 0, deliveries int not null default 0, acceptances int not null default 0,
 lease_owner varchar(36), lease_until timestamptz, last_sent_at timestamptz,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 check (transmission_status in ('READY','SENDING','SENT','UNKNOWN','FAILED')),
 check (institution_status in ('NOT_RECEIVED','PENDING','ACCEPTED','REJECTED','UNKNOWN','REVIEW')),
 check (attempts>=0 and deliveries>=0 and acceptances between 0 and 1)
);
create table kofiu_attempts (
 id varchar(36) primary key, report_id varchar(36) not null references kofiu_receipts(report_id),
 attempt_number int not null, status varchar(16) not null, created_at timestamptz not null default now(),
 unique(report_id,attempt_number)
);

create table kofiu_expired_reports(key_hash varchar(64) primary key,content_hash varchar(64) not null,expired_at timestamptz not null default now());
