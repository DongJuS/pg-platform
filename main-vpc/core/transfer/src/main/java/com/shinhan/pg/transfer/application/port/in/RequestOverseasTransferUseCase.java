package com.shinhan.pg.transfer.application.port.in;

import com.shinhan.pg.transfer.application.command.RequestOverseasTransferCommand;
import com.shinhan.pg.transfer.application.result.RequestOverseasTransferResult;

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
 * TODO: 원장 송금 대기 계정 가처리와 transfer.outbound를 같은 로컬 원자성 계약에 연결한다. 환율·수수료 계산식은 보류다.
 * TODO: 해외 계좌 정보는 합성 데이터만 사용하고 요청 원문을 로그에 남기지 않는다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
 * </pre>
 */
public interface RequestOverseasTransferUseCase {
    RequestOverseasTransferResult requestOverseasTransfer(RequestOverseasTransferCommand command);
}
