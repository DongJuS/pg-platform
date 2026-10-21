package com.shinhan.pg.aml.application.port.out;

/**
 * All facts, cases, immutable decisions/report intents, Inbox and Outbox writes are service-local
 * atomics.
 */
public interface AmlAtomicWritePort extends AmlWorkflowPort {}
