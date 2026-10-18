package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.ReceivePgWebhookCommand;

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
 * TODO: 원문 byte 기준 서명 검증을 payload 변환보다 먼저 수행한다. HMAC/서명 헤더 상세 형식은 미정이다.
 * TODO: 동기 PG 결과와 웹훅은 ApplyPaymentConfirmationUseCase를 공유하도록 구현한다. eventId 중복과 거래 효과 중복을 모두 구분한다.
 * TODO: 현재 Payment 경로 표기는 원문 명세다. FEP 경유 callback 전달·서비스 인증과 기관 서명 보존 방식은 확정 전이다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981bebda6ddcf95ec0732
 * </pre>
 */
public interface ReceivePgWebhookUseCase {
    void receivePgWebhook(ReceivePgWebhookCommand command);
}
