package com.shinhan.pg.swift.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.swift.application.command.RecordSwiftAcknowledgementCommand;
import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.port.in.RecordSwiftAcknowledgementUseCase;
import com.shinhan.pg.swift.application.port.in.RetrySwiftUseCase;
import com.shinhan.pg.swift.application.port.in.SubmitSwiftUseCase;
import com.shinhan.pg.swift.application.port.out.FepSwiftPort;
import com.shinhan.pg.swift.application.port.out.SwiftAtomicWritePort;
import com.shinhan.pg.swift.application.port.out.SwiftMessageRepository;
import com.shinhan.pg.swift.application.result.SubmitSwiftResult;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class SwiftApplicationService
        implements SubmitSwiftUseCase, RetrySwiftUseCase, RecordSwiftAcknowledgementUseCase {
    private final FepSwiftPort fepSwiftPort;
    private final SwiftAtomicWritePort swiftAtomicWritePort;
    private final SwiftMessageRepository swiftMessageRepository;

    public SwiftApplicationService(
            FepSwiftPort fepSwiftPort,
            SwiftAtomicWritePort swiftAtomicWritePort,
            SwiftMessageRepository swiftMessageRepository) {
        this.fepSwiftPort = java.util.Objects.requireNonNull(fepSwiftPort);
        this.swiftAtomicWritePort = java.util.Objects.requireNonNull(swiftAtomicWritePort);
        this.swiftMessageRepository = java.util.Objects.requireNonNull(swiftMessageRepository);
    }

    /**
     * <pre>
     * [MVP5-F03] Kafka transfer.outbound 수신 → FEP REST 전송. 주 진입점은 HTTP가 아니다.
     * TODO: MT103 필드 매핑, UETR 부여 / 예외: 매핑 실패 시 송금 실패 + 역분개
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public SubmitSwiftResult submitSwift(SubmitSwiftCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP5-F03");
    }

    /**
     * <pre>
     * [MVP5-F05] 무응답 송금은 같은 UETR로 FEP REST 재전송한다.
     * TODO: 정해진 간격·최대 횟수, UETR 고정, fep_db retries 기록 / 예외: 최대 횟수 초과 시 운영자 확인 필요
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void retrySwift(RetrySwiftCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP5-F05");
    }

    /**
     * <pre>
     * [MVP5-F06,MVP5-F08] FEP REST/pacs.002 결과 판정 후 fep.ack 의도를 기록할 내부 유스케이스.
     * TODO: ACSC → 확정 분개, RJCT → 역분개로 고객 계좌 전액 반환 / 예외: 중복 응답은 무시
     * TODO: fep.ack 발행자·소비자와 최종 상태 갱신 책임을 정함. 수신 확인·진행 중·최종 결과를 구분하고 확정·역분개는 멱등 처리. / 예외: 미확정 응답·계속 무응답은 확인 대기. 지원 상태 코드와 최종성 정의는 보류.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void recordSwiftAcknowledgement(RecordSwiftAcknowledgementCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP5-F06");
    }
}
