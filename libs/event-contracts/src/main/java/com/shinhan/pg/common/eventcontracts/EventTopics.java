package com.shinhan.pg.common.eventcontracts;

/**
 * Kafka 토픽 이름 상수. 토픽명을 각 서비스 코드에 분산 하드코딩하지 말고 이 클래스를 사용한다. 값은 {@code
 * infra/docker/init/kafka-topics.sh}(및 docker-compose 변형)가 생성하는 토픽과 일치해야 한다.
 */
public final class EventTopics {
    public static final String TRANSFER_OUTBOUND = "transfer.outbound";
    public static final String FEP_ACK = "fep.ack";
    public static final String OVERSEAS_LEDGER_APPLIED = "ledger.overseas-applied";

    /** ADR 0001 2-B: Ledger가 분개 확정 후 발행. */
    public static final String LEDGER_POSTED = "ledger.posted";

    /** ADR 0001 2-B: Transfer가 이체 상태 확정 후 발행. */
    public static final String TRANSFER_COMPLETED = "transfer.completed";

    private EventTopics() {}
}
