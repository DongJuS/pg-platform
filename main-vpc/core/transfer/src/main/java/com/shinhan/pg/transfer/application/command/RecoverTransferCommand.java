package com.shinhan.pg.transfer.application.command;

import java.util.UUID;

/** 내부 입력 초안. 추적 requestId와 금융 멱등성·eventId·업무 ID를 구분한다. 외부 wire 계약은 아니다. */
public record RecoverTransferCommand(UUID requestId, UUID transferId, UUID transactionId) {}
