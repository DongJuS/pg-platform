-- [MVP1-F11~F14, MVP1-F19] 원장 업무 테이블. MariaDB용. ADR 0001 6-A의 저장 기반:
--   계좌 ID 정렬 순서 SELECT ... FOR UPDATE 잠금, journals.transaction_id unique 멱등,
--   통화별 분개 합계 0, 잔액 갱신과 ledger.posted Outbox 기록의 단일 로컬 트랜잭션.
-- ledger_accounts.balance는 잠금 트랜잭션 안에서만 갱신한다(잔액 스냅샷 캐시 금지).
-- 지시서 §5-② 결정안: 계좌 개설 시 별도 provision 호출 없이 분개 시점에 lazy upsert한다.

create table ledger_accounts (
    id varchar(36) primary key,
    external_account_id varchar(36) not null,
    balance decimal(19, 4) not null,
    currency varchar(3) not null,
    version bigint not null default 0,
    constraint uk_ledger_accounts_external unique (external_account_id)
);

create table journals (
    id varchar(36) primary key,
    transaction_id varchar(36) not null,
    posted_at timestamp(6) not null,
    constraint uk_journals_transaction unique (transaction_id)
);

create table postings (
    id varchar(36) primary key,
    journal_id varchar(36) not null,
    ledger_account_id varchar(36) not null,
    signed_amount decimal(19, 4) not null,
    currency varchar(3) not null,
    constraint fk_postings_journal foreign key (journal_id) references journals (id),
    constraint fk_postings_ledger_account foreign key (ledger_account_id) references ledger_accounts (id)
);

-- F12 거래내역: 계좌별 posting을 journal 시간 역순으로 읽는 경로.
create index idx_postings_ledger_account on postings (ledger_account_id, journal_id);
create index idx_journals_posted_at on journals (posted_at, id);
