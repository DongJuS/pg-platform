package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.StartReconciliationCommand;
import com.shinhan.pg.payment.application.result.StartReconciliationResult;

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
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a49819596e2fb38eb2c914a
 * </pre>
 */
public interface StartReconciliationUseCase {
    StartReconciliationResult startReconciliation(StartReconciliationCommand command);
}
