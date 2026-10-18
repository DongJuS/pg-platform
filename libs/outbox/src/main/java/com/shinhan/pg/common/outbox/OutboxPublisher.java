package com.shinhan.pg.common.outbox;
import java.util.concurrent.CompletionStage;
public interface OutboxPublisher { CompletionStage<Void> publish(OutboxRecord record); }
