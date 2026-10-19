package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.application.readmodel.TransferHistoryView;
import com.shinhan.pg.transfer.application.readmodel.TransferSummary;
import com.shinhan.pg.transfer.application.readmodel.UnknownTransferView;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * [MVP1-F16,MVP1-F17,MVP1-F20] 읽기 전용 조회 포트. 조회는 상태를 바꾸지 않는다.
 * 금융 쓰기는 TransferAtomicWritePort가 같은 로컬 트랜잭션으로 수행하며, 이 포트로 쓰지 않는다.
 * inbound adapter(Kafka consumer·복구 배치)는 메시지를 command로 옮길 때만 식별자 해소 용도로 사용한다.
 */
public interface TransferQueryPort {
    /** transactionId(원장 멱등 키, unique)로 이체 식별자를 해소한다. 없으면 empty. */
    Optional<UUID> findTransferIdByTransactionId(UUID transactionId);

    /** 이체 상태. 복구 전 상태 확인용. 없으면 empty. */
    Optional<String> findStatusById(UUID transferId);

    /** [MVP1-F16] 전이 이력을 발생 시각 순으로 돌려준다. */
    List<TransferHistoryView> findHistories(UUID transferId);

    /** [MVP1-F17] 본인 이체 목록. 기간·상태 필터와 offset 페이지네이션. */
    List<TransferSummary> findSummaries(
            UUID actorUserId, Instant from, Instant to, String status, int offset, int limit);

    /** [MVP1-F20] UNKNOWN 상태 이체를 오래된 순으로 최대 limit건 돌려준다. */
    List<UnknownTransferView> findUnknown(int limit);
}
