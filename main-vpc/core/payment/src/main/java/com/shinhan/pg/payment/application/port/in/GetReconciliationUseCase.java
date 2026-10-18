package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.GetReconciliationCommand;
import com.shinhan.pg.payment.application.result.GetReconciliationResult;

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
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a498124adadc5990b64c279
 * </pre>
 */
public interface GetReconciliationUseCase {
    GetReconciliationResult getReconciliation(GetReconciliationCommand command);
}
