package com.shinhan.pg.account.application.port.out;

import com.shinhan.pg.account.application.command.OpenAccountCommand;

import java.util.UUID;

/** 계좌 개설에 필요한 원장 계정 준비를 내부 REST로 요청한다. 교차 DB 원자성을 가정하지 않는다. */
public interface LedgerAccountPort {
    UUID provision(OpenAccountCommand command);
}
