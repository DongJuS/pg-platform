package com.shinhan.pg.aigateway.application.port.out;

import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP3-F01,MVP3-F02] 원문 프롬프트·응답·인증정보는 감사 기록에 기본 저장하지 않는다. 상세 저장/조회 정책은 보류다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface AiRequestAuditRepository {
    Optional<AiRequestAudit> findById(UUID id);

    void save(AiRequestAudit value);
}
