package com.shinhan.pg.transfer.domain.policy;

import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;
import com.shinhan.pg.transfer.domain.model.Transfer;

/**
 * <pre>
 * [MVP1-F15,MVP1-F20] actorUserId+idempotencyKey·본문 해시를 비교한다. 같은 키 요청은 409. 원장 재조회 복구와 신규 요청을 구분한다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 *
 * <p>확정 계약(WO-A6): 같은 키 재요청은 본문 해시와 무관하게 항상 409이며 신규 이체를 만들지 않는다.
 * 해시가 다르면 키 충돌(다른 요청에 키 재사용), 같으면 단순 중복으로 메시지를 구분할 뿐 HTTP 결과는 동일하다.
 * 멱등키 unique 강제는 DB 제약(unique(actor_user_id, idempotency_key))이 담당하고, 이 정책은
 * 충돌을 잡은 뒤 오류 의미를 결정한다. 이 클래스는 상태를 바꾸지 않는다.</p>
 */
public final class TransferIdempotencyPolicy {
    /** 같은 키의 기존 이체가 있으면 항상 DUPLICATE_REQUEST(409)로 거절한다. */
    public void validateDuplicateRequest(Transfer original, String requestHash) {
        java.util.Objects.requireNonNull(original);
        if (original.requestHash() != null && original.requestHash().equals(requestHash)) {
            throw new TransferException(
                    TransferErrorCode.DUPLICATE_REQUEST,
                    "Duplicate transfer request for the same idempotency key",
                    original.id());
        }
        throw new TransferException(
                TransferErrorCode.DUPLICATE_REQUEST,
                "Idempotency key was already used with a different request body",
                original.id());
    }
}
