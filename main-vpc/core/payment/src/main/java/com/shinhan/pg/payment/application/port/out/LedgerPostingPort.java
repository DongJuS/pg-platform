package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.application.command.ApplyPaymentConfirmationCommand;

import java.util.UUID;

/** 확정된 PG 결과에 대한 원장 반영 위치. 동일 업무 ID를 재사용한다. */
public interface LedgerPostingPort {
    UUID postApproval(ApplyPaymentConfirmationCommand command);
}
