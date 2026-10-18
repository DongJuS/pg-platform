package com.shinhan.pg.transfer.application.port.in;

import com.shinhan.pg.transfer.application.command.GetOverseasTransferCommand;
import com.shinhan.pg.transfer.application.result.GetOverseasTransferResult;

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
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
 * </pre>
 */
public interface GetOverseasTransferUseCase {
    GetOverseasTransferResult getOverseasTransfer(GetOverseasTransferCommand command);
}
