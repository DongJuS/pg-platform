package com.shinhan.pg.fepadapter.application.port.in;

import com.shinhan.pg.fepadapter.application.command.SendSwiftMessageCommand;
import com.shinhan.pg.fepadapter.application.result.SendSwiftMessageResult;

/**
 * <pre>
 * [MVP5-F04] FEP 전문 송신
 * API 초안: POST /internal/v1/fep/swift/messages
 * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 서비스 인증 / INTERNAL
 * 입력 근거: 송금 데이터, UETR
 * 출력 근거: messageId
 * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 422 XSD 실패
 * TODO: XSD 검증 후 전문·전송 의도를 영속화하고 Mock SWIFT로 송신한다. messageId 접수만으로 ACSC 완료를 선언하지 않는다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
 * </pre>
 */
public interface SendSwiftMessageUseCase {
    SendSwiftMessageResult sendSwiftMessage(SendSwiftMessageCommand command);
}
