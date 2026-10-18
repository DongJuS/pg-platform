# Technical outbox support
Use only from service adapter.out code. No business table, persistence entity, topic, or business event is included.
Implement OutboxStore with service-local transactional append and atomic persisted lease claims.
Enable `outbox.enabled=true` only after a store exists. Set producer value serializer to ByteArraySerializer for this relay.
The relay waits for broker acknowledgement before marking a claim published. A crash after publish may redeliver;
consumers must deduplicate stable event IDs using a database unique constraint. This is at-least-once delivery.
