package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.readmodel.TransferReservationDraft;

import java.util.UUID;

/**
 * actorUserId+idempotencyKey 중복 요청은 기존 결정대로 충돌 오류. 이체 완료 상태와 transfer.completed Outbox 기록은 같은
 * PostgreSQL 트랜잭션에서 한 번만 반영한다. REST 결과와 ledger.posted가 같은 완료 처리를 사용한다.
 */
public interface TransferAtomicWritePort {
    TransferReservationDraft reserveRequest(RequestTransferCommand command);

    void completeOnceWithOutbox(CompleteTransferCommand command);

    void markUnknown(UUID transferId);

    /**
     * 원장이 명시적으로 거절(422)한 건을 최종 실패로 전이한다. 최종 상태 역전은 하지 않는다.
     * WO-A6 열거 외 추가분 — FAILED 전이는 상태 정책상 허용되나 기존 포트 시그니처에 쓰기 경로가 없어 추가했다.
     * 복구(F20)에서 조회 404만으로는 이 메서드를 호출하지 않는다(실패 단정 금지).
     */
    void markFailed(UUID transferId, String reasonCode);
}
