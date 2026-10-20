package com.shinhan.pg.aigateway.domain.model;

/** Closed vocabulary: no attacker-controlled diagnostic strings. */
public enum PreparationReason {
    CHECKS_PASSED, INVALID_INPUT, UNSUPPORTED_FORMAT, INPUT_TOO_LARGE,
    RESIDUAL_PII, PROMPT_INJECTION, INSPECTION_UNAVAILABLE, AUDIT_UNAVAILABLE,
    FORBIDDEN_CALLER, UNSUPPORTED_TEXT, RESERVED_TOKEN
}
