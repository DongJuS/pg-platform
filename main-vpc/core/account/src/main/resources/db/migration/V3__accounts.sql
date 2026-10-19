-- [MVP1-F07..F10] account 업무 테이블. MariaDB(account_db)용.
-- UUID는 DB 종류 간 공통 계약으로 varchar(36)에 저장한다(String 바인딩) — 기존 outbox V2 패턴 준용.
-- 잔액 컬럼은 두지 않는다. 잔액은 ledger 서비스가 소유한다(ADR: account는 계좌 메타데이터만 소유).
-- account_number는 서버 생성 값이며 unique 충돌 시 애플리케이션이 1회 재생성한다.

create table accounts (
    id varchar(36) primary key,
    owner_id varchar(36) not null,
    account_number varchar(32) not null unique,
    currency char(3) not null,
    alias varchar(100),
    status varchar(16) not null default 'ACTIVE',
    created_at timestamp(6) not null default now()
);

-- 내 계좌 목록(F08) 조회 경로: owner별 최신순 페이지네이션.
create index idx_accounts_owner on accounts (owner_id, created_at);
