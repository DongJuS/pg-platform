package com.shinhan.pg.settlement.adapter.in.batch;

import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;
import com.shinhan.pg.settlement.application.port.in.CloseSettlementUseCase;
import com.shinhan.pg.settlement.application.result.CloseSettlementResult;

/**
 * <pre>
 * [MVP7-F01,MVP7-F02,MVP7-F07,MVP7-F08] 일 마감 배치 또는 내부 수동 재실행. Kafka 메시지마다 마감하는 기능이 아니다.
 * TODO: 컷오프 이후 거래는 익일로, 같은 날짜 중복 실행 방지
 * TODO: 상계 후 차액, 순액 합계 0 검증, 금융결제원 형식 파일
 * TODO: 생성·송신·결과 수신·원장 반영·확정 단계를 기록. 날짜 외에 회차·버전·결과 식별자를 검토하여 안전하게 재실행.
 * TODO: 업무일·시간대·컷오프 기준을 고정하고 입력 목록 또는 검증 가능한 범위를 보존. 지연 거래의 이월·보정 방식은 미정.
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class CloseSettlementBatchSkeleton {
    private final CloseSettlementUseCase useCase;

    public CloseSettlementBatchSkeleton(CloseSettlementUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public CloseSettlementResult handle(CloseSettlementCommand command) {
        return useCase.closeSettlement(command);
    }
}
