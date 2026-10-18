package com.shinhan.pg.aml.application.service;

import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.application.command.ListAmlCasesCommand;
import com.shinhan.pg.aml.application.command.RetryAmlReportCommand;
import com.shinhan.pg.aml.application.command.SubmitAmlReportCommand;
import com.shinhan.pg.aml.application.port.in.EvaluateTransactionUseCase;
import com.shinhan.pg.aml.application.port.in.ListAmlCasesUseCase;
import com.shinhan.pg.aml.application.port.in.RetryAmlReportUseCase;
import com.shinhan.pg.aml.application.port.in.SubmitAmlReportUseCase;
import com.shinhan.pg.aml.application.port.out.AmlAtomicWritePort;
import com.shinhan.pg.aml.application.port.out.AmlCaseRepository;
import com.shinhan.pg.aml.application.port.out.AmlReportRepository;
import com.shinhan.pg.aml.application.port.out.DetectionRuleRepository;
import com.shinhan.pg.aml.application.port.out.FepKofiuPort;
import com.shinhan.pg.aml.application.result.EvaluateTransactionResult;
import com.shinhan.pg.aml.application.result.ListAmlCasesResult;
import com.shinhan.pg.aml.application.result.SubmitAmlReportResult;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class AmlApplicationService
        implements EvaluateTransactionUseCase,
                RetryAmlReportUseCase,
                ListAmlCasesUseCase,
                SubmitAmlReportUseCase {
    private final FepKofiuPort fepKofiuPort;
    private final AmlAtomicWritePort amlAtomicWritePort;
    private final AmlCaseRepository amlCaseRepository;
    private final AmlReportRepository amlReportRepository;
    private final DetectionRuleRepository detectionRuleRepository;

    public AmlApplicationService(
            FepKofiuPort fepKofiuPort,
            AmlAtomicWritePort amlAtomicWritePort,
            AmlCaseRepository amlCaseRepository,
            AmlReportRepository amlReportRepository,
            DetectionRuleRepository detectionRuleRepository) {
        this.fepKofiuPort = java.util.Objects.requireNonNull(fepKofiuPort);
        this.amlAtomicWritePort = java.util.Objects.requireNonNull(amlAtomicWritePort);
        this.amlCaseRepository = java.util.Objects.requireNonNull(amlCaseRepository);
        this.amlReportRepository = java.util.Objects.requireNonNull(amlReportRepository);
        this.detectionRuleRepository = java.util.Objects.requireNonNull(detectionRuleRepository);
    }

    /**
     * <pre>
     * [MVP6-F01,MVP6-F04] Kafka ledger.posted·transfer.completed 기반 룰 평가와 자동 보고. 거래 효과 중복을 구분한다.
     * TODO: CTR(1천만 원 이상), 분할 거래, 단기 다수 이체 → 위험 점수·케이스 / 예외: —
     * TODO: 거래 이벤트 → 규칙 평가 → 위험 점수·케이스 → 보고 대상 자동 판정 → 보고서 생성·전송. GET 조회는 상태를 변경하지 않음. / 예외: 규칙·임계값·오탐 처리 등 세부 정책은 미정.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public EvaluateTransactionResult evaluateTransaction(EvaluateTransactionCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP6-F01");
    }

    /**
     * <pre>
     * [MVP6-F05] 확정 보고 전송 실패를 동일 reportKey로 재시도한다.
     * TODO: 동일 거래의 여러 이벤트를 중복 집계하지 않음. 보고 ID로 재전송을 식별하고 전송 실패·확인 대기·보고 완료·정상 종결을 구분. / 예외: 응답 유실은 취소로 처리하지 않음. Mock 수신 측 중복 접수·오류 계약은 미정.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void retryAmlReport(RetryAmlReportCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP6-F05");
    }

    /**
     * <pre>
     * [MVP6-F02] 케이스 목록 조회
     * API 초안: GET /internal/v1/aml/cases
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: 상태, 기간
     * 출력 근거: 케이스 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: —
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 조회 요청으로 판정·보고 상태를 변경하지 않음.
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public ListAmlCasesResult listAmlCases(ListAmlCasesCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 조회 요청으로 판정·보고 상태를 변경하지 않음.
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP6-F02");
    }

    /**
     * <pre>
     * [MVP6-F03] KoFIU 보고 전송
     * API 초안: POST /internal/v1/aml/cases/{caseId}/reports
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: caseId
     * 출력 근거: 200이면 '보고 완료'
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 오류 코드면 '전송 실패', 재전송 가능
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 보고서 생성 → FEP → Mock KoFIU
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 사람 승인 단계를 추가하지 않는다. 자동 판정한 보고서의 생성·전송 유스케이스 진입점이다.
     * TODO: FEP 접수 성공과 KoFIU Mock 최종 접수를 구분한다. 전송 실패는 보고 확정 취소가 아니라 재전송 대상이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public SubmitAmlReportResult submitAmlReport(SubmitAmlReportCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 보고서 생성 → FEP → Mock KoFIU
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP6-F03");
    }
}
