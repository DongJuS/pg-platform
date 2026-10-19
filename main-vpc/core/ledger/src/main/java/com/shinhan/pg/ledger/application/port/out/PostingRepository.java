package com.shinhan.pg.ledger.application.port.out;

import com.shinhan.pg.ledger.application.readmodel.JournalEntryView;
import com.shinhan.pg.ledger.domain.model.Posting;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F12,MVP1-F13] 원본 분개 삭제로 취소하지 않고 연계 역분개를 검토한다. 통화·반올림 상세 계산은 임의 확정하지 않는다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface PostingRepository {
    Optional<Posting> findById(UUID id);

    /**
     * [MVP1-F12] 계좌 거래내역 키셋 페이지네이션. (posted_at, journal_id) 역순 정렬이며
     * cursorPostedAt/cursorJournalId가 있으면 그 지점 바로 다음부터 읽는다.
     * from/to는 posted_at 포함 범위 필터다.
     */
    List<JournalEntryView> findEntriesByExternalAccountId(
            UUID externalAccountId,
            Instant from,
            Instant to,
            Instant cursorPostedAt,
            UUID cursorJournalId,
            int limit);

    void save(Posting value);
}
