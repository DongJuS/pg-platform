package com.shinhan.pg.ledger.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** [MVP1-F13,MVP1-F14] 순수 업무 모델 초안. 동일 transactionId의 분개는 한 번만 반영. 통화별 posting 합계 0·부분 반영 없음. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record Journal(UUID id, UUID transactionId, List<Posting> postings, Instant postedAt) {}
