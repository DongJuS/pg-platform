package com.shinhan.pg.ledger.adapter.out.persistence.mapper;

import com.shinhan.pg.ledger.adapter.out.persistence.entity.JournalEntity;
import com.shinhan.pg.ledger.domain.model.Journal;

import java.util.List;

/** [MVP1-F13,MVP1-F14] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class JournalEntityMapper {
    private final PostingEntityMapper postingEntityMapper = new PostingEntityMapper();

    public Journal toDomain(JournalEntity entity) {
        /* 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        var postings =
                entity.postings() == null
                        ? List.<com.shinhan.pg.ledger.domain.model.Posting>of()
                        : entity.postings().stream().map(postingEntityMapper::toDomain).toList();
        return new Journal(entity.id(), entity.transactionId(), postings, entity.postedAt());
    }

    public JournalEntity toEntity(Journal value) {
        /* SQL 매핑: postings는 별도 테이블에 저장되므로 여기서는 비운다 */
        return new JournalEntity(
                value.id(), value.transactionId(), List.of(), value.postedAt());
    }
}
