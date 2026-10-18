package com.shinhan.pg.payment.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.payment.application.command.ApplyPaymentConfirmationCommand;
import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.command.CancelPaymentCommand;
import com.shinhan.pg.payment.application.command.GetPaymentCommand;
import com.shinhan.pg.payment.application.command.GetReconciliationCommand;
import com.shinhan.pg.payment.application.command.ListRefundsCommand;
import com.shinhan.pg.payment.application.command.ReceivePgWebhookCommand;
import com.shinhan.pg.payment.application.command.RecoverPaymentCommand;
import com.shinhan.pg.payment.application.command.RequestRefundCommand;
import com.shinhan.pg.payment.application.command.StartReconciliationCommand;
import com.shinhan.pg.payment.application.port.in.ApplyPaymentConfirmationUseCase;
import com.shinhan.pg.payment.application.port.in.ApprovePaymentUseCase;
import com.shinhan.pg.payment.application.port.in.CancelPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.GetPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.GetReconciliationUseCase;
import com.shinhan.pg.payment.application.port.in.ListRefundsUseCase;
import com.shinhan.pg.payment.application.port.in.ReceivePgWebhookUseCase;
import com.shinhan.pg.payment.application.port.in.RecoverPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.RequestRefundUseCase;
import com.shinhan.pg.payment.application.port.in.StartReconciliationUseCase;
import com.shinhan.pg.payment.application.port.out.FepPaymentPort;
import com.shinhan.pg.payment.application.port.out.LedgerPostingPort;
import com.shinhan.pg.payment.application.port.out.PaymentAtomicWritePort;
import com.shinhan.pg.payment.application.port.out.PaymentRepository;
import com.shinhan.pg.payment.application.port.out.PgEventRepository;
import com.shinhan.pg.payment.application.port.out.PgWebhookDecoderPort;
import com.shinhan.pg.payment.application.port.out.ReconciliationRunRepository;
import com.shinhan.pg.payment.application.port.out.RefundRepository;
import com.shinhan.pg.payment.application.result.ApprovePaymentResult;
import com.shinhan.pg.payment.application.result.CancelPaymentResult;
import com.shinhan.pg.payment.application.result.GetPaymentResult;
import com.shinhan.pg.payment.application.result.GetReconciliationResult;
import com.shinhan.pg.payment.application.result.ListRefundsResult;
import com.shinhan.pg.payment.application.result.RequestRefundResult;
import com.shinhan.pg.payment.application.result.StartReconciliationResult;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class PaymentApplicationService
        implements ApprovePaymentUseCase,
                ApplyPaymentConfirmationUseCase,
                GetPaymentUseCase,
                CancelPaymentUseCase,
                RequestRefundUseCase,
                ListRefundsUseCase,
                ReceivePgWebhookUseCase,
                StartReconciliationUseCase,
                GetReconciliationUseCase,
                RecoverPaymentUseCase {
    private final FepPaymentPort fepPaymentPort;
    private final LedgerPostingPort ledgerPostingPort;
    private final PaymentAtomicWritePort paymentAtomicWritePort;
    private final PgWebhookDecoderPort pgWebhookDecoderPort;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PgEventRepository pgEventRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;

    public PaymentApplicationService(
            FepPaymentPort fepPaymentPort,
            LedgerPostingPort ledgerPostingPort,
            PaymentAtomicWritePort paymentAtomicWritePort,
            PgWebhookDecoderPort pgWebhookDecoderPort,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            PgEventRepository pgEventRepository,
            ReconciliationRunRepository reconciliationRunRepository) {
        this.fepPaymentPort = java.util.Objects.requireNonNull(fepPaymentPort);
        this.ledgerPostingPort = java.util.Objects.requireNonNull(ledgerPostingPort);
        this.paymentAtomicWritePort = java.util.Objects.requireNonNull(paymentAtomicWritePort);
        this.pgWebhookDecoderPort = java.util.Objects.requireNonNull(pgWebhookDecoderPort);
        this.paymentRepository = java.util.Objects.requireNonNull(paymentRepository);
        this.refundRepository = java.util.Objects.requireNonNull(refundRepository);
        this.pgEventRepository = java.util.Objects.requireNonNull(pgEventRepository);
        this.reconciliationRunRepository =
                java.util.Objects.requireNonNull(reconciliationRunRepository);
    }

    /**
     * <pre>
     * [MVP2-F01] 모의 결제
     * API 초안: POST /api/v1/payments
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: Idempotency-Key, 주문번호, 금액
     * 출력 근거: paymentId, 상태
     * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 중복, 502 PG 오류
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. '결제 시작' 기록 → FEP → Mock PG 승인 요청, 타임아웃 시 '결과 확인 중'
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: Payment → FEP 내부 REST → Mock PG. 직접 Mock PG를 호출하지 않는다. 접수와 최종 승인을 구분한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981c9b971efeb78107e6d
     * </pre>
     */
    @Override
    public ApprovePaymentResult approvePayment(ApprovePaymentCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: '결제 시작' 기록 → FEP → Mock PG 승인 요청, 타임아웃 시 '결과 확인 중'
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP2-F01");
    }

    /**
     * <pre>
     * [MVP2-F15] 검증된 PG 결과 이벤트를 반영한다. 구체 topic/웹훅 변환 계약은 후속이다.
     * TODO: 웹훅을 검증·기록한 뒤 상태전이와 원장 반영을 조정. 외부 승인 후 내부 반영 실패는 복구 대상으로 보존. / 예외: 중복·역순 응답과 동일 ID의 상이한 내용에 대한 정책은 Q23에서 검토.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void applyPaymentConfirmation(ApplyPaymentConfirmationCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP2-F15");
    }

    /**
     * <pre>
     * [MVP2-F02] 결제 조회
     * API 초안: GET /api/v1/payments/{paymentId}
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: paymentId
     * 출력 근거: 상태, 금액
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403, 404
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 본인 결제만
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981449d22d4d2771b461e
     * </pre>
     */
    @Override
    public GetPaymentResult getPayment(GetPaymentCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 본인 결제만
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP2-F02");
    }

    /**
     * <pre>
     * [MVP2-F03] 결제 취소
     * API 초안: POST /api/v1/payments/{paymentId}/cancel
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: paymentId
     * 출력 근거: 상태
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 취소 불가 상태
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 승인 완료 상태만, 중복 취소는 첫 결과 반환
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 진행 중 환불 예약과 취소가 충돌할 때의 우선순위·해제 조건은 미정이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498145b515caf3f1cb101e
     * </pre>
     */
    @Override
    public CancelPaymentResult cancelPayment(CancelPaymentCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 승인 완료 상태만, 중복 취소는 첫 결과 반환
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP2-F03");
    }

    /**
     * <pre>
     * [MVP2-F04] 환불 요청
     * API 초안: POST /api/v1/payments/{paymentId}/refunds
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 금액, Idempotency-Key
     * 출력 근거: refundId, 상태
     * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 422 한도 초과
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 누적 환불액 ≤ 승인액
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 확정 환불액과 처리 중 예약액을 같은 DB 트랜잭션으로 검사한다. 누적 환불 승인액 초과를 방지한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49815fb6b4c3b5663a8c92
     * </pre>
     */
    @Override
    public RequestRefundResult requestRefund(RequestRefundCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 누적 환불액 ≤ 승인액
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP2-F04");
    }

    /**
     * <pre>
     * [MVP2-F05] 환불 이력
     * API 초안: GET /api/v1/payments/{paymentId}/refunds
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: paymentId
     * 출력 근거: 환불 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 본인 결제만
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498101a9cdcbd1595716c0
     * </pre>
     */
    @Override
    public ListRefundsResult listRefunds(ListRefundsCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 본인 결제만
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP2-F05");
    }

    /**
     * <pre>
     * [MVP2-F06] Mock PG 웹훅 수신
     * API 초안: POST /mock/v1/pg/webhooks
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 웹훅 검증 / MOCK_WEBHOOK
     * 입력 근거: PG 이벤트(이벤트 ID, 유형, 시각)
     * 출력 근거: 200
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 400 서명 오류
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 이벤트 ID 중복 제거, 상태 전이 규칙으로 역순 처리
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 원문 byte 기준 서명 검증을 payload 변환보다 먼저 수행한다. HMAC/서명 헤더 상세 형식은 미정이다.
     * TODO: 동기 PG 결과와 웹훅은 ApplyPaymentConfirmationUseCase를 공유하도록 구현한다. eventId 중복과 거래 효과 중복을 모두 구분한다.
     * TODO: 현재 Payment 경로 표기는 원문 명세다. FEP 경유 callback 전달·서비스 인증과 기관 서명 보존 방식은 확정 전이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981bebda6ddcf95ec0732
     * </pre>
     */
    @Override
    public void receivePgWebhook(ReceivePgWebhookCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 이벤트 ID 중복 제거, 상태 전이 규칙으로 역순 처리
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP2-F06");
    }

    /**
     * <pre>
     * [MVP2-F08] 대사 실행
     * API 초안: POST /internal/v1/payments/reconciliation-runs
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: 대상 일자
     * 출력 근거: runId
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 실행 중
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 누락·중복·금액 불일치 탐지, 원본 이벤트 연결
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49819596e2fb38eb2c914a
     * </pre>
     */
    @Override
    public StartReconciliationResult startReconciliation(StartReconciliationCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 누락·중복·금액 불일치 탐지, 원본 이벤트 연결
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP2-F08");
    }

    /**
     * <pre>
     * [MVP2-F09] 대사 결과
     * API 초안: GET /internal/v1/payments/reconciliation-runs/{runId}
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: runId
     * 출력 근거: 불일치 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 404
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. —
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498124adadc5990b64c279
     * </pre>
     */
    @Override
    public GetReconciliationResult getReconciliation(GetReconciliationCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: —
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP2-F09");
    }

    /**
     * <pre>
     * [MVP2-F14] PG/FEP 조회로 UNKNOWN을 확인한다. 조회 중 재승인하지 않는다.
     * TODO: 승인·거절·취소가 확인된 결과만 반영. 확인 불가 시 UNKNOWN 유지. 조회만으로 재승인하지 않음. / 예외: 반복 무응답은 운영자 확인 대상으로 남김. 횟수·간격은 미정.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void recoverPayment(RecoverPaymentCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP2-F14");
    }
}
