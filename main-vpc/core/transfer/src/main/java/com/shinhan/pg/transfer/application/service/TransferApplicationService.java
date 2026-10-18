package com.shinhan.pg.transfer.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.command.GetOverseasTransferCommand;
import com.shinhan.pg.transfer.application.command.GetTransferCommand;
import com.shinhan.pg.transfer.application.command.ListTransfersCommand;
import com.shinhan.pg.transfer.application.command.RecoverTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestOverseasTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.in.CompleteTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.GetOverseasTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.GetTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.ListTransfersUseCase;
import com.shinhan.pg.transfer.application.port.in.RecoverTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestOverseasTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestTransferUseCase;
import com.shinhan.pg.transfer.application.port.out.LedgerPostingPort;
import com.shinhan.pg.transfer.application.port.out.OtpVerificationPort;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferDetailsRepository;
import com.shinhan.pg.transfer.application.port.out.TransferAtomicWritePort;
import com.shinhan.pg.transfer.application.port.out.TransferHistoryRepository;
import com.shinhan.pg.transfer.application.port.out.TransferRepository;
import com.shinhan.pg.transfer.application.result.GetOverseasTransferResult;
import com.shinhan.pg.transfer.application.result.GetTransferResult;
import com.shinhan.pg.transfer.application.result.ListTransfersResult;
import com.shinhan.pg.transfer.application.result.RequestOverseasTransferResult;
import com.shinhan.pg.transfer.application.result.RequestTransferResult;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class TransferApplicationService
        implements RequestTransferUseCase,
                CompleteTransferUseCase,
                RecoverTransferUseCase,
                GetTransferUseCase,
                ListTransfersUseCase,
                RequestOverseasTransferUseCase,
                GetOverseasTransferUseCase {
    private final LedgerPostingPort ledgerPostingPort;
    private final OtpVerificationPort otpVerificationPort;
    private final TransferAtomicWritePort transferAtomicWritePort;
    private final TransferRepository transferRepository;
    private final TransferHistoryRepository transferHistoryRepository;
    private final OverseasTransferDetailsRepository overseasTransferDetailsRepository;

    public TransferApplicationService(
            LedgerPostingPort ledgerPostingPort,
            OtpVerificationPort otpVerificationPort,
            TransferAtomicWritePort transferAtomicWritePort,
            TransferRepository transferRepository,
            TransferHistoryRepository transferHistoryRepository,
            OverseasTransferDetailsRepository overseasTransferDetailsRepository) {
        this.ledgerPostingPort = java.util.Objects.requireNonNull(ledgerPostingPort);
        this.otpVerificationPort = java.util.Objects.requireNonNull(otpVerificationPort);
        this.transferAtomicWritePort = java.util.Objects.requireNonNull(transferAtomicWritePort);
        this.transferRepository = java.util.Objects.requireNonNull(transferRepository);
        this.transferHistoryRepository =
                java.util.Objects.requireNonNull(transferHistoryRepository);
        this.overseasTransferDetailsRepository =
                java.util.Objects.requireNonNull(overseasTransferDetailsRepository);
    }

    /**
     * <pre>
     * [MVP1-F15] 가상 이체
     * API 초안: POST /api/v1/transfers
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: Idempotency-Key 헤더, 출금·입금 계좌, 금액, otpToken
     * 출력 근거: transferId, 상태; 중복 요청은 오류
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 중복 요청·키 충돌, 422 잔액 부족, 401 OTP
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 중복 이체 요청은 오류로 알림. 잔액 조건 검사·분개·잔액 반영은 Ledger에서 원자 처리. 결과 불명은 기존 transactionId로 확인.
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: actorUserId + idempotencyKey 고유 조건과 요청 해시를 예약한다. 같은 키 재요청은 사용자 결정대로 409 오류이며 신규 이체를 만들지 않는다.
     * TODO: 원장 REST 성공과 ledger.posted 소비는 동일 완료 경로로 수렴시킨다. 시간 초과는 UNKNOWN으로 두고 동일 transactionId로 조회한다.
     * TODO: 성공 200은 동기 완료의 응답 초안이다. 접수/결과불명 시 HTTP 응답 계약은 미정이며 코드로 성공을 꾸미지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981ae8beff34a15fd47f1
     * </pre>
     */
    @Override
    public RequestTransferResult requestTransfer(RequestTransferCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 중복 이체 요청은 오류로 알림. 잔액 조건 검사·분개·잔액 반영은 Ledger에서 원자 처리. 결과 불명은 기존 transactionId로 확인.
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 동일 거래 결과를 완료 처리하고 transfer.completed를 로컬 Outbox에 한 번만 기록한다.
        throw new FeatureNotImplementedException("MVP1-F15");
    }

    /**
     * <pre>
     * [MVP1-F15,MVP1-F20] Kafka ledger.posted 반영 또는 REST 결과 확정. transfer.completed 발행 책임은 Transfer에 있다.
     * TODO: 중복 이체 요청은 오류로 알림. 잔액 조건 검사·분개·잔액 반영은 Ledger에서 원자 처리. 결과 불명은 기존 transactionId로 확인. / 예외: 409 중복 요청·키 충돌, 422 잔액 부족, 401 OTP
     * TODO: 같은 키 재요청은 중복 오류로 알리고 새 이체를 만들지 않음. 복구 작업은 기존 transactionId로 원장 결과 확인. 조회 404만으로 최종 실패를 단정하지 않음. / 예외: 일시 장애는 확인 대기 유지. 복구 간격·수동 확인 전환 조건은 보류.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void completeTransfer(CompleteTransferCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP1-F15");
    }

    /**
     * <pre>
     * [MVP1-F20] UNKNOWN 결과를 원장에서 조회·복구하는 재개 진입점.
     * TODO: 같은 키 재요청은 중복 오류로 알리고 새 이체를 만들지 않음. 복구 작업은 기존 transactionId로 원장 결과 확인. 조회 404만으로 최종 실패를 단정하지 않음. / 예외: 일시 장애는 확인 대기 유지. 복구 간격·수동 확인 전환 조건은 보류.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void recoverTransfer(RecoverTransferCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP1-F20");
    }

    /**
     * <pre>
     * [MVP1-F16] 이체 상태 조회
     * API 초안: GET /api/v1/transfers/{transferId}
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: transferId
     * 출력 근거: 상태, 전이 이력
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403, 404
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 상태 전이 이력 포함
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498126a203df4f8dd678f7
     * </pre>
     */
    @Override
    public GetTransferResult getTransfer(GetTransferCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 상태 전이 이력 포함
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F16");
    }

    /**
     * <pre>
     * [MVP1-F17] 이체 목록
     * API 초안: GET /api/v1/transfers
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 기간, 상태, page
     * 출력 근거: 이체 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 401
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 본인 것만
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498101b674c0841cd7ad05
     * </pre>
     */
    @Override
    public ListTransfersResult listTransfers(ListTransfersCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 본인 것만
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F17");
    }

    /**
     * <pre>
     * [MVP5-F01] 해외 송금 신청
     * API 초안: POST /api/v1/transfers/overseas
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT + OTP / PUBLIC
     * 입력 근거: Idempotency-Key, 수취인, BIC, 계좌, 통화, 금액
     * 출력 근거: transferId, 상태(처리 중)
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 422 잔액 부족
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 고객 계좌 → 송금 대기 계정 가처리 분개 → Outbox → transfer.outbound
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 원장 송금 대기 계정 가처리와 transfer.outbound를 같은 로컬 원자성 계약에 연결한다. 환율·수수료 계산식은 보류다.
     * TODO: 해외 계좌 정보는 합성 데이터만 사용하고 요청 원문을 로그에 남기지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public RequestOverseasTransferResult requestOverseasTransfer(
            RequestOverseasTransferCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 고객 계좌 → 송금 대기 계정 가처리 분개 → Outbox → transfer.outbound
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP5-F01");
    }

    /**
     * <pre>
     * [MVP5-F02] 해외 송금 상태
     * API 초안: GET /api/v1/transfers/overseas/{transferId}
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: transferId
     * 출력 근거: 상태, UETR, 거절 사유
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403, 404
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 본인 것만
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public GetOverseasTransferResult getOverseasTransfer(GetOverseasTransferCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 본인 것만
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP5-F02");
    }
}
