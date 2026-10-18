package com.shinhan.pg.aigateway.application.port.in;

import com.shinhan.pg.aigateway.application.command.GetAiRequestCommand;
import com.shinhan.pg.aigateway.application.result.GetAiRequestResult;

/**
 * <pre>
 * [MVP3-F02] AI 요청 기록 조회
 * API 초안: GET /internal/v1/ai/requests/{requestId}
 * 상태: 보류 — Controller bean/HTTP mapping 등록 금지
 * 인증/접근: 운영자 권한 / ADMIN
 * 입력 근거: requestId
 * 출력 근거: 처리 이력
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 403
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a49813c90a5e041a90c08df
 * </pre>
 */
public interface GetAiRequestUseCase {
    GetAiRequestResult getAiRequest(GetAiRequestCommand command);
}
