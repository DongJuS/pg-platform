package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.domain.model.AmlCase;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP6-F01,MVP6-F02,MVP6-F04] 규칙 점수로 보고 대상을 자동 판정한다. 조회는 판정/보고 상태를 바꾸지 않는다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface AmlCaseRepository {
    Optional<AmlCase> findById(UUID id);

    void save(AmlCase value);
}
