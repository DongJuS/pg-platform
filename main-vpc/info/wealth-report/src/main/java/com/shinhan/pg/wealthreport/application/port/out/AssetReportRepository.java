package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.domain.model.AssetReport;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP4-F01,MVP4-F02,MVP4-F03,MVP4-F10] PDF 저장 완료 후 동기 응답. 진행 단계와 asOf는 중단 복구를 위해 별도 보존한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface AssetReportRepository {
    Optional<AssetReport> findById(UUID id);

    void save(AssetReport value);
}
