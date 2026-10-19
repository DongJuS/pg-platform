package com.shinhan.pg.account.adapter.out.persistence;

import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.application.port.out.AccountWritePort;
import com.shinhan.pg.account.application.result.OpenAccountResult;
import com.shinhan.pg.account.domain.error.AccountErrorCode;
import com.shinhan.pg.account.domain.model.Account;
import com.shinhan.pg.common.commonerror.ApplicationException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.SecureRandom;
import java.util.UUID;

/** 계좌 개설 진행 상태와 재개 지점을 MariaDB에 저장할 위치. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다.
 *
 * <p>구현 확정(MVP1-F07): JdbcTemplate으로 accounts에 INSERT한다. 개설 시 상태는 ACTIVE,
 * 계좌번호는 서버 생성(account_number unique). unique 충돌 시 계좌번호를 1회 재생성하고,
 * 재충돌하면 ACCOUNT_NUMBER_GENERATION_FAILED(500)로 실패한다 — 가짜 성공을 반환하지 않는다.
 * upsert가 아닌 순수 INSERT를 쓴다: account_number 충돌을 update로 덮어쓰면 타인 계좌가 훼손된다.
 */
public final class AccountWriteAdapter implements AccountWritePort {
    /** 계좌번호는 16자리 숫자 문자열. TODO[CONTRACT]: 은행 코드·체크 디짓 등 최종 계좌번호 형식은 미정 — 임의 확정 아님, 충돌 재시도 가능한 최소 형식만 사용. */
    private static final int ACCOUNT_NUMBER_DIGITS = 16;

    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public AccountWriteAdapter(JdbcTemplate jdbc) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc);
    }

    @Override
    public OpenAccountResult create(OpenAccountCommand command) {
        var accountId = UUID.randomUUID();
        // unique 충돌 시 재시도 1회(계약). 그 외 재시도 없음.
        for (int attempt = 0; attempt < 2; attempt++) {
            var accountNumber = generateAccountNumber();
            try {
                jdbc.update(
                        "insert into accounts (id, owner_id, account_number, currency, alias, status)"
                                + " values (?, ?, ?, ?, ?, ?)",
                        accountId.toString(),
                        command.actorUserId().toString(),
                        accountNumber,
                        command.currency(),
                        command.alias(),
                        Account.STATUS_ACTIVE);
                return new OpenAccountResult(
                        accountId, accountNumber, command.currency(), Account.STATUS_ACTIVE);
            } catch (DuplicateKeyException duplicate) {
                if (attempt == 1) {
                    throw new ApplicationException(
                            AccountErrorCode.ACCOUNT_NUMBER_GENERATION_FAILED);
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private String generateAccountNumber() {
        var digits = new StringBuilder(ACCOUNT_NUMBER_DIGITS);
        // 첫 자리 0 방지 — 문자열이지만 숫자 형태를 유지한다.
        digits.append(random.nextInt(9) + 1);
        for (int i = 1; i < ACCOUNT_NUMBER_DIGITS; i++) {
            digits.append(random.nextInt(10));
        }
        return digits.toString();
    }
}
