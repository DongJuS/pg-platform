package com.shinhan.pg.aml.application.port.out;

import java.util.UUID;

/**
 * <pre>
 * 소비자 eventId와 업무 effectKey의 중복 처리를 같은 DB 트랜잭션에서 보호할 위치.
 * TODO: 소비자 이름+eventId와 소비자 이름+effectKey의 unique 의미를 구분한다.
 * TODO: 거래 반영과 claimOnce를 원자적으로 묶는다. true를 임의로 반환하지 않는다.
 * </pre>
 */
public interface ProcessedEffectRepository {
    boolean claimOnce(String consumerName, UUID eventId, String effectKey);
}
