package com.shinhan.pg.auth.application.port.out;

import com.shinhan.pg.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F01,MVP1-F02,MVP1-F05] 이메일 고유성·비밀번호 해시만 저장. 이메일/전화번호/해시를 로그에 남기지 않는다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface UserRepository {
    Optional<User> findById(UUID id);

    void save(User value);

    Optional<User> findByEmail(String email);
}
