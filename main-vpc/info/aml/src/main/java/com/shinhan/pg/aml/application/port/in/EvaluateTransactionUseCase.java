package com.shinhan.pg.aml.application.port.in;

import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.application.result.EvaluateTransactionResult;

/** Kafka ledger.posted·transfer.completed 기반 룰 평가와 자동 보고. 거래 효과 중복을 구분한다. */
public interface EvaluateTransactionUseCase {
    EvaluateTransactionResult evaluateTransaction(EvaluateTransactionCommand command);
}
