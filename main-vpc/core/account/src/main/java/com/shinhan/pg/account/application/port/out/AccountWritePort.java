package com.shinhan.pg.account.application.port.out;

import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.application.result.OpenAccountResult;

/** 계좌 개설 진행 상태와 재개 지점을 MariaDB에 저장할 위치. */
public interface AccountWritePort {
    OpenAccountResult create(OpenAccountCommand command);
}
