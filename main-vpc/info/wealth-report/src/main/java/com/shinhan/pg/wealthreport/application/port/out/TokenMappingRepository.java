package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.domain.model.TokenMapping;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP4-F06] 원본↔토큰 매핑은 내부에서만 저장·복원. 저장 보호 방식·보존 기간은 미정이다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface TokenMappingRepository {
    Optional<TokenMapping> findByReportAndToken(UUID reportId, String tokenLabel);

    void save(TokenMapping value);
}
