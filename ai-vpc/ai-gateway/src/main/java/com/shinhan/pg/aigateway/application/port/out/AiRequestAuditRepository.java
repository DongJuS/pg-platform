package com.shinhan.pg.aigateway.application.port.out;

import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;

import java.util.Optional;
import java.util.UUID;

/** Minimal preparation audit repository. HTTP audit lookup and its authorization remain deferred.
 * Implemented by the same PostgreSQL adapter as AiAuditWritePort; no financial Outbox is involved.
 */
public interface AiRequestAuditRepository {
    Optional<AiRequestAudit> findById(UUID id);

    void save(AiRequestAudit value);
}
