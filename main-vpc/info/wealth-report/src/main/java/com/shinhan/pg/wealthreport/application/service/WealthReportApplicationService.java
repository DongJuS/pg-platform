package com.shinhan.pg.wealthreport.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.command.GetReportCommand;
import com.shinhan.pg.wealthreport.application.command.ListReportsCommand;
import com.shinhan.pg.wealthreport.application.command.RecoverReportGenerationCommand;
import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;
import com.shinhan.pg.wealthreport.application.port.in.GenerateReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.GetReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.ListReportsUseCase;
import com.shinhan.pg.wealthreport.application.port.in.RecoverReportGenerationUseCase;
import com.shinhan.pg.wealthreport.application.port.in.UpdateReportSnapshotUseCase;
import com.shinhan.pg.wealthreport.application.port.out.AiReportDraftPort;
import com.shinhan.pg.wealthreport.application.port.out.AssetReportRepository;
import com.shinhan.pg.wealthreport.application.port.out.AssetSnapshotRepository;
import com.shinhan.pg.wealthreport.application.port.out.PdfRenderingPort;
import com.shinhan.pg.wealthreport.application.port.out.PseudonymizationPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportProgressPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotWritePort;
import com.shinhan.pg.wealthreport.application.port.out.ReportStoragePort;
import com.shinhan.pg.wealthreport.application.port.out.TokenMappingRepository;
import com.shinhan.pg.wealthreport.application.result.GenerateReportResult;
import com.shinhan.pg.wealthreport.application.result.GetReportResult;
import com.shinhan.pg.wealthreport.application.result.ListReportsResult;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class WealthReportApplicationService
        implements GenerateReportUseCase,
                UpdateReportSnapshotUseCase,
                GetReportUseCase,
                ListReportsUseCase,
                RecoverReportGenerationUseCase {
    private final ReportSnapshotPort reportSnapshotPort;
    private final PseudonymizationPort pseudonymizationPort;
    private final AiReportDraftPort aiReportDraftPort;
    private final PdfRenderingPort pdfRenderingPort;
    private final ReportStoragePort reportStoragePort;
    private final ReportProgressPort reportProgressPort;
    private final ReportSnapshotWritePort reportSnapshotWritePort;
    private final AssetReportRepository assetReportRepository;
    private final AssetSnapshotRepository assetSnapshotRepository;
    private final TokenMappingRepository tokenMappingRepository;

    public WealthReportApplicationService(
            ReportSnapshotPort reportSnapshotPort,
            PseudonymizationPort pseudonymizationPort,
            AiReportDraftPort aiReportDraftPort,
            PdfRenderingPort pdfRenderingPort,
            ReportStoragePort reportStoragePort,
            ReportProgressPort reportProgressPort,
            ReportSnapshotWritePort reportSnapshotWritePort,
            AssetReportRepository assetReportRepository,
            AssetSnapshotRepository assetSnapshotRepository,
            TokenMappingRepository tokenMappingRepository) {
        this.reportSnapshotPort = java.util.Objects.requireNonNull(reportSnapshotPort);
        this.pseudonymizationPort = java.util.Objects.requireNonNull(pseudonymizationPort);
        this.aiReportDraftPort = java.util.Objects.requireNonNull(aiReportDraftPort);
        this.pdfRenderingPort = java.util.Objects.requireNonNull(pdfRenderingPort);
        this.reportStoragePort = java.util.Objects.requireNonNull(reportStoragePort);
        this.reportProgressPort = java.util.Objects.requireNonNull(reportProgressPort);
        this.reportSnapshotWritePort = java.util.Objects.requireNonNull(reportSnapshotWritePort);
        this.assetReportRepository = java.util.Objects.requireNonNull(assetReportRepository);
        this.assetSnapshotRepository = java.util.Objects.requireNonNull(assetSnapshotRepository);
        this.tokenMappingRepository = java.util.Objects.requireNonNull(tokenMappingRepository);
    }

    /**
     * <pre>
     * [MVP4-F01] 보고서 생성 요청
     * API 초안: POST /api/v1/wealth/reports
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 기간
     * 출력 근거: reportId, 상태(접수)
     * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 진행 중 보고서 있음
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 자료 시점 고정·Java 계산 → 가명처리 → AI 게이트웨이 → 계산 결과와 숫자 검증 → 토큰 복원 → PDF·S3. 작업 중단 시 기존 reportId로 복구.
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 최신 사용자 4-A: PDF 생성·S3 저장까지 기다려 완성 결과를 동기 응답한다. 과거 202/작업 접수 응답으로 되돌리지 않는다.
     * TODO: Java 지표 계산과 기준 시점을 고정한 뒤 가명처리한다. AI는 설명을 생성하고 숫자는 계산 결과와 대조한다.
     * TODO: 요청 키의 헤더명·필수 여부와 동기 성공 200/201 세부 계약은 초안이다. timeout·동시 생성 수는 기존 설정을 실제 집행하는 코드로 연결한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49816f8110d5e30813050b
     * </pre>
     */
    @Override
    public GenerateReportResult generateReport(GenerateReportCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 자료 시점 고정·Java 계산 → 가명처리 → AI 게이트웨이 → 계산 결과와 숫자 검증 → 토큰 복원 → PDF·S3. 작업 중단 시 기존
        // reportId로 복구.
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: PDF/S3 저장을 완료한 뒤 동기 응답한다. 202 작업 접수로 바꾸지 않는다.
        throw new FeatureNotImplementedException("MVP4-F01");
    }

    /**
     * <pre>
     * [MVP4-F05,MVP4-F09] Kafka 이체·결제 완료로 스냅샷을 갱신한다. 보고서 생성을 호출하지 않는다.
     * TODO: 이벤트 ID 중복 제거, 일자별 집계 / 예외: 처리 실패 시 DLQ
     * TODO: 업무 거래 식별자로 중복 집계를 방지하고 보고서 자료 시점을 고정. 지연 이벤트·재구축의 포함 기준은 Q24에서 검토. / 예외: 불완전한 스냅샷을 현재 잔액과 섞지 않음. 지연 허용 범위는 미정.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void updateReportSnapshot(UpdateReportSnapshotCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP4-F05");
    }

    /**
     * <pre>
     * [MVP4-F02] 보고서 결과 조회
     * API 초안: GET /api/v1/wealth/reports/{reportId}
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: reportId
     * 출력 근거: 상태, PDF 링크(만료 시각)
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403, 404
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 본인 보고서만, 완료 시 Presigned URL 발급
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981339cb8fae5e2cd0f23
     * </pre>
     */
    @Override
    public GetReportResult getReport(GetReportCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 본인 보고서만, 완료 시 Presigned URL 발급
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP4-F02");
    }

    /**
     * <pre>
     * [MVP4-F03] 보고서 목록
     * API 초안: GET /api/v1/wealth/reports
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 기간, 상태
     * 출력 근거: 보고서 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 401
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 본인 것만
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981429715cd54675fcd4c
     * </pre>
     */
    @Override
    public ListReportsResult listReports(ListReportsCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 본인 것만
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP4-F03");
    }

    /**
     * <pre>
     * [MVP4-F10] 동기 보고서 생성의 중단 단계·기준 시점·생성 시도를 복구한다. 별도 202 API나 비동기 큐를 확정하지 않는다.
     * TODO: 접수 상태를 영속화하고 작업을 실행. 같은 요청의 중복 작업과 재처리 시 중복 전달·과금을 통제하는 정책 검토. / 예외: 프로세스 종료 후 미완료 작업 복구. 외부 과금 확인 불가 상태를 성공으로 단정하지 않음.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void recoverReportGeneration(RecoverReportGenerationCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP4-F10");
    }
}
