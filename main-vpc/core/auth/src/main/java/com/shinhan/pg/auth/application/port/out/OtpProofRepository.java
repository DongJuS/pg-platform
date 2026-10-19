package com.shinhan.pg.auth.application.port.out;

import com.shinhan.pg.auth.domain.model.OtpProof;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F06] Mock OTP의 유효시간·일회 소모·실패 잠금 정책은 상세 계약 후 구현한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface OtpProofRepository {
    Optional<OtpProof> findById(UUID id);

    void save(OtpProof value);

    /** Atomically bind the proof to one user/request. An identical retry is allowed; a different use is rejected. */
    boolean useForTransfer(UUID proofId, UUID userId, String idempotencyKey, String requestHash, java.time.Instant now);


    /** 사용자·이체 초안의 최근 검증 시도를 조회한다. 없으면 최초 시도다. */
    Optional<OtpProof> findLatestByUserIdAndTransferDraftId(UUID userId, UUID transferDraftId);
}
