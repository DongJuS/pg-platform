package com.shinhan.pg.account.adapter.out.persistence;

import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.application.port.out.AccountWritePort;
import com.shinhan.pg.account.application.result.OpenAccountResult;

/** 계좌 개설 진행 상태와 재개 지점을 MariaDB에 저장할 위치. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class AccountWriteSkeleton implements AccountWritePort {
    @Override
    public OpenAccountResult create(OpenAccountCommand command) {
        throw new UnsupportedOperationException("SKELETON account: create");
    }
}
