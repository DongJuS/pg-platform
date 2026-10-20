package com.shinhan.pg.fepadapter.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.fepadapter.application.command.DispatchInstitutionMessageCommand;
import com.shinhan.pg.fepadapter.application.command.ExchangeSettlementFileCommand;
import com.shinhan.pg.fepadapter.application.command.QueryInstitutionResultCommand;
import com.shinhan.pg.fepadapter.application.command.ReceiveInstitutionResultCommand;
import com.shinhan.pg.fepadapter.application.command.SendSwiftMessageCommand;
import com.shinhan.pg.fepadapter.application.port.in.DispatchInstitutionMessageUseCase;
import com.shinhan.pg.fepadapter.application.port.in.ExchangeSettlementFileUseCase;
import com.shinhan.pg.fepadapter.application.port.in.QueryInstitutionResultUseCase;
import com.shinhan.pg.fepadapter.application.port.in.ReceiveInstitutionResultUseCase;
import com.shinhan.pg.fepadapter.application.port.in.SendSwiftMessageUseCase;
import com.shinhan.pg.fepadapter.application.port.out.ClearingInstitutionPort;
import com.shinhan.pg.fepadapter.application.port.out.FepMessageRepository;
import com.shinhan.pg.fepadapter.application.port.out.FepMessageWritePort;
import com.shinhan.pg.fepadapter.application.port.out.FepTransmissionAttemptRepository;
import com.shinhan.pg.fepadapter.application.port.out.KofiuInstitutionPort;
import com.shinhan.pg.fepadapter.application.port.out.PgInstitutionPort;
import com.shinhan.pg.fepadapter.application.port.out.SwiftInstitutionPort;
import com.shinhan.pg.fepadapter.application.result.DispatchInstitutionMessageResult;
import com.shinhan.pg.fepadapter.application.result.ExchangeSettlementFileResult;
import com.shinhan.pg.fepadapter.application.result.QueryInstitutionResultResult;
import com.shinhan.pg.fepadapter.application.result.SendSwiftMessageResult;

/**
 * 기능별 계약과 TODO를 모은 application 구현 자리. Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB
 * 트랜잭션 안에서 수행하지 않는다. 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class FepAdapterApplicationService
        implements DispatchInstitutionMessageUseCase,
                ReceiveInstitutionResultUseCase,
                QueryInstitutionResultUseCase,
                SendSwiftMessageUseCase,
                ExchangeSettlementFileUseCase {
    private final PgInstitutionPort pgInstitutionPort;
    private final SwiftInstitutionPort swiftInstitutionPort;
    private final KofiuInstitutionPort kofiuInstitutionPort;
    private final ClearingInstitutionPort clearingInstitutionPort;
    private final FepMessageWritePort fepMessageWritePort;
    private final FepMessageRepository fepMessageRepository;
    private final FepTransmissionAttemptRepository fepTransmissionAttemptRepository;
    private final SwiftFepService swift;

    public FepAdapterApplicationService(
            PgInstitutionPort pgInstitutionPort,
            SwiftInstitutionPort swiftInstitutionPort,
            KofiuInstitutionPort kofiuInstitutionPort,
            ClearingInstitutionPort clearingInstitutionPort,
            FepMessageWritePort fepMessageWritePort,
            FepMessageRepository fepMessageRepository,
            FepTransmissionAttemptRepository fepTransmissionAttemptRepository) {
        this(
                pgInstitutionPort,
                swiftInstitutionPort,
                kofiuInstitutionPort,
                clearingInstitutionPort,
                fepMessageWritePort,
                fepMessageRepository,
                fepTransmissionAttemptRepository,
                null);
    }

    public FepAdapterApplicationService(
            PgInstitutionPort pgInstitutionPort,
            SwiftInstitutionPort swiftInstitutionPort,
            KofiuInstitutionPort kofiuInstitutionPort,
            ClearingInstitutionPort clearingInstitutionPort,
            FepMessageWritePort fepMessageWritePort,
            FepMessageRepository fepMessageRepository,
            FepTransmissionAttemptRepository fepTransmissionAttemptRepository,
            SwiftFepService swift) {
        this.swift = swift;
        this.pgInstitutionPort = java.util.Objects.requireNonNull(pgInstitutionPort);
        this.swiftInstitutionPort = java.util.Objects.requireNonNull(swiftInstitutionPort);
        this.kofiuInstitutionPort = java.util.Objects.requireNonNull(kofiuInstitutionPort);
        this.clearingInstitutionPort = java.util.Objects.requireNonNull(clearingInstitutionPort);
        this.fepMessageWritePort = java.util.Objects.requireNonNull(fepMessageWritePort);
        this.fepMessageRepository = java.util.Objects.requireNonNull(fepMessageRepository);
        this.fepTransmissionAttemptRepository =
                java.util.Objects.requireNonNull(fepTransmissionAttemptRepository);
    }

    /**
     *
     *
     * <pre>
     * [CMN-F04] 내부 REST 금융 대외 전문 접수. 기관 최종 결과를 즉시 접수 성공과 구분한다.
     * TODO: 금융 FEP의 송수신·인증·결과 추적 계약을 후속 검토. 외부 LLM 요청은 기존 AI Security Gateway가 중계하며 이 보류 항목의 대상에서 제외. / 예외: 금융 연동 타임아웃·재전송·웹훅 경로는 미정.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public DispatchInstitutionMessageResult dispatchInstitutionMessage(
            DispatchInstitutionMessageCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("CMN-F04");
    }

    /**
     *
     *
     * <pre>
     * [MVP2-F06,MVP5-F06] 기관 callback/result 수신 위치. 구체 인증·반환·전파 계약은 후속이다.
     * TODO: 이벤트 ID 중복 제거, 상태 전이 규칙으로 역순 처리 / 예외: 400 서명 오류
     * TODO: ACSC → 확정 분개, RJCT → 역분개로 고객 계좌 전액 반환 / 예외: 중복 응답은 무시
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void receiveInstitutionResult(ReceiveInstitutionResultCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP2-F06");
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F04,MVP7-F03] 동일 messageKey 결과 조회. 접수와 최종 결과를 구분한다.
     * TODO: pacs.008 생성, XSD 검증, fep_db messages 저장 / 예외: 422 XSD 실패
     * TODO: FEP 외 경로 차단, 결과 파일 수신 / 예외: 무응답 시 재전송(같은 날짜 1회만 반영)
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public QueryInstitutionResultResult queryInstitutionResult(
            QueryInstitutionResultCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP5-F04");
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F04] FEP 전문 송신
     * API 초안: POST /internal/v1/fep/swift/messages
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: 송금 데이터, UETR
     * 출력 근거: messageId
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 422 XSD 실패
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. pacs.008 생성, XSD 검증, fep_db messages 저장
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: XSD 검증 후 전문·전송 의도를 영속화하고 Mock SWIFT로 송신한다. messageId 접수만으로 ACSC 완료를 선언하지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public SendSwiftMessageResult sendSwiftMessage(SendSwiftMessageCommand command) {
        if (swift != null) return swift.send(command);
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: pacs.008 생성, XSD 검증, fep_db messages 저장
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP5-F04");
    }

    /**
     *
     *
     * <pre>
     * [MVP7-F03] 정산 파일 송수신
     * API 초안: POST /internal/v1/fep/settlement/files
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: 정산 파일
     * 출력 근거: 결과 파일
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 무응답 시 재전송(같은 날짜 1회만 반영)
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. FEP 외 경로 차단, 결과 파일 수신
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 같은 날짜가 아니라 cycle/version/fileKey를 중복 효과 식별자로 사용한다. 실제 고정길이 파일 필드는 미정이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public ExchangeSettlementFileResult exchangeSettlementFile(
            ExchangeSettlementFileCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: FEP 외 경로 차단, 결과 파일 수신
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP7-F03");
    }
}
