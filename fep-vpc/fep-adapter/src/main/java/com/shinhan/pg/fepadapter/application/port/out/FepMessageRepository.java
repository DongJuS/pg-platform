package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.fepadapter.domain.model.FepMessage;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP5-F04,MVP7-F03] 접수와 기관 최종 결과를 구분한다. 채널+messageKey로 중복 전송을 통제한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface FepMessageRepository {
    Optional<FepMessage> findById(UUID id);

    void save(FepMessage value);

    Optional<FepMessage> findByKey(String channelCode, String messageKey);
}
