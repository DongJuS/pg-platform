-- MVP3 pre-transfer inspection only. No prompt, raw hash, secret or restoration map columns.
CREATE TABLE ai_request_audits (
    request_id UUID PRIMARY KEY,
    caller_service_id VARCHAR(48) NOT NULL CHECK (caller_service_id ~ '^[a-z][a-z0-9-]{0,47}$'),
    decision VARCHAR(32) NOT NULL CHECK (decision IN ('READY_FOR_TRANSFER','BLOCKED','ERROR')),
    reason VARCHAR(32) NOT NULL CHECK (reason IN (
        'CHECKS_PASSED','INVALID_INPUT','UNSUPPORTED_FORMAT','INPUT_TOO_LARGE','RESIDUAL_PII',
        'PROMPT_INJECTION','INSPECTION_UNAVAILABLE','FORBIDDEN_CALLER','UNSUPPORTED_TEXT','RESERVED_TOKEN')),
    name_count INTEGER NOT NULL CHECK (name_count >= 0),
    rrn_count INTEGER NOT NULL CHECK (rrn_count >= 0),
    account_count INTEGER NOT NULL CHECK (account_count >= 0),
    phone_count INTEGER NOT NULL CHECK (phone_count >= 0),
    email_count INTEGER NOT NULL CHECK (email_count >= 0),
    card_count INTEGER NOT NULL CHECK (card_count >= 0),
    policy_version VARCHAR(40) NOT NULL CHECK (policy_version = 'preparation-rules-v1'),
    created_at TIMESTAMPTZ NOT NULL,
    CHECK ((decision = 'READY_FOR_TRANSFER') = (reason = 'CHECKS_PASSED')),
    CHECK ((decision = 'ERROR') = (reason = 'INSPECTION_UNAVAILABLE'))
);
CREATE INDEX ai_request_audits_created_idx ON ai_request_audits (created_at);
