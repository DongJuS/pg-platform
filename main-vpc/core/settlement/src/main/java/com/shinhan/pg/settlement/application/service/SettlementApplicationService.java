package com.shinhan.pg.settlement.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;
import com.shinhan.pg.settlement.application.command.GetSettlementCommand;
import com.shinhan.pg.settlement.application.command.RerunSettlementCommand;
import com.shinhan.pg.settlement.application.port.in.CloseSettlementUseCase;
import com.shinhan.pg.settlement.application.port.in.GetSettlementUseCase;
import com.shinhan.pg.settlement.application.port.in.RerunSettlementUseCase;
import com.shinhan.pg.settlement.application.port.out.FepSettlementPort;
import com.shinhan.pg.settlement.application.port.out.LedgerPostingPort;
import com.shinhan.pg.settlement.application.port.out.SettlementAtomicWritePort;
import com.shinhan.pg.settlement.application.port.out.SettlementCycleRepository;
import com.shinhan.pg.settlement.application.port.out.SettlementFileRepository;
import com.shinhan.pg.settlement.application.port.out.SettlementLineRepository;
import com.shinhan.pg.settlement.application.result.CloseSettlementResult;
import com.shinhan.pg.settlement.application.result.GetSettlementResult;
import com.shinhan.pg.settlement.application.result.RerunSettlementResult;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class SettlementApplicationService
        implements CloseSettlementUseCase, GetSettlementUseCase, RerunSettlementUseCase {
    private final FepSettlementPort fepSettlementPort;
    private final LedgerPostingPort ledgerPostingPort;
    private final SettlementAtomicWritePort settlementAtomicWritePort;
    private final SettlementCycleRepository settlementCycleRepository;
    private final SettlementLineRepository settlementLineRepository;
    private final SettlementFileRepository settlementFileRepository;

    public SettlementApplicationService(
            FepSettlementPort fepSettlementPort,
            LedgerPostingPort ledgerPostingPort,
            SettlementAtomicWritePort settlementAtomicWritePort,
            SettlementCycleRepository settlementCycleRepository,
            SettlementLineRepository settlementLineRepository,
            SettlementFileRepository settlementFileRepository) {
        this.fepSettlementPort = java.util.Objects.requireNonNull(fepSettlementPort);
        this.ledgerPostingPort = java.util.Objects.requireNonNull(ledgerPostingPort);
        this.settlementAtomicWritePort =
                java.util.Objects.requireNonNull(settlementAtomicWritePort);
        this.settlementCycleRepository =
                java.util.Objects.requireNonNull(settlementCycleRepository);
        this.settlementLineRepository = java.util.Objects.requireNonNull(settlementLineRepository);
        this.settlementFileRepository = java.util.Objects.requireNonNull(settlementFileRepository);
    }

    /**
     * <pre>
     * [MVP7-F01,MVP7-F02,MVP7-F07,MVP7-F08] 일 마감 배치 또는 내부 수동 재실행. Kafka 메시지마다 마감하는 기능이 아니다.
     * TODO: 컷오프 이후 거래는 익일로, 같은 날짜 중복 실행 방지 / 예외: 실패 시 운영자 알림
     * TODO: 상계 후 차액, 순액 합계 0 검증, 금융결제원 형식 파일 / 예외: 합계≠0이면 중단
     * TODO: 생성·송신·결과 수신·원장 반영·확정 단계를 기록. 날짜 외에 회차·버전·결과 식별자를 검토하여 안전하게 재실행. / 예외: 반영 후 응답 유실 시 기존 결과 조회. 불일치·반복 실패는 확정 보류.
     * TODO: 업무일·시간대·컷오프 기준을 고정하고 입력 목록 또는 검증 가능한 범위를 보존. 지연 거래의 이월·보정 방식은 미정. / 예외: 재실행에서 입력이 조용히 달라지지 않게 회차·자료 버전을 확인.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public CloseSettlementResult closeSettlement(CloseSettlementCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP7-F01");
    }

    /**
     * <pre>
     * [MVP7-F04] 마감 결과·재처리
     * API 초안: GET /internal/v1/settlements/{date}
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: date
     * 출력 근거: 마감 상태, 불일치 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 불일치 시 확정 안 함
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 결과 대사 → 일치 시 원장 반영·마감 확정, settlement.completed 발행
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public GetSettlementResult getSettlement(GetSettlementCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 결과 대사 → 일치 시 원장 반영·마감 확정, settlement.completed 발행
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP7-F04");
    }

    /**
     * <pre>
     * [MVP7-F04] 마감 결과·재처리
     * API 초안: POST /internal/v1/settlements/{date}/rerun
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: date
     * 출력 근거: 마감 상태, 불일치 목록
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 불일치 시 확정 안 함
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 결과 대사 → 일치 시 원장 반영·마감 확정, settlement.completed 발행
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 회차의 원래 cutoff와 입력 스냅샷을 재사용한다. 결과 대사 후에만 정산 분개·마감 확정한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public RerunSettlementResult rerunSettlement(RerunSettlementCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 결과 대사 → 일치 시 원장 반영·마감 확정, settlement.completed 발행
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP7-F04");
    }
}
