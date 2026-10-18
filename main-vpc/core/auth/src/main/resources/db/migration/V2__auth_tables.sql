-- [MVP1-F01..F06] auth 업무 테이블. PostgreSQL(auth_db)용.
-- UUID는 DB 종류 간 공통 계약으로 varchar(36)에 저장한다(String 바인딩).
-- 토큰 원문·비밀번호 평문·OTP 코드를 저장하지 않는다.

create table users (
    id varchar(36) primary key,
    email varchar(320) not null unique,
    password_hash varchar(256) not null,
    name varchar(100) not null,
    phone_number varchar(32),
    roles varchar(256) not null default 'USER',
    created_at timestamptz not null default now()
);

create table refresh_sessions (
    id varchar(36) primary key,
    user_id varchar(36) not null references users (id),
    token_hash varchar(64) not null unique,
    family_id varchar(36) not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_refresh_sessions_family on refresh_sessions (family_id);
create index idx_refresh_sessions_user on refresh_sessions (user_id);

create table otp_proofs (
    id varchar(36) primary key,
    user_id varchar(36) not null references users (id),
    transfer_draft_id varchar(36) not null,
    proof_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    consumed_at timestamptz,
    failure_count int not null default 0,
    created_at timestamptz not null default now()
);

create index idx_otp_proofs_draft on otp_proofs (user_id, transfer_draft_id);
