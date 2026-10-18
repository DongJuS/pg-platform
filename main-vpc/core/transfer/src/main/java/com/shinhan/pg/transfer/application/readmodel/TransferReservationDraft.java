package com.shinhan.pg.transfer.application.readmodel;

import java.util.UUID;

/** 멱등 요청 예약으로 재사용할 서버 측 업무 ID 초안. 추적 requestId와 구분한다. */
public record TransferReservationDraft(UUID transferId, UUID transactionId) {}
