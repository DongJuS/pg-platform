package com.shinhan.pg.aigateway.application.port.out;

import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;

/** Returns only after the minimal audit is durably committed. Failure prevents a ready response. */
public interface AiAuditWritePort { void record(AiRequestAudit audit); }
