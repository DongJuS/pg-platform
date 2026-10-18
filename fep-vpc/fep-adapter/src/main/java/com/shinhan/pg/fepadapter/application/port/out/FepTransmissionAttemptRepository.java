package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.fepadapter.domain.model.FepTransmissionAttempt;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP5-F04,MVP7-F03] 응답 유실 후 같은 업무 식별자로 재전송·결과 확인. UETR 전체를 모든 메시지의 전역 unique로 쓰지 않는다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface FepTransmissionAttemptRepository {
    Optional<FepTransmissionAttempt> findById(UUID id);

    void save(FepTransmissionAttempt value);
}
