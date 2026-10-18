package com.shinhan.pg.swift.application.port.out;

import com.shinhan.pg.swift.domain.model.SwiftMessage;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP5-F03,MVP5-F05,MVP5-F08] MT103 입력을 pacs.008 필드로 해석하고 동일 UETR로 재송신한다. 상세 전문은 후속 XSD 계약이다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface SwiftMessageRepository {
    Optional<SwiftMessage> findById(UUID id);

    void save(SwiftMessage value);

    Optional<SwiftMessage> findByUetr(String uetr);
}
