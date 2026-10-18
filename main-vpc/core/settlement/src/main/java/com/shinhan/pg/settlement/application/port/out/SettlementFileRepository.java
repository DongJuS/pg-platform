package com.shinhan.pg.settlement.application.port.out;

import com.shinhan.pg.settlement.domain.model.SettlementFile;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP7-F02,MVP7-F03,MVP7-F07] 파일 건수·합계·해시와 결과 파일 대사를 보존한다. 고정길이 필드·순액 산식 상세는 후속이다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface SettlementFileRepository {
    Optional<SettlementFile> findById(UUID id);

    void save(SettlementFile value);
}
