package com.shinhan.pg.aigateway.adapter.out.persistence;

import com.shinhan.pg.aigateway.application.port.out.AiAuditWritePort;
import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;

/** Historical inference placeholder; never registered by preparation configuration. */
public final class AiAuditWriteSkeleton implements AiAuditWritePort {
    @Override public void record(AiRequestAudit audit) {
        throw new UnsupportedOperationException("Deferred inference audit");
    }
}
