package com.shinhan.pg.swift.application.port.in;

import com.shinhan.pg.swift.application.command.RetrySwiftCommand;

/** 무응답 송금은 같은 UETR로 FEP REST 재전송한다. */
public interface RetrySwiftUseCase {
    void retrySwift(RetrySwiftCommand command);
}
