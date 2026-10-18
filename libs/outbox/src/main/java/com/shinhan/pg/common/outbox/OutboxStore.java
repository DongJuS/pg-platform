package com.shinhan.pg.common.outbox;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
// Implement only in the owning service's adapter.out.persistence package.
public interface OutboxStore {
    // Business updates and append MUST use the same service-local transaction/connection.
    void append(OutboxRecord record);
    // Claim atomically with a persisted lease, e.g. FOR UPDATE SKIP LOCKED.
    // Stale workers must not complete another worker's lease; compare claimToken on updates.
    List<OutboxRecord> claim(int limit, UUID claimToken, Duration lease);
    void published(UUID id, UUID claimToken);
    void retry(UUID id, UUID claimToken);
}
