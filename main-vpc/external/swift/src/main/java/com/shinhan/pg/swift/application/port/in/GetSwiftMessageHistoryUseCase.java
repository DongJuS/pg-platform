package com.shinhan.pg.swift.application.port.in;

import com.shinhan.pg.swift.application.command.GetSwiftMessageHistoryCommand;
import com.shinhan.pg.swift.application.result.GetSwiftMessageHistoryResult;

/**
 * <pre>
 * [MVP5-F07] UETR 전문·재전송 이력 조회
 * API 초안: GET /internal/v1/swift/transfers/{uetr}/messages
 * 상태: 보류 — Controller bean/HTTP mapping 등록 금지
 * 인증/접근: 운영자 권한 / ADMIN
 * 입력 근거: UETR
 * 출력 근거: 메시지 이력·처리 결과·재전송 횟수
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 없는 UETR·접근 거절의 응답 계약은 후속 확정.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
 * </pre>
 */
public interface GetSwiftMessageHistoryUseCase {
    GetSwiftMessageHistoryResult getSwiftMessageHistory(GetSwiftMessageHistoryCommand command);
}
