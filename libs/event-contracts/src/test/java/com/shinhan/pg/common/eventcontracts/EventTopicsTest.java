package com.shinhan.pg.common.eventcontracts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 토픽 상수가 infra/docker/init/kafka-topics.sh가 생성하는 토픽명과 일치하는지 고정한다. */
class EventTopicsTest {

    @Test
    void topicNamesMatchKafkaTopicsScript() {
        assertEquals("ledger.posted", EventTopics.LEDGER_POSTED);
        assertEquals("transfer.completed", EventTopics.TRANSFER_COMPLETED);
    }
}
