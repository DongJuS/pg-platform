-- A proof authorizes one logical transfer. Same-request retries remain safe after a lost reply.
create table otp_transfer_uses (
    proof_id varchar(36) primary key references otp_proofs(id),
    idempotency_key varchar(255) not null,
    request_hash varchar(64) not null,
    consumed_at timestamptz not null
);
