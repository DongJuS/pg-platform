package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.domain.model.OverseasTransferDetails;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP5-F01,MVP5-F02,MVP5-F08] 가상 수취인만 사용. UETR은 송금별로 재사용하며 최종 승인/거절과 중간 응답을 구분한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface OverseasTransferDetailsRepository {
    Optional<OverseasTransferDetails> findByTransferId(UUID transferId);

    void save(OverseasTransferDetails value);
}
