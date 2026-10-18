package com.shinhan.pg.transfer.application.port.in;

import com.shinhan.pg.transfer.application.command.ListTransfersCommand;
import com.shinhan.pg.transfer.application.result.ListTransfersResult;

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
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a498101b674c0841cd7ad05
 * </pre>
 */
public interface ListTransfersUseCase {
    ListTransfersResult listTransfers(ListTransfersCommand command);
}
