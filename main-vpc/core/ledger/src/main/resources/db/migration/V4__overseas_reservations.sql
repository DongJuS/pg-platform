create table overseas_reservations (
    transfer_id varchar(36) primary key,
    uetr varchar(36) not null unique,
    actor_user_id varchar(36) not null,
    source_account_id varchar(36) not null,
    amount decimal(15,2) not null check (amount > 0),
    currency varchar(3) not null check (currency = 'USD'),
    hold_account_id varchar(36) not null unique,
    hold_journal_id varchar(36) not null,
    outcome varchar(8),
    final_journal_id varchar(36),
    review_required boolean not null default false,
    created_at timestamp(6) not null default current_timestamp(6)
);
