package com.shinhan.pg.common.outbox;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OutboxRelayTest {
    static class Store implements OutboxStore {
        UUID token; int sent; int retries;
        final OutboxRecord record=new OutboxRecord(UUID.randomUUID(),UUID.randomUUID(),"test-destination","key",new byte[]{1});
        public void append(OutboxRecord record) { }
        public List<OutboxRecord> claim(int limit,UUID token,Duration lease) { this.token=token; return List.of(record); }
        public void published(UUID id,UUID token) { assertEquals(this.token,token); assertEquals(record.id(),id); sent++; }
        public void retry(UUID id,UUID token) { assertEquals(this.token,token); retries++; }
    }
    @Test void marksPublishedOnlyAfterAcknowledgement() {
        var store=new Store();
        var acknowledgement=new CompletableFuture<Void>();
        new OutboxRelay(store,record -> { assertEquals(0,store.sent); acknowledgement.complete(null); return acknowledgement; }).poll();
        assertEquals(1,store.sent); assertEquals(0,store.retries);
    }
    @Test void failureRemainsRetryable() {
        var store=new Store();
        new OutboxRelay(store,record -> CompletableFuture.failedFuture(new IllegalStateException("broker unavailable"))).poll();
        assertEquals(0,store.sent); assertEquals(1,store.retries);
    }
}
